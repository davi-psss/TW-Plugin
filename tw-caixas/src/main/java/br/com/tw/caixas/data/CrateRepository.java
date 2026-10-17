package br.com.tw.caixas.data;

import br.com.tw.caixas.model.CrateType;
import br.com.tw.caixas.model.CrateEffect;
import br.com.tw.caixas.model.PityProgress;
import br.com.tw.core.database.DatabaseManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class CrateRepository {
    private final DatabaseManager database;

    public CrateRepository(DatabaseManager database) {
        if (database == null) throw new IllegalArgumentException("DatabaseManager é obrigatório.");
        this.database = database;
    }

    public void initialize() {
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_wallet ("
                    + "player_uuid UUID NOT NULL, crate_type VARCHAR(32) NOT NULL, key_count BIGINT NOT NULL DEFAULT 0 "
                    + "CHECK (key_count >= 0), PRIMARY KEY (player_uuid, crate_type))");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_pity ("
                    + "player_uuid UUID NOT NULL, crate_type VARCHAR(32) NOT NULL, progress_units INTEGER NOT NULL DEFAULT 0 "
                    + "CHECK (progress_units >= 0), PRIMARY KEY (player_uuid, crate_type))");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_opening ("
                    + "opening_id UUID PRIMARY KEY, player_uuid UUID NOT NULL, crate_type VARCHAR(32) NOT NULL, "
                    + "amount INTEGER NOT NULL CHECK (amount > 0), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_physical_crates ("
                    + "id BIGSERIAL PRIMARY KEY, world VARCHAR(128) NOT NULL, block_x INTEGER NOT NULL, "
                    + "block_y INTEGER NOT NULL, block_z INTEGER NOT NULL, crate_type VARCHAR(32) NOT NULL, "
                    + "UNIQUE (world, block_x, block_y, block_z))");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_mail ("
                    + "id BIGSERIAL PRIMARY KEY, player_uuid UUID NOT NULL, item_data BYTEA NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_mutant_crops ("
                    + "location_key VARCHAR(256) PRIMARY KEY, owner_uuid UUID NOT NULL, "
                    + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_crate_player_effects ("
                    + "player_uuid UUID NOT NULL, effect_type VARCHAR(40) NOT NULL, charges BIGINT NOT NULL "
                    + "CHECK (charges >= 0), PRIMARY KEY (player_uuid, effect_type))");
        } catch (SQLException e) {
            throw failure("Falha ao criar as tabelas do módulo tw-caixas.", e);
        }
        database.recordSchemaVersion("tw-caixas", 2);
        refundPendingOpenings();
    }

    public Map<CrateEffect, Long> loadPlayerEffects(UUID playerId) {
        Map<CrateEffect, Long> effects = new EnumMap<>(CrateEffect.class);
        String sql = "SELECT effect_type, charges FROM tw_crate_player_effects WHERE player_uuid = ?";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    CrateEffect effect = CrateEffect.fromId(rows.getString("effect_type"));
                    long charges = rows.getLong("charges");
                    if (effect != null && charges > 0L) effects.put(effect, charges);
                }
            }
            return effects;
        } catch (SQLException e) {
            throw failure("Falha ao carregar consumíveis ativos de " + playerId + ".", e);
        }
    }

    public void savePlayerEffects(UUID playerId, Map<CrateEffect, Long> effects) {
        String delete = "DELETE FROM tw_crate_player_effects WHERE player_uuid = ?";
        String insert = "INSERT INTO tw_crate_player_effects (player_uuid, effect_type, charges) VALUES (?, ?, ?)";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                statement.setObject(1, playerId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                for (Map.Entry<CrateEffect, Long> entry : effects.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0L) {
                        throw new SQLException("Estado de consumível inválido.");
                    }
                    if (entry.getValue() == 0L) continue;
                    statement.setObject(1, playerId);
                    statement.setString(2, entry.getKey().getId());
                    statement.setLong(3, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            throw failure("Falha ao guardar consumíveis ativos de " + playerId + ".", e);
        }
    }

    public CrateAccount loadAccount(UUID playerId) {
        CrateAccount account = new CrateAccount(playerId);
        String wallets = "SELECT crate_type, key_count FROM tw_crate_wallet WHERE player_uuid = ?";
        String pity = "SELECT crate_type, progress_units FROM tw_crate_pity WHERE player_uuid = ?";
        try (Connection connection = database.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(wallets)) {
                statement.setObject(1, playerId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        CrateType type = CrateType.fromId(rows.getString("crate_type"));
                        if (type != null) account.setKeys(type, rows.getLong("key_count"));
                    }
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(pity)) {
                statement.setObject(1, playerId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        CrateType type = CrateType.fromId(rows.getString("crate_type"));
                        if (type != null) account.setPity(type, rows.getInt("progress_units"));
                    }
                }
            }
            return account;
        } catch (SQLException e) {
            throw failure("Falha ao carregar chaves e pity do jogador " + playerId + ".", e);
        }
    }

    public long getKeys(UUID playerId, CrateType type) {
        String sql = "SELECT key_count FROM tw_crate_wallet WHERE player_uuid = ? AND crate_type = ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerId);
            statement.setString(2, type.getId());
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw failure("Falha ao consultar o saldo de chaves.", e);
        }
    }

    public void grantKeys(UUID playerId, CrateType type, long amount) {
        if (amount < 1) throw new IllegalArgumentException("A quantidade de chaves deve ser positiva.");
        String sql = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = tw_crate_wallet.key_count + EXCLUDED.key_count";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerId);
            statement.setString(2, type.getId());
            statement.setLong(3, amount);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao adicionar chaves ao jogador " + playerId + ".", e);
        }
    }

    public boolean transferKeys(UUID from, UUID to, CrateType type, long amount) {
        if (amount < 1 || from.equals(to)) return false;
        String decrement = "UPDATE tw_crate_wallet SET key_count = key_count - ? "
                + "WHERE player_uuid = ? AND crate_type = ? AND key_count >= ?";
        String increment = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = tw_crate_wallet.key_count + EXCLUDED.key_count";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement subtract = connection.prepareStatement(decrement)) {
                subtract.setLong(1, amount);
                subtract.setObject(2, from);
                subtract.setString(3, type.getId());
                subtract.setLong(4, amount);
                if (subtract.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }

            }
            try (PreparedStatement add = connection.prepareStatement(increment)) {
                add.setObject(1, to);
                add.setString(2, type.getId());
                add.setLong(3, amount);
                add.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw failure("Falha ao transferir chaves entre jogadores.", e);
        }
    }

    public boolean forgeKeys(UUID playerId, CrateType fromType, CrateType toType, long cost) {
        if (fromType == toType || cost < 1) return false;
        String decrement = "UPDATE tw_crate_wallet SET key_count = key_count - ? "
                + "WHERE player_uuid = ? AND crate_type = ? AND key_count >= ?";
        String increment = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, 1) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = tw_crate_wallet.key_count + 1";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement subtract = connection.prepareStatement(decrement)) {
                subtract.setLong(1, cost);
                subtract.setObject(2, playerId);
                subtract.setString(3, fromType.getId());
                subtract.setLong(4, cost);
                if (subtract.executeUpdate() != 1) {
                    connection.rollback();
                    return false;
                }
            }
            try (PreparedStatement add = connection.prepareStatement(increment)) {
                add.setObject(1, playerId);
                add.setString(2, toType.getId());
                add.executeUpdate();
            }
            connection.commit();
            return true;
        } catch (SQLException e) {
            throw failure("Falha ao fundir chaves.", e);
        }
    }

    public Optional<UUID> reserveOpening(UUID playerId, CrateType type, int amount) {
        if (amount < 1) throw new IllegalArgumentException("A quantidade de caixas deve ser positiva.");
        UUID openingId = UUID.randomUUID();
        String decrement = "UPDATE tw_crate_wallet SET key_count = key_count - ? "
                + "WHERE player_uuid = ? AND crate_type = ? AND key_count >= ?";
        String insert = "INSERT INTO tw_crate_opening (opening_id, player_uuid, crate_type, amount) VALUES (?, ?, ?, ?)";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement subtract = connection.prepareStatement(decrement)) {
                subtract.setInt(1, amount);
                subtract.setObject(2, playerId);
                subtract.setString(3, type.getId());
                subtract.setInt(4, amount);
                if (subtract.executeUpdate() != 1) {
                    connection.rollback();
                    return Optional.empty();
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(insert)) {
                statement.setObject(1, openingId);
                statement.setObject(2, playerId);
                statement.setString(3, type.getId());
                statement.setInt(4, amount);
                statement.executeUpdate();
            }
            connection.commit();
            return Optional.of(openingId);
        } catch (SQLException e) {
            throw failure("Falha ao reservar chaves para abertura.", e);
        }
    }

    public List<Boolean> completeOpening(UUID playerId, UUID openingId, int threshold, int pityUnitsPerCrate) {
        String lockOpening = "SELECT crate_type, amount FROM tw_crate_opening "
                + "WHERE opening_id = ? AND player_uuid = ? FOR UPDATE";
        String ensurePity = "INSERT INTO tw_crate_pity (player_uuid, crate_type, progress_units) VALUES (?, ?, 0) "
                + "ON CONFLICT (player_uuid, crate_type) DO NOTHING";
        String lockPity = "SELECT progress_units FROM tw_crate_pity "
                + "WHERE player_uuid = ? AND crate_type = ? FOR UPDATE";
        String savePity = "UPDATE tw_crate_pity SET progress_units = ? WHERE player_uuid = ? AND crate_type = ?";
        String delete = "DELETE FROM tw_crate_opening WHERE opening_id = ? AND player_uuid = ?";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            CrateType type;
            int amount;
            try (PreparedStatement statement = connection.prepareStatement(lockOpening)) {
                statement.setObject(1, openingId);
                statement.setObject(2, playerId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Reserva de abertura não encontrada.");
                    type = CrateType.fromId(rows.getString("crate_type"));
                    amount = rows.getInt("amount");
                }
            }
            if (type == null || amount < 1) throw new SQLException("Reserva de abertura inválida.");
            try (PreparedStatement statement = connection.prepareStatement(ensurePity)) {
                statement.setObject(1, playerId);
                statement.setString(2, type.getId());
                statement.executeUpdate();
            }
            int progress;
            try (PreparedStatement statement = connection.prepareStatement(lockPity)) {
                statement.setObject(1, playerId);
                statement.setString(2, type.getId());
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) throw new SQLException("Registro de pity não encontrado.");
                    progress = rows.getInt(1);
                }
            }
            List<Boolean> guarantees = new ArrayList<>(amount);
            for (int i = 0; i < amount; i++) {
                PityProgress.Step step = PityProgress.advance(progress, threshold, pityUnitsPerCrate);
                progress = step.getProgress();
                guarantees.add(step.isGuaranteed());
            }
            try (PreparedStatement statement = connection.prepareStatement(savePity)) {
                statement.setInt(1, progress);
                statement.setObject(2, playerId);
                statement.setString(3, type.getId());
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                statement.setObject(1, openingId);
                statement.setObject(2, playerId);
                if (statement.executeUpdate() != 1) throw new SQLException("A reserva foi alterada durante a conclusão.");
            }
            connection.commit();
            return Collections.unmodifiableList(guarantees);
        } catch (SQLException e) {
            throw failure("Falha ao concluir abertura de caixa.", e);
        }
    }

    public void cancelOpening(UUID playerId, UUID openingId) {
        String find = "SELECT crate_type, amount FROM tw_crate_opening WHERE opening_id = ? AND player_uuid = ? FOR UPDATE";
        String credit = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = tw_crate_wallet.key_count + EXCLUDED.key_count";
        String delete = "DELETE FROM tw_crate_opening WHERE opening_id = ? AND player_uuid = ?";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            String type;
            int amount;
            try (PreparedStatement statement = connection.prepareStatement(find)) {
                statement.setObject(1, openingId);
                statement.setObject(2, playerId);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        connection.rollback();
                        return;
                    }
                    type = rows.getString("crate_type");
                    amount = rows.getInt("amount");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(credit)) {
                statement.setObject(1, playerId);
                statement.setString(2, type);
                statement.setInt(3, amount);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement(delete)) {
                statement.setObject(1, openingId);
                statement.setObject(2, playerId);
                statement.executeUpdate();
            }
            connection.commit();
        } catch (SQLException e) {
            throw failure("Falha ao devolver chaves de uma abertura cancelada.", e);
        }
    }

    public void setKeys(UUID playerId, CrateType type, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Chaves não podem ser negativas.");
        String sql = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = EXCLUDED.key_count";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, playerId);
            statement.setString(2, type.getId());
            statement.setLong(3, amount);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao definir saldo de chaves.", e);
        }
    }

    public void storeMail(UUID playerId, ItemStack item) {
        storeMail(playerId, Collections.singletonList(item));
    }

    public void storeMail(UUID playerId, List<ItemStack> items) {
        String insert = "INSERT INTO tw_crate_mail (player_uuid, item_data) VALUES (?, ?)";
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(insert)) {
            for (ItemStack item : items) {
                if (item == null || item.getType() == org.bukkit.Material.AIR || item.getAmount() < 1) continue;
                try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                     BukkitObjectOutputStream output = new BukkitObjectOutputStream(bytes)) {
                    output.writeObject(item);
                    output.flush();
                    statement.setObject(1, playerId);
                    statement.setBytes(2, bytes.toByteArray());
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        } catch (IOException e) {
            throw failure("Falha ao serializar item excedente para o correio de caixas.",
                    new SQLException("Falha ao serializar item de caixa.", e));
        } catch (SQLException e) {
            throw failure("Falha ao guardar item excedente no correio de caixas.", e);
        }
    }

    public List<ItemStack> claimMail(UUID playerId) {
        String select = "SELECT id, item_data FROM tw_crate_mail WHERE player_uuid = ? ORDER BY id FOR UPDATE";
        List<Long> ids = new ArrayList<>();
        List<ItemStack> items = new ArrayList<>();
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(select)) {
                statement.setObject(1, playerId);
                try (ResultSet rows = statement.executeQuery()) {
                    while (rows.next()) {
                        ids.add(rows.getLong("id"));
                        try (BukkitObjectInputStream input = new BukkitObjectInputStream(
                                new ByteArrayInputStream(rows.getBytes("item_data")))) {
                            items.add((ItemStack) input.readObject());
                        } catch (IOException | ClassNotFoundException e) {
                            throw new SQLException("Item inválido no correio de caixas.", e);
                        }
                    }
                }
            }
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM tw_crate_mail WHERE id = ?")) {
                for (Long id : ids) {
                    delete.setLong(1, id);
                    delete.addBatch();
                }
                delete.executeBatch();
            }
            connection.commit();
            return items;
        } catch (SQLException e) {
            throw failure("Falha ao recolher itens do correio de caixas.", e);
        }
    }

    public Set<String> loadMutantCrops() {
        Set<String> crops = new HashSet<>();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT location_key FROM tw_crate_mutant_crops")) {
            while (rows.next()) crops.add(rows.getString(1));
            return crops;
        } catch (SQLException e) {
            throw failure("Falha ao carregar plantações mutantes.", e);
        }
    }

    public boolean markMutantCrop(String locationKey, UUID ownerId) {
        String sql = "INSERT INTO tw_crate_mutant_crops (location_key, owner_uuid) VALUES (?, ?) "
                + "ON CONFLICT (location_key) DO NOTHING";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, locationKey);
            statement.setObject(2, ownerId);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("Falha ao marcar plantação mutante.", e);
        }
    }

    public List<PhysicalCrate> loadPhysicalCrates() {
        List<PhysicalCrate> crates = new ArrayList<>();
        String sql = "SELECT id, world, block_x, block_y, block_z, crate_type FROM tw_physical_crates";
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                CrateType type = CrateType.fromId(rows.getString("crate_type"));
                if (type != null) {
                    crates.add(new PhysicalCrate(rows.getLong("id"), rows.getString("world"),
                            rows.getInt("block_x"), rows.getInt("block_y"), rows.getInt("block_z"), type));
                }
            }
            return crates;
        } catch (SQLException e) {
            throw failure("Falha ao carregar caixas físicas.", e);
        }
    }

    public void addPhysicalCrate(String world, int x, int y, int z, CrateType type) {
        String sql = "INSERT INTO tw_physical_crates (world, block_x, block_y, block_z, crate_type) "
                + "VALUES (?, ?, ?, ?, ?) ON CONFLICT (world, block_x, block_y, block_z) "
                + "DO UPDATE SET crate_type = EXCLUDED.crate_type";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.setString(5, type.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao configurar caixa física.", e);
        }
    }

    public boolean removePhysicalCrate(String world, int x, int y, int z) {
        String sql = "DELETE FROM tw_physical_crates WHERE world = ? AND block_x = ? AND block_y = ? AND block_z = ?";
        try (Connection connection = database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Falha ao remover caixa física.", e);
        }
    }

    private void refundPendingOpenings() {
        String find = "SELECT opening_id, player_uuid, crate_type, amount FROM tw_crate_opening FOR UPDATE";
        String credit = "INSERT INTO tw_crate_wallet (player_uuid, crate_type, key_count) VALUES (?, ?, ?) "
                + "ON CONFLICT (player_uuid, crate_type) DO UPDATE SET key_count = tw_crate_wallet.key_count + EXCLUDED.key_count";
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            List<PendingOpening> pending = new ArrayList<>();
            try (Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(find)) {
                while (rows.next()) {
                    pending.add(new PendingOpening((UUID) rows.getObject("opening_id"),
                            (UUID) rows.getObject("player_uuid"), rows.getString("crate_type"), rows.getInt("amount")));
                }
            }
            try (PreparedStatement add = connection.prepareStatement(credit);
                 PreparedStatement remove = connection.prepareStatement("DELETE FROM tw_crate_opening WHERE opening_id = ?")) {
                for (PendingOpening opening : pending) {
                    add.setObject(1, opening.playerId);
                    add.setString(2, opening.type);
                    add.setInt(3, opening.amount);
                    add.addBatch();
                    remove.setObject(1, opening.id);
                    remove.addBatch();
                }
                add.executeBatch();
                remove.executeBatch();
            }
            connection.commit();
        } catch (SQLException e) {
            throw failure("Falha ao recuperar chaves de aberturas interrompidas.", e);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        database.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }

    private static final class PendingOpening {
        private final UUID id;
        private final UUID playerId;
        private final String type;
        private final int amount;

        private PendingOpening(UUID id, UUID playerId, String type, int amount) {
            this.id = id;
            this.playerId = playerId;
            this.type = type;
            this.amount = amount;
        }
    }
}
