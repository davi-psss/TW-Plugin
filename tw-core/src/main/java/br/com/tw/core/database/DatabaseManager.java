package br.com.tw.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        plugin.getLogger().info("Iniciando conexao com o banco de dados...");
        HikariConfig config = new HikariConfig();

        String host = getSetting("TW_DB_HOST", "postgresql.host", "localhost");
        int port;
        try {
            port = Integer.parseInt(getSetting("TW_DB_PORT", "postgresql.port", "5432"));
        } catch (NumberFormatException e) {
            throw new IllegalStateException("postgresql.port/TW_DB_PORT deve ser um número entre 1 e 65535.", e);
        }
        if (port < 1 || port > 65535) {
            throw new IllegalStateException("postgresql.port/TW_DB_PORT deve ser um número entre 1 e 65535.");
        }
        String database = getSetting("TW_DB_DATABASE", "postgresql.database", "tw_network_db");
        String username = getSetting("TW_DB_USERNAME", "postgresql.username", "");
        String password = getSetting("TW_DB_PASSWORD", "postgresql.password", "");
        if (username.trim().isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Configure postgresql.username/password ou TW_DB_USERNAME/TW_DB_PASSWORD.");
        }
        if (host.trim().isEmpty() || database.trim().isEmpty()) {
            throw new IllegalStateException("postgresql.host e postgresql.database não podem estar vazios.");
        }

        config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);

        try {
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Pool de conexoes estabelecida com sucesso!");
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao conectar ao PostgreSQL.", e);
            throw e;
        }
    }

    private String getSetting(String environmentVariable, String configPath, String fallback) {
        String environmentValue = System.getenv(environmentVariable);
        if (environmentValue != null && !environmentValue.trim().isEmpty()) {
            return environmentValue;
        }
        return plugin.getConfig().getString(configPath, fallback);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("O HikariCP não está inicializado!");
        }
        return dataSource.getConnection();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Conexao com o banco encerrada com seguranca.");
        }
    }

    public java.util.logging.Logger getLogger() {
        return plugin.getLogger();
    }

    public void recordSchemaVersion(String module, int version) {
        if (module == null || module.trim().isEmpty() || version < 1) {
            throw new IllegalArgumentException("Módulo e versão de schema são obrigatórios.");
        }
        String create = "CREATE TABLE IF NOT EXISTS tw_schema_version ("
                + "module VARCHAR(64) PRIMARY KEY, version INT NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        String upsert = "INSERT INTO tw_schema_version (module, version) VALUES (?, ?) "
                + "ON CONFLICT (module) DO UPDATE SET version = GREATEST(tw_schema_version.version, EXCLUDED.version), "
                + "updated_at = CURRENT_TIMESTAMP";
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(create);
            try (PreparedStatement prepared = connection.prepareStatement(upsert)) {
                prepared.setString(1, module);
                prepared.setInt(2, version);
                prepared.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Falha ao registrar versão do schema " + module + ".", e);
            throw new IllegalStateException("Falha ao registrar versão do schema " + module + ".", e);
        }
    }
}
