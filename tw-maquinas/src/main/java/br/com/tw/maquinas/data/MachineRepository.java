package br.com.tw.maquinas.data;

import br.com.tw.core.database.DatabaseManager;
import br.com.tw.maquinas.models.PlacedMachine;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

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
                "stored_earnings DOUBLE PRECISION DEFAULT 0.0, " +
                "speed_level INT DEFAULT 1, " +
                "storage_level INT DEFAULT 1, " +
                "fuel_eff_level INT DEFAULT 1, " +
                "total_generated DOUBLE PRECISION DEFAULT 0.0" +
                ")";

        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);

            stmt.execute("ALTER TABLE tw_machines ADD COLUMN IF NOT EXISTS speed_level INT DEFAULT 1");
            stmt.execute("ALTER TABLE tw_machines ADD COLUMN IF NOT EXISTS storage_level INT DEFAULT 1");
            stmt.execute("ALTER TABLE tw_machines ADD COLUMN IF NOT EXISTS fuel_eff_level INT DEFAULT 1");
            stmt.execute("ALTER TABLE tw_machines ADD COLUMN IF NOT EXISTS total_generated DOUBLE PRECISION DEFAULT 0.0");
        } catch (SQLException e) {
            throw failure("Falha ao criar ou atualizar a tabela de máquinas.", e);
        }
        db.recordSchemaVersion("tw_maquinas", 2);
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
            throw failure("Falha ao carregar as máquinas.", e);
        }
        return map;
    }

    public void saveMachine(PlacedMachine m) {
        String query = "INSERT INTO tw_machines (loc_key, owner_uuid, owner_name, type_id, stack_amount, fuel, stored_earnings, speed_level, storage_level, fuel_eff_level, total_generated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT (loc_key) DO UPDATE SET stack_amount = EXCLUDED.stack_amount, fuel = EXCLUDED.fuel, " +
                "stored_earnings = EXCLUDED.stored_earnings, speed_level = EXCLUDED.speed_level, " +
                "storage_level = EXCLUDED.storage_level, fuel_eff_level = EXCLUDED.fuel_eff_level, " +
                "total_generated = EXCLUDED.total_generated";

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
            throw failure("Falha ao salvar máquina em " + m.getLocationKey() + ".", e);
        }
    }

    public void deleteMachine(String locKey) {
        String query = "DELETE FROM tw_machines WHERE loc_key = ?";

        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, locKey);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw failure("Falha ao remover máquina em " + locKey + ".", e);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        db.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }
}