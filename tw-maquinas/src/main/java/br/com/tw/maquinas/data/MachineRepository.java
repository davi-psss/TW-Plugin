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
                "fuel BIGINT DEFAULT 0, " +
                "stored_earnings DOUBLE DEFAULT 0.0, " +
                "speed_level INT DEFAULT 1, " +
                "storage_level INT DEFAULT 1, " +
                "fuel_eff_level INT DEFAULT 1, " +
                "total_generated DOUBLE DEFAULT 0.0" +
                ")";

        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);

            // Tenta adicionar as colunas novas caso a tabela antiga já exista (Atualização Silenciosa)
            try { stmt.execute("ALTER TABLE tw_machines ADD COLUMN speed_level INT DEFAULT 1"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_machines ADD COLUMN storage_level INT DEFAULT 1"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_machines ADD COLUMN fuel_eff_level INT DEFAULT 1"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE tw_machines ADD COLUMN total_generated DOUBLE DEFAULT 0.0"); } catch (SQLException ignored) {}

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
                        rs.getLong("fuel"),
                        rs.getDouble("stored_earnings"),
                        rs.getInt("speed_level"),
                        rs.getInt("storage_level"),
                        rs.getInt("fuel_eff_level"),
                        rs.getDouble("total_generated")
                );
                map.put(machine.getLocationKey(), machine);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public void saveMachine(PlacedMachine m) {
        String query = "INSERT INTO tw_machines (loc_key, owner_uuid, owner_name, type_id, stack_amount, fuel, stored_earnings, speed_level, storage_level, fuel_eff_level, total_generated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE stack_amount=VALUES(stack_amount), fuel=VALUES(fuel), stored_earnings=VALUES(stored_earnings), " +
                "speed_level=VALUES(speed_level), storage_level=VALUES(storage_level), fuel_eff_level=VALUES(fuel_eff_level), total_generated=VALUES(total_generated)";

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, m.getLocationKey());
            ps.setString(2, m.getOwnerUuid().toString());
            ps.setString(3, m.getOwnerName());
            ps.setString(4, m.getTypeId());
            ps.setLong(5, m.getStackAmount());
            ps.setLong(6, m.getFuel());
            ps.setDouble(7, m.getStoredEarnings());
            ps.setInt(8, m.getSpeedLevel());
            ps.setInt(9, m.getStorageLevel());
            ps.setInt(10, m.getFuelEfficiencyLevel());
            ps.setDouble(11, m.getTotalGenerated());

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