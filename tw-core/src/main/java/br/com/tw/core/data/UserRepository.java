package br.com.tw.core.data;

import br.com.tw.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class UserRepository {

    private final DatabaseManager db;

    public UserRepository(DatabaseManager db) {
        this.db = db;
        createTable();
    }

    private void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS tw_users (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "name VARCHAR(16), " +
                "rank_id VARCHAR(32) DEFAULT 'CARVAO', " +
                "prestige_id VARCHAR(32) DEFAULT 'NENHUM', " +
                "tokens BIGINT DEFAULT 0, " +
                "blocos_quebrados BIGINT DEFAULT 0, " +
                "dinheiro_mina DOUBLE PRECISION DEFAULT 0.0, " +
                "balance DOUBLE PRECISION DEFAULT 0.0, " +
                "pic_level INT DEFAULT 1, " +
                "pic_xp DOUBLE PRECISION DEFAULT 0.0, " +
                "pic_skills VARCHAR(500) DEFAULT '', " +
                "missoes_data VARCHAR(500) DEFAULT ''" +
                ")";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS name VARCHAR(16)");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS rank_id VARCHAR(32) DEFAULT 'CARVAO'");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS prestige_id VARCHAR(32) DEFAULT 'NENHUM'");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS tokens BIGINT DEFAULT 0");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS blocos_quebrados BIGINT DEFAULT 0");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS dinheiro_mina DOUBLE PRECISION DEFAULT 0.0");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS balance DOUBLE PRECISION DEFAULT 0.0");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS pic_level INT DEFAULT 1");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS pic_xp DOUBLE PRECISION DEFAULT 0.0");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS pic_skills VARCHAR(500) DEFAULT ''");
            stmt.execute("ALTER TABLE tw_users ADD COLUMN IF NOT EXISTS missoes_data VARCHAR(500) DEFAULT ''");
        } catch (SQLException e) {
            throw failure("Falha ao criar ou atualizar a tabela de jogadores.", e);
        }
        db.recordSchemaVersion("tw_core", 2);
    }

    public Optional<TWUser> loadUser(UUID uuid, String name) {
        String query = "SELECT * FROM tw_users WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();

                TWUser user = new TWUser(uuid, name, rs.getLong("tokens"),
                        rs.getString("rank_id"), rs.getString("prestige_id"));
                user.setBalance(rs.getDouble("balance"));
                user.setBlocosQuebrados(rs.getLong("blocos_quebrados"));
                user.setDinheiroMina(rs.getDouble("dinheiro_mina"));
                user.setPickaxeLevel(rs.getInt("pic_level"));
                user.setPickaxeXp(rs.getDouble("pic_xp"));
                user.loadSkillsFromString(rs.getString("pic_skills"));
                user.setMissoesData(rs.getString("missoes_data"));
                user.setDirty(false);
                return Optional.of(user);
            }
        } catch (SQLException | RuntimeException e) {
            throw failure("Falha ao carregar os dados do jogador " + uuid + ".", e);
        }
    }

    public void saveUser(TWUserSnapshot user) {
        String query = "INSERT INTO tw_users (uuid, name, tokens, rank_id, prestige_id, balance, blocos_quebrados, dinheiro_mina, pic_level, pic_xp, pic_skills, missoes_data) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (uuid) DO UPDATE SET name = EXCLUDED.name, tokens = EXCLUDED.tokens, rank_id = EXCLUDED.rank_id, " +
                "prestige_id = EXCLUDED.prestige_id, balance = EXCLUDED.balance, blocos_quebrados = EXCLUDED.blocos_quebrados, " +
                "dinheiro_mina = EXCLUDED.dinheiro_mina, pic_level = EXCLUDED.pic_level, pic_xp = EXCLUDED.pic_xp, " +
                "pic_skills = EXCLUDED.pic_skills, missoes_data = EXCLUDED.missoes_data";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, user.getUuid().toString());
            ps.setString(2, user.getName());
            ps.setLong(3, user.getTokens());
            ps.setString(4, user.getRankId());
            ps.setString(5, user.getPrestigeId());
            ps.setDouble(6, user.getBalance());
            ps.setLong(7, user.getBlocksBroken());
            ps.setDouble(8, user.getMineMoney());
            ps.setInt(9, user.getPickaxeLevel());
            ps.setDouble(10, user.getPickaxeXp());
            ps.setString(11, user.getPickaxeSkills());
            ps.setString(12, user.getMissionsData());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao salvar os dados do jogador " + user.getUuid() + ".", e);
        }
    }

    public Map<String, Double> getTopBalances(int limit) {
        Map<String, Double> top = new LinkedHashMap<>();
        String query = "SELECT name, balance FROM tw_users ORDER BY balance DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, Math.max(0, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) top.put(rs.getString("name"), rs.getDouble("balance"));
            }
        } catch (SQLException e) {
            throw failure("Falha ao consultar o ranking de saldo.", e);
        }
        return top;
    }

    public boolean updateOfflineBalance(String playerName, double amount, String action) {
        if (!Double.isFinite(amount) || amount < 0 || action == null) return false;

        String sql;
        switch (action.toLowerCase()) {
            case "give":
            case "add":
                sql = "UPDATE tw_users SET balance = balance + ? WHERE LOWER(name) = LOWER(?)";
                break;
            case "take":
            case "remove":
                sql = "UPDATE tw_users SET balance = GREATEST(0, balance - ?) WHERE LOWER(name) = LOWER(?)";
                break;
            case "set":
                sql = "UPDATE tw_users SET balance = ? WHERE LOWER(name) = LOWER(?)";
                break;
            default:
                return false;
        }

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setString(2, playerName);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Falha ao atualizar o saldo offline de " + playerName + ".", e);
        }
    }

    public Map<String, Long> getTopMiners(int limit) {
        Map<String, Long> top = new LinkedHashMap<>();
        String query = "SELECT name, blocos_quebrados FROM tw_users ORDER BY blocos_quebrados DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, Math.max(0, limit));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) top.put(rs.getString("name"), rs.getLong("blocos_quebrados"));
            }
        } catch (SQLException e) {
            throw failure("Falha ao consultar o ranking de mineradores.", e);
        }
        return top;
    }

    private IllegalStateException failure(String message, Exception cause) {
        db.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }
}
