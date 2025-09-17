package dev.msntech.msnreports;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import dev.msntech.msnreports.commands.ManageReportsCommand;
import dev.msntech.msnreports.listeners.AdminLoginListener;
import dev.msntech.msnreports.utils.ChatUtils;
import dev.msntech.msnreports.utils.UpdateChecker;
import dev.msntech.msnreports.database.DatabaseManager;
import dev.msntech.msnreports.managers.ReportManager;
import net.kyori.adventure.text.Component;
import java.util.Objects;

public class App extends JavaPlugin {
    private FileConfiguration config;
    private String reportsWebhookUrl;
    private String adminChangesWebhookUrl;
    private String adminNotesWebhookUrl;
    private String statusChangesWebhookUrl;
    private DiscordWebhookSender webhookSender;
    private ReportBugCommand reportBugCommand;
    private ReportCommand reportCommand;
    private DatabaseManager databaseManager;
    private ManageReportsCommand manageReportsCommand;
    private ReportManager reportManager;
    private UpdateChecker updateChecker;

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
        
        // Load configuration with new structure
        reportsWebhookUrl = config.getString("discord.webhooks.reports.url", "");
        adminChangesWebhookUrl = config.getString("discord.webhooks.admin-changes.url", "");
        adminNotesWebhookUrl = config.getString("discord.webhooks.admin-notes.url", "");
        statusChangesWebhookUrl = config.getString("discord.webhooks.status-changes.url", "");
        
