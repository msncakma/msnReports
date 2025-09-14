package dev.msntech.msnreports.database;

import dev.msntech.msnreports.BugReport;
import dev.msntech.msnreports.utils.EncryptionUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.io.File;

public class DatabaseManager {
    private final String databasePath;
    private final Plugin plugin;
    private final EncryptionUtil encryption;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        // Store database in plugin's data folder
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        this.databasePath = new File(plugin.getDataFolder(), "reports.db").getAbsolutePath();
        
        // Initialize encryption with a secure key
        try {
            String encryptionKey = plugin.getConfig().getString("encryption-key", "default-key-change-this");
            if (encryptionKey.equals("default-key-change-this")) {
                plugin.getLogger().warning("Using default encryption key! Please change it in config.yml for security!");
            }
            this.encryption = new EncryptionUtil(encryptionKey);
            plugin.getLogger().info("Encryption initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize encryption: " + e.getMessage());
            throw new RuntimeException("Failed to initialize encryption", e);
        }
        
        createTables();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    private void createTables() {
        try (Connection conn = getConnection()) {
            conn.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS bug_reports (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_name TEXT NOT NULL," +
                "player_uuid TEXT NOT NULL," +
                "description TEXT NOT NULL," +
                "world TEXT NOT NULL," +
                "x REAL NOT NULL," +
                "y REAL NOT NULL," +
                "z REAL NOT NULL," +
                "ip_address TEXT NOT NULL," +
                "game_mode TEXT NOT NULL," +
                "health REAL NOT NULL," +
                "level INTEGER NOT NULL," +
                "inventory TEXT NOT NULL," +
                "created_at TEXT DEFAULT CURRENT_TIMESTAMP," +
                "status TEXT DEFAULT 'OPEN'" +
                ")"
            );
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveBugReport(BugReport report) {
        String sql = "INSERT INTO bug_reports (player_name, player_uuid, description, world, x, y, z, " +
                    "ip_address, game_mode, health, level, inventory) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
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
            plugin.getLogger().info("Encrypted bug report saved to database successfully!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save bug report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String decrypt(String encryptedData) {
        return encryption.decrypt(encryptedData);
    }

    public void close() {
        // SQLite doesn't need explicit cleanup
    }
}