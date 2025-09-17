package dev.msntech.msnreports.database;

import dev.msntech.msnreports.BugReport;
import dev.msntech.msnreports.utils.EncryptionUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.io.File;

public class DatabaseManager {
    private final String databasePath;
    private final Plugin plugin;
    private final EncryptionUtil encryption;
    private final HikariDataSource dataSource;
    private final String databaseType;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        FileConfiguration config = plugin.getConfig();
        this.databaseType = config.getString("database.type", "sqlite").toLowerCase();
        
        // Initialize encryption with a secure key
        try {
            String encryptionKey = config.getString("encryption-key", "default-key-change-this");
            if (encryptionKey.equals("default-key-change-this")) {
                plugin.getLogger().warning("Using default encryption key! Please change it in config.yml for security!");
            }
            this.encryption = new EncryptionUtil(encryptionKey);
            plugin.getLogger().info("Encryption initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize encryption: " + e.getMessage());
            throw new RuntimeException("Failed to initialize encryption", e);
        }
        
        // Initialize database connection based on type
        if ("mysql".equals(databaseType)) {
            this.databasePath = null; // Not used for MySQL
            this.dataSource = initializeMySQLConnection(config);
            plugin.getLogger().info("Using MySQL database");
        } else {
            // Default to SQLite
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            this.databasePath = new File(plugin.getDataFolder(), "reports.db").getAbsolutePath();
            this.dataSource = initializeSQLiteConnection();
            plugin.getLogger().info("Using SQLite database");
        }
        
