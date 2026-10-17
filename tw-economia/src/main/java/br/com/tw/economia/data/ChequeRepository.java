package br.com.tw.economia.data;

import br.com.tw.core.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.logging.Level;

public final class ChequeRepository {

    private final DatabaseManager database;

    public ChequeRepository(DatabaseManager database) {
        this.database = database;
        createTable();
    }

    public OptionalDouble issue(String chequeId, UUID owner, double amount) {
        requireValidAmount(amount);
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                double newBalance;
                try (PreparedStatement debit = connection.prepareStatement(
                        "UPDATE tw_users SET balance = balance - ? WHERE uuid = ? AND balance >= ? RETURNING balance")) {
                    debit.setDouble(1, amount);
                    debit.setString(2, owner.toString());
                    debit.setDouble(3, amount);
                    try (ResultSet result = debit.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return OptionalDouble.empty();
                        }
                        newBalance = result.getDouble(1);
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO tw_cheques (cheque_id, owner_uuid, amount) VALUES (?, ?, ?)")) {
                    insert.setString(1, chequeId);
                    insert.setString(2, owner.toString());
                    insert.setDouble(3, amount);
                    insert.executeUpdate();
                }
                connection.commit();
                return OptionalDouble.of(newBalance);
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw failure("Falha ao emitir cheque " + chequeId + ".", e);
        }
    }

    public OptionalDouble redeem(String chequeId, UUID redeemer, double expectedAmount) {
        requireValidAmount(expectedAmount);
        try (Connection connection = database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                double amount;
                try (PreparedStatement redeem = connection.prepareStatement(
                        "UPDATE tw_cheques SET redeemed_by = ?, redeemed_at = CURRENT_TIMESTAMP "
                                + "WHERE cheque_id = ? AND redeemed_by IS NULL AND amount = ? RETURNING amount")) {
                    redeem.setString(1, redeemer.toString());
                    redeem.setString(2, chequeId);
                    redeem.setDouble(3, expectedAmount);
                    try (ResultSet result = redeem.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            return OptionalDouble.empty();
                        }
                        amount = result.getDouble(1);
                    }
                }

                double balance;
                try (PreparedStatement credit = connection.prepareStatement(
                        "UPDATE tw_users SET balance = balance + ? WHERE uuid = ? RETURNING balance")) {
                    credit.setDouble(1, amount);
                    credit.setString(2, redeemer.toString());
                    try (ResultSet result = credit.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Conta do jogador não encontrada ao resgatar cheque.");
                        }
                        balance = result.getDouble(1);
                    }
                }
                if (!Double.isFinite(balance)) {
                    throw new SQLException("O resgate do cheque ultrapassaria o limite numérico do saldo.");
                }
                connection.commit();
                return OptionalDouble.of(balance);
            } catch (SQLException | RuntimeException e) {
                rollback(connection, e);
                throw e;
            }
        } catch (SQLException e) {
            throw failure("Falha ao resgatar cheque " + chequeId + ".", e);
        }
    }

    private void createTable() {
        String sql = "CREATE TABLE IF NOT EXISTS tw_cheques ("
                + "cheque_id VARCHAR(36) PRIMARY KEY, "
                + "owner_uuid VARCHAR(36) NOT NULL, "
                + "amount DOUBLE PRECISION NOT NULL CHECK (amount > 0), "
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "redeemed_by VARCHAR(36), "
                + "redeemed_at TIMESTAMP)";
        try (Connection connection = database.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw failure("Falha ao criar a tabela de cheques.", e);
        }
        database.recordSchemaVersion("tw_economia", 1);
    }

    private void requireValidAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0.0) {
            throw new IllegalArgumentException("O valor do cheque deve ser finito e maior que zero.");
        }
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private IllegalStateException failure(String message, SQLException cause) {
        database.getLogger().log(Level.SEVERE, message, cause);
        return new IllegalStateException(message, cause);
    }
}
