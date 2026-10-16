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
                "spawner_level INT DEFAULT 1, " +
                "enabled BOOLEAN DEFAULT TRUE, " +
                "total_kills BIGINT DEFAULT 0" +
                ")";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN spawner_level INT DEFAULT 1"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN enabled BOOLEAN DEFAULT TRUE"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_spawners ADD COLUMN total_kills BIGINT DEFAULT 0"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, PlacedSpawner> loadAllSpawners() {
        Map<String, PlacedSpawner> map = new HashMap<>();
        String query = "SELECT * FROM tw_spawners";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlacedSpawner spawner = new PlacedSpawner(
                        rs.getString("loc_key"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("owner_name"),
                        rs.getString("type_id"),
                        rs.getLong("stack_amount"),
                        rs.getInt("spawner_level"),
                        rs.getBoolean("enabled"),
                        rs.getLong("total_kills")
                );
                map.put(spawner.getLocationKey(), spawner);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void saveSpawner(PlacedSpawner s) {
        String query = "INSERT INTO tw_spawners (loc_key, owner_uuid, owner_name, type_id, stack_amount, spawner_level, enabled, total_kills) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE stack_amount=VALUES(stack_amount), spawner_level=VALUES(spawner_level), " +
                "enabled=VALUES(enabled), total_kills=VALUES(total_kills)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, s.getLocationKey());
            ps.setString(2, s.getOwnerUuid().toString());
            ps.setString(3, s.getOwnerName());
            ps.setString(4, s.getTypeId());
            ps.setLong(5, s.getStackAmount());
            ps.setInt(6, s.getLevel());
            ps.setBoolean(7, s.getEnabled());
            ps.setLong(8, s.getTotalKills());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteSpawner(String locKey) {
        String query = "DELETE FROM tw_spawners WHERE loc_key = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, locKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}