        createTables();
        migrateDatabase();
    }
    
    private HikariDataSource initializeSQLiteConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + databasePath);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        
        return new HikariDataSource(config);
    }
    
    private HikariDataSource initializeMySQLConnection(FileConfiguration config) {
        HikariConfig hikariConfig = new HikariConfig();
        
        String host = config.getString("database.mysql.host", "localhost");
        int port = config.getInt("database.mysql.port", 3306);
        String database = config.getString("database.mysql.database", "msnreports");
        String username = config.getString("database.mysql.username", "root");
        String password = config.getString("database.mysql.password", "");
        
        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
                                      host, port, database);
        
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // Connection pool settings
        hikariConfig.setMaximumPoolSize(config.getInt("database.mysql.pool.maximum-pool-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("database.mysql.pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("database.mysql.pool.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("database.mysql.pool.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("database.mysql.pool.max-lifetime", 1800000));
        hikariConfig.setLeakDetectionThreshold(config.getLong("database.mysql.pool.leak-detection-threshold", 60000));
        
        try {
            return new HikariDataSource(hikariConfig);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize MySQL connection: " + e.getMessage());
            plugin.getLogger().severe("Please check your MySQL configuration in config.yml");
            throw new RuntimeException("Failed to initialize MySQL connection", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed!");
        }
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            String createTableSQL;
            
            if ("mysql".equals(databaseType)) {
                createTableSQL = 
                    "CREATE TABLE IF NOT EXISTS bug_reports (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "player_name TEXT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "description TEXT NOT NULL," +
                    "world TEXT NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "ip_address TEXT," +
                    "game_mode TEXT," +
                    "health DOUBLE," +
                    "level INT," +
                    "inventory LONGTEXT," +
                    "status VARCHAR(50) DEFAULT 'OPEN'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                    "handler TEXT," +
                    "comments LONGTEXT" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";
            } else {
                // SQLite (default)
                createTableSQL = 
                    "CREATE TABLE IF NOT EXISTS bug_reports (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "player_name TEXT NOT NULL," +
                    "player_uuid TEXT NOT NULL," +
                    "description TEXT NOT NULL," +
                    "world TEXT NOT NULL," +
                    "x DOUBLE NOT NULL," +
                    "y DOUBLE NOT NULL," +
                    "z DOUBLE NOT NULL," +
                    "ip_address TEXT," +
                    "game_mode TEXT," +
                    "health DOUBLE," +
                    "level INTEGER," +
                    "inventory TEXT," +
                    "status TEXT DEFAULT 'OPEN'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "handler TEXT," +
                    "comments TEXT" +
                    ")";
            }
            
            conn.createStatement().execute(createTableSQL);
            plugin.getLogger().info("Database tables created/verified successfully using " + databaseType.toUpperCase());
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public int saveBugReport(BugReport report) {
        String sql = "INSERT INTO bug_reports (player_name, player_uuid, description, world, x, y, z, " +
                    "ip_address, game_mode, health, level, inventory) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            
            String[] locationParts = report.getLocation().split(", ");
            String world = locationParts[0].replace("World: ", "");
            double x = Double.parseDouble(locationParts[1].replace("X: ", ""));
            double y = Double.parseDouble(locationParts[2].replace("Y: ", ""));
            double z = Double.parseDouble(locationParts[3].replace("Z: ", ""));
            
            // Encrypt sensitive data
            stmt.setString(1, encryption.encrypt(report.getPlayerName()));
            stmt.setString(2, encryption.encrypt(report.getPlayerUUID()));
            stmt.setString(3, encryption.encrypt(report.getDescription()));
            stmt.setString(4, encryption.encrypt(world));
            stmt.setDouble(5, x);
            stmt.setDouble(6, y);
            stmt.setDouble(7, z);
            stmt.setString(8, encryption.encrypt(report.getIpAddress()));
            stmt.setString(9, report.getGameMode());
            stmt.setDouble(10, report.getHealth());
            stmt.setInt(11, report.getLevel());
            stmt.setString(12, encryption.encrypt(report.getInventory()));
            
            stmt.executeUpdate();
            
            // Get the generated report ID
            try (java.sql.ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int reportId = rs.getInt(1);
                    // Bug report saved successfully - reduced logging for cleaner console
                    return reportId;
                }
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save bug report: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1; // Return -1 if failed
    }

    public String encrypt(String data) {
        try {
            return encryption.encrypt(data);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to encrypt data: " + e.getMessage());
            return data; // Return unencrypted data as fallback
        }
    }

    public String decrypt(String encryptedData) {
        try {
            return encryption.decrypt(encryptedData);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to decrypt data: " + e.getMessage());
            return encryptedData; // Return encrypted data as fallback
        }
    }

    private void migrateDatabase() {
        try (Connection conn = getConnection()) {
            // Check if old schema exists and migrate if needed
            try {
                // Check if 'player' column exists (old schema)
                conn.createStatement().execute("SELECT player FROM bug_reports LIMIT 1");
                
                // If we get here, old schema exists, need to migrate
                plugin.getLogger().info("Migrating database from old schema...");
                
                // Add missing columns if they don't exist
                addColumnIfNotExists(conn, "player_name", "TEXT");
                addColumnIfNotExists(conn, "player_uuid", "TEXT");
                addColumnIfNotExists(conn, "ip_address", "TEXT");
                addColumnIfNotExists(conn, "game_mode", "TEXT");
                addColumnIfNotExists(conn, "health", "DOUBLE");
                
                if ("mysql".equals(databaseType)) {
                    addColumnIfNotExists(conn, "level", "INT");
                } else {
                    addColumnIfNotExists(conn, "level", "INTEGER");
                }
                
                // Copy data from old 'player' column to new 'player_name' column if player_name is empty
                conn.createStatement().execute(
                    "UPDATE bug_reports SET player_name = player WHERE player_name IS NULL OR player_name = ''"
                );
                
                plugin.getLogger().info("Database migration completed successfully!");
                
            } catch (SQLException e) {
                // Old schema doesn't exist, new schema is already in place
                plugin.getLogger().info("Database schema is up to date");
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to migrate database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void addColumnIfNotExists(Connection conn, String columnName, String columnType) {
        try {
            if ("mysql".equals(databaseType)) {
                // MySQL syntax for adding columns
                conn.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN " + columnName + " " + columnType);
            } else {
                // SQLite syntax for adding columns
                conn.createStatement().execute("ALTER TABLE bug_reports ADD COLUMN " + columnName + " " + columnType);
            }
            plugin.getLogger().info("Added column: " + columnName);
        } catch (SQLException e) {
            // Column already exists, ignore the error
        }
    }
}