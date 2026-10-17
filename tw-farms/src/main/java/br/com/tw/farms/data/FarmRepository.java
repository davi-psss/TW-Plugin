package br.com.tw.farms.data;

import br.com.tw.core.database.DatabaseManager;
import br.com.tw.farms.models.FarmStorage;
import br.com.tw.farms.models.PhysicalCrop;
import br.com.tw.farms.models.PlacedFarm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class FarmRepository {
    private final DatabaseManager db;

    public FarmRepository(DatabaseManager db) {
        this.db = db;
        createTables();
    }

    private void createTables() {
        try (Connection connection = db.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS tw_farms ("
                    + "loc_key VARCHAR(128) PRIMARY KEY, owner_uuid VARCHAR(36) NOT NULL, "
                    + "owner_name VARCHAR(16) NOT NULL, type_id VARCHAR(32) NOT NULL, "
                    + "stack_amount BIGINT NOT NULL DEFAULT 1, fertilizer BIGINT NOT NULL DEFAULT 0, "
                    + "soil_level INT NOT NULL DEFAULT 1, stored BIGINT NOT NULL DEFAULT 0, "
                    + "stored_golden BIGINT NOT NULL DEFAULT 0, total_harvested BIGINT NOT NULL DEFAULT 0, "
                    + "total_sold BIGINT NOT NULL DEFAULT 0, speed_level INT NOT NULL DEFAULT 1, "
                    + "storage_level INT NOT NULL DEFAULT 1, quality_level INT NOT NULL DEFAULT 1, "
                    + "auto_sell BOOLEAN NOT NULL DEFAULT FALSE, auto_sell_unlocked BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "booster_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0, "
                    + "booster_end BIGINT NOT NULL DEFAULT 0, last_harvest BIGINT NOT NULL DEFAULT 0, "
                    + "last_tick BIGINT NOT NULL DEFAULT 0)");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_farm_crops ("
                    + "loc_key VARCHAR(128) PRIMARY KEY, owner_uuid VARCHAR(36) NOT NULL, "
                    + "owner_name VARCHAR(16) NOT NULL, type_id VARCHAR(32) NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS tw_farm_storage ("
                    + "owner_uuid VARCHAR(36) NOT NULL, type_id VARCHAR(32) NOT NULL, "
                    + "stored BIGINT NOT NULL DEFAULT 0, stored_golden BIGINT NOT NULL DEFAULT 0, "
                    + "total_harvested BIGINT NOT NULL DEFAULT 0, total_sold BIGINT NOT NULL DEFAULT 0, "
                    + "auto_sell BOOLEAN NOT NULL DEFAULT FALSE, auto_sell_unlocked BOOLEAN NOT NULL DEFAULT FALSE, "
                    + "growth_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0, growth_boost_end BIGINT NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (owner_uuid, type_id))");
            statement.execute("ALTER TABLE tw_farms ADD COLUMN IF NOT EXISTS auto_sell_unlocked BOOLEAN NOT NULL DEFAULT FALSE");
            statement.execute("ALTER TABLE tw_farm_storage ADD COLUMN IF NOT EXISTS auto_sell BOOLEAN NOT NULL DEFAULT FALSE");
            statement.execute("ALTER TABLE tw_farm_storage ADD COLUMN IF NOT EXISTS auto_sell_unlocked BOOLEAN NOT NULL DEFAULT FALSE");
            statement.execute("ALTER TABLE tw_farm_storage ADD COLUMN IF NOT EXISTS growth_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0");
            statement.execute("ALTER TABLE tw_farm_storage ADD COLUMN IF NOT EXISTS growth_boost_end BIGINT NOT NULL DEFAULT 0");
        } catch (SQLException e) {
            throw failure("Falha ao criar as tabelas do módulo de farms.", e);
        }
        db.recordSchemaVersion("tw_farms", 3);
    }

    public Map<String, PlacedFarm> loadFarms() {
        Map<String, PlacedFarm> farms = new HashMap<>();
        String query = "SELECT * FROM tw_farms";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                PlacedFarm farm = new PlacedFarm(results.getString("loc_key"),
                        UUID.fromString(results.getString("owner_uuid")), results.getString("owner_name"),
                        results.getString("type_id"), results.getLong("stack_amount"),
                        results.getLong("fertilizer"), results.getInt("soil_level"),
                        results.getLong("stored"), results.getLong("stored_golden"),
                        results.getLong("total_harvested"), results.getLong("total_sold"),
                        results.getInt("speed_level"), results.getInt("storage_level"),
                        results.getInt("quality_level"), results.getBoolean("auto_sell"),
                        results.getBoolean("auto_sell_unlocked"),
                        results.getDouble("booster_multiplier"), results.getLong("booster_end"),
                        results.getLong("last_harvest"), results.getLong("last_tick"));
                farms.put(farm.getLocationKey(), farm);
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw failure("Falha ao carregar farms.", e);
        }
        return farms;
    }

    public Map<String, PhysicalCrop> loadCrops() {
        Map<String, PhysicalCrop> crops = new HashMap<>();
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tw_farm_crops");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                PhysicalCrop crop = new PhysicalCrop(results.getString("loc_key"),
                        UUID.fromString(results.getString("owner_uuid")),
                        results.getString("owner_name"), results.getString("type_id"));
                crops.put(crop.getLocationKey(), crop);
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw failure("Falha ao carregar plantações físicas.", e);
        }
        return crops;
    }

    public Map<String, FarmStorage> loadStorage() {
        Map<String, FarmStorage> storage = new HashMap<>();
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM tw_farm_storage");
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                FarmStorage item = new FarmStorage(UUID.fromString(results.getString("owner_uuid")),
                        results.getString("type_id"), results.getLong("stored"),
                        results.getLong("stored_golden"), results.getLong("total_harvested"),
                        results.getLong("total_sold"), results.getBoolean("auto_sell"),
                        results.getBoolean("auto_sell_unlocked"), results.getDouble("growth_multiplier"),
                        results.getLong("growth_boost_end"));
                storage.put(storageKey(item.getOwnerUuid(), item.getTypeId()), item);
            }
        } catch (SQLException | IllegalArgumentException e) {
            throw failure("Falha ao carregar armazéns de colheitas.", e);
        }
        return storage;
    }

    public void saveFarm(PlacedFarm farm) {
        String query = "INSERT INTO tw_farms (loc_key, owner_uuid, owner_name, type_id, stack_amount, "
                + "fertilizer, soil_level, stored, stored_golden, total_harvested, total_sold, "
                + "speed_level, storage_level, quality_level, auto_sell, auto_sell_unlocked, booster_multiplier, booster_end, "
                + "last_harvest, last_tick) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (loc_key) DO UPDATE SET stack_amount = EXCLUDED.stack_amount, "
                + "fertilizer = EXCLUDED.fertilizer, soil_level = EXCLUDED.soil_level, stored = EXCLUDED.stored, "
                + "stored_golden = EXCLUDED.stored_golden, total_harvested = EXCLUDED.total_harvested, "
                + "total_sold = EXCLUDED.total_sold, speed_level = EXCLUDED.speed_level, "
                + "storage_level = EXCLUDED.storage_level, quality_level = EXCLUDED.quality_level, "
                + "auto_sell = EXCLUDED.auto_sell, auto_sell_unlocked = EXCLUDED.auto_sell_unlocked, "
                + "booster_multiplier = EXCLUDED.booster_multiplier, "
                + "booster_end = EXCLUDED.booster_end, last_harvest = EXCLUDED.last_harvest, "
                + "last_tick = EXCLUDED.last_tick";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, farm.getLocationKey());
            statement.setString(2, farm.getOwnerUuid().toString());
            statement.setString(3, farm.getOwnerName());
            statement.setString(4, farm.getTypeId());
            statement.setLong(5, farm.getStackAmount());
            statement.setLong(6, farm.getFertilizer());
            statement.setInt(7, farm.getSoilLevel());
            statement.setLong(8, farm.getStored());
            statement.setLong(9, farm.getStoredGolden());
            statement.setLong(10, farm.getTotalHarvested());
            statement.setLong(11, farm.getTotalSold());
            statement.setInt(12, farm.getSpeedLevel());
            statement.setInt(13, farm.getStorageLevel());
            statement.setInt(14, farm.getQualityLevel());
            statement.setBoolean(15, farm.isAutoSell());
            statement.setBoolean(16, farm.isAutoSellUnlocked());
            statement.setDouble(17, farm.getBoosterMultiplier());
            statement.setLong(18, farm.getBoosterEndTime());
            statement.setLong(19, farm.getLastHarvestTime());
            statement.setLong(20, farm.getLastTickEpochSeconds());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao salvar farm em " + farm.getLocationKey() + ".", e);
        }
    }

    public void saveCrop(PhysicalCrop crop) {
        String query = "INSERT INTO tw_farm_crops (loc_key, owner_uuid, owner_name, type_id) VALUES (?, ?, ?, ?) "
                + "ON CONFLICT (loc_key) DO UPDATE SET owner_uuid = EXCLUDED.owner_uuid, "
                + "owner_name = EXCLUDED.owner_name, type_id = EXCLUDED.type_id";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, crop.getLocationKey());
            statement.setString(2, crop.getOwnerUuid().toString());
            statement.setString(3, crop.getOwnerName());
            statement.setString(4, crop.getTypeId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao salvar planta em " + crop.getLocationKey() + ".", e);
        }
    }

    public void saveStorage(FarmStorage storage) {
        String query = "INSERT INTO tw_farm_storage (owner_uuid, type_id, stored, stored_golden, "
                + "total_harvested, total_sold, auto_sell, auto_sell_unlocked, growth_multiplier, growth_boost_end) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (owner_uuid, type_id) DO UPDATE SET stored = EXCLUDED.stored, "
                + "stored_golden = EXCLUDED.stored_golden, total_harvested = EXCLUDED.total_harvested, "
                + "total_sold = EXCLUDED.total_sold, auto_sell = EXCLUDED.auto_sell, "
                + "auto_sell_unlocked = EXCLUDED.auto_sell_unlocked, "
                + "growth_multiplier = EXCLUDED.growth_multiplier, growth_boost_end = EXCLUDED.growth_boost_end";
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, storage.getOwnerUuid().toString());
            statement.setString(2, storage.getTypeId());
            statement.setLong(3, storage.getStored());
            statement.setLong(4, storage.getStoredGolden());
            statement.setLong(5, storage.getTotalHarvested());
            statement.setLong(6, storage.getTotalSold());
            statement.setBoolean(7, storage.isAutoSell());
            statement.setBoolean(8, storage.isAutoSellUnlocked());
            statement.setDouble(9, storage.getGrowthMultiplier());
            statement.setLong(10, storage.getGrowthBoostEndTime());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao salvar silo de " + storage.getOwnerUuid() + ".", e);
        }
    }

    public void deleteFarm(String key) {
        executeDelete("DELETE FROM tw_farms WHERE loc_key = ?", key);
    }

    public void deleteCrop(String key) {
        executeDelete("DELETE FROM tw_farm_crops WHERE loc_key = ?", key);
    }

    private void executeDelete(String query, String key) {
        try (Connection connection = db.getConnection(); PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, key);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao remover registo " + key + ".", e);
        }
    }

    public static String storageKey(UUID owner, String typeId) {
        return owner.toString() + ";" + typeId.toUpperCase(java.util.Locale.ROOT);
    }

    private IllegalStateException failure(String message, Exception cause) {
        db.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }
}
