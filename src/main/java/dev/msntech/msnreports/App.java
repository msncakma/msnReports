package dev.msntech.msnreports;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import dev.msntech.msnreports.commands.ManageReportsCommand;
import dev.msntech.msnreports.utils.ChatUtils;
import dev.msntech.msnreports.database.DatabaseManager;
import dev.msntech.msnreports.managers.ReportManager;
import net.kyori.adventure.text.Component;

public class App extends JavaPlugin {
    private FileConfiguration config;
    private String webhookUrl;
    private DiscordWebhookSender webhookSender;
    private ReportBugCommand reportCommand;
    private DatabaseManager databaseManager;
    private ManageReportsCommand manageReportsCommand;
    private ReportManager reportManager;

    @Override
    public void onEnable() {
        try {
            // Save default config
            saveDefaultConfig();
            config = getConfig();
            
            // Check license
            String license = config.getString("license", "");
            if (!license.equals("github.com/msncakma")) {
                getLogger().severe("Invalid license in config.yml! Plugin will be disabled.");
                getLogger().severe("Please set license: 'github.com/msncakma' in your config.yml");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
        } catch (Exception e) {
            getLogger().severe("Failed to load config.yml! Plugin will be disabled.");
            getLogger().severe("Error: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Load configuration
        webhookUrl = config.getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE")) {
            getLogger().warning("Discord webhook URL not configured! Please set it in config.yml");
        }
        
        // Initialize webhook sender
        webhookSender = new DiscordWebhookSender(this);
        
        // Initialize SQLite database and report manager
        try {
            databaseManager = new DatabaseManager(this);
            reportManager = new ReportManager(this);
            getLogger().info("SQLite database and report manager initialized successfully!");
        } catch (Exception e) {
            getLogger().severe("Failed to initialize SQLite database!");
            getLogger().severe("Error: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize and register commands
        reportCommand = new ReportBugCommand(this);
        manageReportsCommand = new ManageReportsCommand(this);
        
        getCommand("reportbug").setExecutor(reportCommand);
        getCommand("managereports").setExecutor(manageReportsCommand);
        getCommand("managereports").setTabCompleter(manageReportsCommand);
        
        getServer().getPluginManager().registerEvents(reportCommand.getReportListener(), this);
        
        // Log startup
        getLogger().info(ChatUtils.getPrefix().append(Component.text("Plugin enabled successfully!")).toString());
        
        getLogger().info("MSNReports has been enabled!");
    }

    @Override
    public void onDisable() {
        if (webhookSender != null) {
            webhookSender.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("MSNReports has been disabled!");
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }
}
