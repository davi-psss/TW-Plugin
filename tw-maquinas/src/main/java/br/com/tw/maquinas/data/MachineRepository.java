package br.com.tw.maquinas.data;

import br.com.tw.core.database.DatabaseManager;
import br.com.tw.maquinas.models.PlacedMachine;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MachineRepository {

    private final DatabaseManager db;

    public MachineRepository(DatabaseManager db) {
        this.db = db;
        createTable();
    }

    private void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS tw_machines (" +
                "loc_key VARCHAR(128) PRIMARY KEY, " +
                "owner_uuid VARCHAR(36) NOT NULL, " +
                "owner_name VARCHAR(16) NOT NULL, " +
                "type_id VARCHAR(32) NOT NULL, " +
                "stack_amount BIGINT DEFAULT 1, " +
                "machine_level INT DEFAULT 1, " +
                "fuel BIGINT DEFAULT 0, " +
                "stored_earnings DOUBLE DEFAULT 0.0" +
                ")";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, PlacedMachine> loadAllMachines() {
        Map<String, PlacedMachine> map = new HashMap<>();
        String query = "SELECT * FROM tw_machines";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PlacedMachine machine = new PlacedMachine(
                        rs.getString("loc_key"),
                        UUID.fromString(rs.getString("owner_uuid")),
                        rs.getString("owner_name"),
                        rs.getString("type_id"),
                        rs.getLong("stack_amount"),
                        rs.getInt("machine_level"),
                        rs.getLong("fuel"),
                        rs.getDouble("stored_earnings")
                );
                map.put(machine.getLocationKey(), machine);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void saveMachine(PlacedMachine m) {
        String query = "INSERT INTO tw_machines (loc_key, owner_uuid, owner_name, type_id, stack_amount, machine_level, fuel, stored_earnings) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE stack_amount=VALUES(stack_amount), machine_level=VALUES(machine_level), " +
                "fuel=VALUES(fuel), stored_earnings=VALUES(stored_earnings)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, m.getLocationKey());
            ps.setString(2, m.getOwnerUuid().toString());
            ps.setString(3, m.getOwnerName());
            ps.setString(4, m.getTypeId());
            ps.setLong(5, m.getStackAmount());
            ps.setInt(6, m.getLevel());
            ps.setLong(7, m.getFuel());
            ps.setDouble(8, m.getStoredEarnings());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteMachine(String locKey) {
        String query = "DELETE FROM tw_machines WHERE loc_key = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, locKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}