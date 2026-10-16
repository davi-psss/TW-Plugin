package br.com.tw.core.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private HikariDataSource dataSource;
    private final JavaPlugin plugin;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        plugin.getLogger().info("Iniciando conexao com o banco de dados...");
        HikariConfig config = new HikariConfig();

        // Puxando os dados do config.yml de forma segura
        String host = plugin.getConfig().getString("mysql.host");
        int port = plugin.getConfig().getInt("mysql.port");
        String database = plugin.getConfig().getString("mysql.database");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true");
        config.setUsername(plugin.getConfig().getString("mysql.username"));
        config.setPassword(plugin.getConfig().getString("mysql.password"));

        // Otimizações do HikariCP recomendadas para servidores Minecraft
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setMaximumPoolSize(10); // 10 conexões são suficientes para a versão 1.8.8

        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Pool de conexoes estabelecida com sucesso!");
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
}