        boolean discordEnabled = config.getBoolean("discord.enabled", true);
        if (!discordEnabled) {
            getLogger().info("Discord webhooks are disabled in config.yml");
        } else if (reportsWebhookUrl.isEmpty() || reportsWebhookUrl.equals("YOUR_REPORTS_WEBHOOK_URL_HERE")) {
            getLogger().warning("Discord webhook URLs not configured! Please set them in config.yml");
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
        reportBugCommand = new ReportBugCommand(this);
        reportCommand = new ReportCommand(this);
        manageReportsCommand = new ManageReportsCommand(this);
        
        getCommand("reportbug").setExecutor(reportBugCommand);
        getCommand("report").setExecutor(reportCommand);
        getCommand("report").setTabCompleter(reportCommand);
        getCommand("managereports").setExecutor(manageReportsCommand);
        getCommand("managereports").setTabCompleter(manageReportsCommand);
        
        getServer().getPluginManager().registerEvents(reportBugCommand.getReportListener(), this);
        getServer().getPluginManager().registerEvents(reportCommand.getReportListener(), this);
        getServer().getPluginManager().registerEvents(new AdminLoginListener(this), this);
        
        // Start rate limiter cleanup task (every 10 minutes)
        getServer().getGlobalRegionScheduler().runAtFixedRate(this, (task) -> {
            dev.msntech.msnreports.utils.RateLimiter.cleanup();
        }, 20L * 60 * 10, 20L * 60 * 10); // 10 minutes in ticks
        
        // Log startup
        getLogger().info(ChatUtils.getPrefix().append(Component.text("Plugin enabled successfully!")).toString());
        
        // Initialize update checker and check for updates if enabled
        if (isUpdateNotificationsEnabled()) {
            updateChecker = new UpdateChecker(this);
            if (shouldCheckUpdatesOnStartup()) {
                updateChecker.checkForUpdates().thenAccept(updateAvailable -> {
                    if (updateAvailable) {
                        getServer().getGlobalRegionScheduler().runDelayed(this, (task) -> {
                            updateChecker.notifyAdminsIfUpdateAvailable();
                        }, 60L); // Wait 3 seconds before notifying
                    }
                });
            }
        }
        
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

    public DiscordWebhookSender getWebhookSender() {
        return webhookSender;
    }

    public String getReportsWebhookUrl() {
        return reportsWebhookUrl;
    }

    public String getAdminChangesWebhookUrl() {
        return adminChangesWebhookUrl;
    }

    public String getAdminNotesWebhookUrl() {
        return adminNotesWebhookUrl;
    }

    public String getStatusChangesWebhookUrl() {
        return statusChangesWebhookUrl;
    }

    @Override
    public void reloadConfig() {
        // Reload the config file from disk
        super.reloadConfig();
        config = getConfig();
        
        try {
            // Check license again
            String license = config.getString("license", "");
            if (!license.equals("github.com/msncakma")) {
                getLogger().severe("Invalid license in config.yml after reload!");
                return;
            }
            
            // Store old webhook URLs for comparison (only if they exist)
            String oldReportsUrl = this.reportsWebhookUrl;
            String oldAdminChangesUrl = this.adminChangesWebhookUrl;
            String oldAdminNotesUrl = this.adminNotesWebhookUrl;
            String oldStatusChangesUrl = this.statusChangesWebhookUrl;
            
            // Reload webhook URLs with new structure
            reportsWebhookUrl = config.getString("discord.webhooks.reports.url", "");
            adminChangesWebhookUrl = config.getString("discord.webhooks.admin-changes.url", "");
            adminNotesWebhookUrl = config.getString("discord.webhooks.admin-notes.url", "");
            statusChangesWebhookUrl = config.getString("discord.webhooks.status-changes.url", "");
            
            // Log webhook URL changes (only if this is not the first initialization)
            if (oldReportsUrl != null) {
                getLogger().info("Reloading webhook configurations...");
                getLogger().info("Webhook enabled states: reports=" + isReportsWebhookEnabled() + 
                               ", admin-changes=" + isAdminChangesWebhookEnabled() + 
                               ", admin-notes=" + isAdminNotesWebhookEnabled() + 
                               ", status-changes=" + isStatusChangesWebhookEnabled());
                
                // Log URL changes with better comparison
                boolean urlsChanged = false;
                if (!Objects.equals(oldReportsUrl, reportsWebhookUrl)) {
                    getLogger().info("Reports webhook URL changed from '" + (oldReportsUrl == null ? "null" : oldReportsUrl) + 
                                   "' to '" + reportsWebhookUrl + "'");
                    urlsChanged = true;
                }
                if (!Objects.equals(oldAdminChangesUrl, adminChangesWebhookUrl)) {
                    getLogger().info("Admin changes webhook URL changed from '" + (oldAdminChangesUrl == null ? "null" : oldAdminChangesUrl) + 
                                   "' to '" + adminChangesWebhookUrl + "'");
                    urlsChanged = true;
                }
                if (!Objects.equals(oldAdminNotesUrl, adminNotesWebhookUrl)) {
                    getLogger().info("Admin notes webhook URL changed from '" + (oldAdminNotesUrl == null ? "null" : oldAdminNotesUrl) + 
                                   "' to '" + adminNotesWebhookUrl + "'");
                    urlsChanged = true;
                }
                if (!Objects.equals(oldStatusChangesUrl, statusChangesWebhookUrl)) {
                    getLogger().info("Status changes webhook URL changed from '" + (oldStatusChangesUrl == null ? "null" : oldStatusChangesUrl) + 
                                   "' to '" + statusChangesWebhookUrl + "'");
                    urlsChanged = true;
                }
                
                if (urlsChanged) {
                    getLogger().info("Webhook URLs changed, reinitializing webhook clients...");
                } else {
                    getLogger().info("No webhook URL changes detected, but reinitializing anyway for configuration reload...");
                }
                
                // Always close and reinitialize webhook sender on reload
                if (webhookSender != null) {
                    webhookSender.close();
                    getLogger().info("Closed old webhook clients");
                }
                webhookSender = new DiscordWebhookSender(this);
                getLogger().info("Initialized new webhook clients");
                
                // Close and reinitialize database manager 
                if (databaseManager != null) {
                    databaseManager.close();
                }
                databaseManager = new DatabaseManager(this);
                
                // Reinitialize report manager
                reportManager = new ReportManager(this);
                
                getLogger().info("Configuration reloaded successfully! All components reinitialized.");
            } else {
                // First-time initialization, just log that config was loaded
                getLogger().info("Configuration loaded successfully.");
            }
            
        } catch (Exception e) {
            getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled", true);
    }

    public boolean isAdminNotificationsEnabled() {
        return config.getBoolean("admin-notifications.enabled", true);
    }

    public boolean isLoginNotificationsEnabled() {
        return config.getBoolean("admin-notifications.on-login", true);
    }
    
    public boolean isReportsWebhookEnabled() {
        return config.getBoolean("discord.webhooks.reports.enabled", true);
    }
    
    public boolean isAdminChangesWebhookEnabled() {
        return config.getBoolean("discord.webhooks.admin-changes.enabled", true);
    }
    
    public boolean isAdminNotesWebhookEnabled() {
        return config.getBoolean("discord.webhooks.admin-notes.enabled", true);
    }
    
    public boolean isStatusChangesWebhookEnabled() {
        return config.getBoolean("discord.webhooks.status-changes.enabled", true);
    }
    
    public boolean isUpdateNotificationsEnabled() {
        return config.getBoolean("update-notifications.enabled", true);
    }
    
    public boolean shouldCheckUpdatesOnStartup() {
        return config.getBoolean("update-notifications.check-on-startup", true);
    }
    
    public boolean shouldNotifyAdminsOfUpdates() {
        return config.getBoolean("update-notifications.notify-admins", true);
    }
}
