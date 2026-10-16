package br.com.tw.core.data;

import br.com.tw.core.database.DatabaseManager;
import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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
                "dinheiro_mina DOUBLE DEFAULT 0.0, " +
                "balance DOUBLE DEFAULT 0.0, " +
                "pic_level INT DEFAULT 1, " +
                "pic_xp DOUBLE DEFAULT 0.0, " +
                "pic_skills VARCHAR(500) DEFAULT '', " +
                "missoes_data VARCHAR(500) DEFAULT ''" +
                ")";
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(query);
            // Garante a criação da coluna em bancos antigos sem dar erro
            try {
                stmt.execute("ALTER TABLE tw_users ADD COLUMN missoes_data VARCHAR(500) DEFAULT ''");
            } catch (SQLException ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public TWUser loadUser(UUID uuid, String name) {
        String query = "SELECT * FROM tw_users WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    TWUser user = new TWUser(uuid, name, rs.getLong("tokens"), rs.getString("rank_id"), rs.getString("prestige_id"));
                    user.setBalance(rs.getDouble("balance"));
                    user.setBlocosQuebrados(rs.getLong("blocos_quebrados"));
                    user.setDinheiroMina(rs.getDouble("dinheiro_mina"));
                    user.setPickaxeLevel(rs.getInt("pic_level"));
                    user.setPickaxeXp(rs.getDouble("pic_xp"));
                    user.loadSkillsFromString(rs.getString("pic_skills"));
                    user.setMissoesData(rs.getString("missoes_data"));
                    user.setDirty(false);
                    return user;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new TWUser(uuid, name, 0L, "CARVAO", "NENHUM");
    }

    public void saveUser(TWUser user) {
        String query = "INSERT INTO tw_users (uuid, name, tokens, rank_id, prestige_id, balance, blocos_quebrados, dinheiro_mina, pic_level, pic_xp, pic_skills, missoes_data) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE name=VALUES(name), tokens=VALUES(tokens), rank_id=VALUES(rank_id), prestige_id=VALUES(prestige_id), " +
                "balance=VALUES(balance), blocos_quebrados=VALUES(blocos_quebrados), dinheiro_mina=VALUES(dinheiro_mina), " +
                "pic_level=VALUES(pic_level), pic_xp=VALUES(pic_xp), pic_skills=VALUES(pic_skills), missoes_data=VALUES(missoes_data)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, user.getUuid().toString());
            ps.setString(2, user.getName());
            ps.setLong(3, user.getTokens());
            ps.setString(4, user.getRankId());
            ps.setString(5, user.getPrestigeId());
            ps.setDouble(6, user.getBalance());
            ps.setLong(7, user.getBlocosQuebrados());
            ps.setDouble(8, user.getDinheiroMina());
            ps.setInt(9, user.getPickaxeLevel());
            ps.setDouble(10, user.getPickaxeXp());
            ps.setString(11, user.getSkillsAsString());
            ps.setString(12, user.getMissoesData());
            ps.executeUpdate();
            user.setDirty(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Double> getTopBalances(int limit) {
        Map<String, Double> top = new LinkedHashMap<>();
        String query = "SELECT name, balance FROM tw_users ORDER BY balance DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(rs.getString("name"), rs.getDouble("balance"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }

    public boolean updateOfflineBalance(String playerName, double amount, String action) {
        String selectQuery = "SELECT uuid, balance FROM tw_users WHERE LOWER(name) = LOWER(?) LIMIT 1";
        double currentBalance;
        String uuid;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(selectQuery)) {
            ps.setString(1, playerName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    uuid = rs.getString("uuid");
                    currentBalance = rs.getDouble("balance");
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        double newBalance = currentBalance;
        switch (action.toLowerCase()) {
            case "give":
            case "add":
                newBalance += amount;
                break;
            case "take":
            case "remove":
                newBalance = Math.max(0, newBalance - amount);
                break;
            case "set":
                newBalance = amount;
                break;
        }

        String updateQuery = "UPDATE tw_users SET balance = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(updateQuery)) {
            ps.setDouble(1, newBalance);
            ps.setString(2, uuid);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Long> getTopMiners(int limit) {
        Map<String, Long> top = new LinkedHashMap<>();
        String query = "SELECT name, blocos_quebrados FROM tw_users ORDER BY blocos_quebrados DESC LIMIT ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    top.put(rs.getString("name"), rs.getLong("blocos_quebrados"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return top;
    }
}