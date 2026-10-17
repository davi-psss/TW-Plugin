package br.com.tw.spawners.data;

import br.com.tw.core.database.DatabaseManager;
import br.com.tw.spawners.models.PlacedSpawner;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnerRepository {

    private final DatabaseManager db;

    public SpawnerRepository(DatabaseManager db) {
        this.db = db;
        createTable();
    }

    private void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS tw_spawners (" +
                "loc_key VARCHAR(128) PRIMARY KEY, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "owner_name VARCHAR(16) NOT NULL, " +
                "type_id VARCHAR(32) NOT NULL, " +
                "stack_amount BIGINT DEFAULT 1, " +
                "last_spawn_time BIGINT DEFAULT 0, " +
                "speed_level INT DEFAULT 1, " +
                "loot_level INT DEFAULT 1, " +
                "amount_level INT DEFAULT 1, " +
                "virtual_mode BOOLEAN DEFAULT FALSE, " +
                "auto_sell BOOLEAN DEFAULT FALSE, " +
                "stored_money DOUBLE DEFAULT 0.0, " +
                "stored_tokens BIGINT DEFAULT 0, " +
                "stored_xp BIGINT DEFAULT 0, " +
                "stored_souls BIGINT DEFAULT 0, " +
                "total_kills BIGINT DEFAULT 0, " +
                "booster_mult DOUBLE DEFAULT 1.0, " +
                "booster_end BIGINT DEFAULT 0" +
                ")";

        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN auto_sell BOOLEAN DEFAULT FALSE"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN stored_xp BIGINT DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN stored_souls BIGINT DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN booster_mult DOUBLE DEFAULT 1.0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN booster_end BIGINT DEFAULT 0"); } catch (SQLException ignored) {}
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public Map<String, PlacedSpawner> loadAllSpawners() {
        Map<String, PlacedSpawner> map = new HashMap<>();
        String query = "SELECT * FROM tw_spawners";

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlacedSpawner spawner = new PlacedSpawner(
                        rs.getString("loc_key"), UUID.fromString(rs.getString("owner_uuid")), rs.getString("owner_name"),
                        rs.getString("type_id"), rs.getLong("stack_amount"), rs.getLong("last_spawn_time"),
                        rs.getInt("speed_level"), rs.getInt("loot_level"), rs.getInt("amount_level"),
                        rs.getBoolean("virtual_mode"), rs.getBoolean("auto_sell"), rs.getDouble("stored_money"),
                        rs.getLong("stored_tokens"), rs.getLong("stored_xp"), rs.getLong("stored_souls"),
                        rs.getLong("total_kills"), rs.getDouble("booster_mult"), rs.getLong("booster_end")
                );
                map.put(spawner.getLocationKey(), spawner);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return map;
    }

    public void saveSpawner(PlacedSpawner s) {
        String query = "INSERT INTO tw_spawners (loc_key, owner_uuid, owner_name, type_id, stack_amount, last_spawn_time, speed_level, loot_level, amount_level, virtual_mode, auto_sell, stored_money, stored_tokens, stored_xp, stored_souls, total_kills, booster_mult, booster_end) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE stack_amount=VALUES(stack_amount), last_spawn_time=VALUES(last_spawn_time), speed_level=VALUES(speed_level), " +
                "loot_level=VALUES(loot_level), amount_level=VALUES(amount_level), virtual_mode=VALUES(virtual_mode), auto_sell=VALUES(auto_sell), " +
                "stored_money=VALUES(stored_money), stored_tokens=VALUES(stored_tokens), stored_xp=VALUES(stored_xp), stored_souls=VALUES(stored_souls), " +
                "total_kills=VALUES(total_kills), booster_mult=VALUES(booster_mult), booster_end=VALUES(booster_end)";

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, s.getLocationKey()); ps.setString(2, s.getOwnerUuid().toString()); ps.setString(3, s.getOwnerName());
            ps.setString(4, s.getTypeId()); ps.setLong(5, s.getStackAmount()); ps.setLong(6, s.getLastSpawnTime());
            ps.setInt(7, s.getSpeedLevel()); ps.setInt(8, s.getLootLevel()); ps.setInt(9, s.getAmountLevel());
            ps.setBoolean(10, s.isVirtualMode()); ps.setBoolean(11, s.isAutoSell()); ps.setDouble(12, s.getStoredMoney());
            ps.setLong(13, s.getStoredTokens()); ps.setLong(14, s.getStoredXp()); ps.setLong(15, s.getStoredSouls());
            ps.setLong(16, s.getTotalKills()); ps.setDouble(17, s.getBoosterMultiplier()); ps.setLong(18, s.getBoosterEndTime());
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void deleteSpawner(String locKey) {
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM tw_spawners WHERE loc_key = ?")) {
            ps.setString(1, locKey); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}