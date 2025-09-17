package dev.msntech.msnreports;

import dev.msntech.msnreports.models.ReportStatus;
import dev.msntech.msnreports.utils.ChatUtils;
import dev.msntech.msnreports.utils.RateLimiter;
import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.awt.Color;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DiscordWebhookSender {
    private final WebhookClient reportsClient;
    private final WebhookClient adminChangesClient;
    private final WebhookClient adminNotesClient;
    private final WebhookClient statusChangesClient;
    private final App plugin;

    public DiscordWebhookSender(App plugin) {
        this.plugin = plugin;
        
        // Initialize webhook clients based on configuration
        String reportsUrl = plugin.getReportsWebhookUrl();
        String adminChangesUrl = plugin.getAdminChangesWebhookUrl();
        String adminNotesUrl = plugin.getAdminNotesWebhookUrl();
        String statusChangesUrl = plugin.getStatusChangesWebhookUrl();
        
        this.reportsClient = initializeClient(reportsUrl, "Reports");
        this.adminChangesClient = initializeClient(adminChangesUrl, "Admin Changes");
        this.adminNotesClient = initializeClient(adminNotesUrl, "Admin Notes");
        this.statusChangesClient = initializeClient(statusChangesUrl, "Status Changes");
    }

    private WebhookClient initializeClient(String webhookUrl, String clientName) {
        // Validate webhook URL with less verbose logging
        if (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.isBlank()) {
            return null;
        }
        
        if (webhookUrl.startsWith("YOUR_")) {
            plugin.getLogger().warning(clientName + " webhook URL is placeholder - please update in config.yml");
            return null;
        }
        
        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/")) {
            plugin.getLogger().warning(clientName + " webhook URL is invalid - must be a Discord webhook URL");
            return null;
        }
        
        // Create the client
        try {
            WebhookClient client = new WebhookClientBuilder(webhookUrl).build();
            return client;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize " + clientName + " webhook client: " + e.getMessage());
            return null;
        }
    }
    
    private String stripMinecraftColors(String text) {
        if (text == null) return null;
        // Remove §-based color codes (§a, §1, etc.)
        text = text.replaceAll("§[0-9a-fk-or]", "");
        // Remove &-based color codes (&a, &1, etc.)
        text = text.replaceAll("&[0-9a-fk-or]", "");
        return text;
    }

    public void sendBugReport(BugReport report, int reportId, Player player) {
        if (!plugin.isDiscordEnabled()) {
            plugin.getLogger().info("Discord is disabled in config.yml");
            // Still notify the player that the report was saved
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been saved! ")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.AQUA)));
            return;
        }
        
        if (!plugin.isReportsWebhookEnabled()) {
            plugin.getLogger().info("Reports webhook is disabled in config.yml");
            // Still notify the player that the report was saved
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been saved! ")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.AQUA)));
            return;
        }
        
        if (reportsClient == null) {
            plugin.getLogger().warning("Reports webhook client is null! URL: '" + plugin.getReportsWebhookUrl() + 
                                      "', Enabled: " + plugin.isReportsWebhookEnabled());
            plugin.getLogger().warning("Please set a valid Discord webhook URL in config.yml");
            // Still notify the player that the report was saved
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been saved! ")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text("Report ID: #" + reportId + " (Discord notification failed)")
                            .color(NamedTextColor.YELLOW)));
            return;
        }
        
        // Check rate limiting for Discord webhooks
        if (!RateLimiter.canSendDiscordWebhook()) {
            plugin.getLogger().warning("Discord webhook rate limited, delaying send...");
            // Still notify the player that the report was saved
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been saved! Discord notification will be sent shortly. ")
                            .color(NamedTextColor.YELLOW))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.AQUA)));
            return;
        }
        
        // Record the webhook send
        RateLimiter.recordDiscordWebhook();
        
        Color embedColor = new Color(220, 20, 60); // Crimson red for bug reports
        
        // Strip color codes from text fields
        String cleanDescription = stripMinecraftColors(report.getDescription());
        String cleanPlayerName = stripMinecraftColors(report.getPlayerName());
        String cleanLocation = stripMinecraftColors(formatLocation(report.getLocation()));
        String cleanGameMode = stripMinecraftColors(report.getGameMode());
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "🐛 Bug Report #" + reportId, 
                    null))
                .setDescription("**📝 Description:**\n" + cleanDescription)
                .addField(new WebhookEmbed.EmbedField(true, "👤 Reporter", cleanPlayerName))
                .addField(new WebhookEmbed.EmbedField(true, "📊 Status", "Open"))
                .addField(new WebhookEmbed.EmbedField(true, "⚡ Priority", "Normal"))
                .addField(new WebhookEmbed.EmbedField(true, "❓ Game Mode", cleanGameMode))
                .addField(new WebhookEmbed.EmbedField(true, "❤ Health", String.format("%.1f/20", report.getHealth())))
                .addField(new WebhookEmbed.EmbedField(true, "⭐ Level", String.valueOf(report.getLevel())))
                .addField(new WebhookEmbed.EmbedField(false, "📍 Location", cleanLocation))
                .addField(new WebhookEmbed.EmbedField(false, "⏰ Reported", report.getTimestamp()))
                .setColor(embedColor.getRGB())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "msnReports-v" + plugin.getDescription().getVersion() + " • ID: " + reportId, 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("MSN Reports")
                .addEmbeds(embed)
                .build();

        reportsClient.send(message).thenAccept(sentMessage -> {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been sent to Discord successfully! ")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.YELLOW)));
        }).exceptionally(error -> {
            plugin.getLogger().severe("Failed to send bug report to Discord: " + error.getMessage());
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Warning: Failed to send notification to Discord, but your report was saved! ")
                            .color(NamedTextColor.YELLOW))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.AQUA)));
            return null;
        });
    }

    public void sendStatusUpdate(int reportId, ReportStatus oldStatus, ReportStatus newStatus, String handlerName) {
        if (!plugin.isDiscordEnabled() || !plugin.isStatusChangesWebhookEnabled() || statusChangesClient == null) {
            plugin.getLogger().info("Status changes webhook is disabled in config.yml");
            return;
        }
        
        Color embedColor = getStatusColor(newStatus);
        String statusEmoji = getStatusEmoji(newStatus);
        String cleanHandlerName = stripMinecraftColors(handlerName);
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    statusEmoji + " Report #" + reportId + " Status Updated", 
                    null))
                .setDescription("Status changed from **" + oldStatus.getDisplay() + "** to **" + newStatus.getDisplay() + "**")
                .addField(new WebhookEmbed.EmbedField(true, "Handler", cleanHandlerName))
                .addField(new WebhookEmbed.EmbedField(true, "New Status", newStatus.getDisplay()))
                .setColor(embedColor.getRGB())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "msnReports-v" + plugin.getDescription().getVersion() + " • ID: " + reportId, 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("MSN Reports")
                .addEmbeds(embed)
                .build();

        statusChangesClient.send(message).thenAccept(sentMessage -> {
            // Success - reduced logging for cleaner console
        }).exceptionally(error -> {
            plugin.getLogger().severe("Failed to send status update to Discord: " + error.getMessage());
            return null;
        });
    }

    public void sendCommentNotification(int reportId, String commenterName, String comment) {
        String cleanComment = stripMinecraftColors(comment);
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "💬 Comment on Report #" + reportId, 
                    null))
                .setDescription("**" + commenterName + "** added a comment:\n\n" + truncateText(cleanComment, 300))
                .setColor(new Color(52, 152, 219).getRGB()) // Blue color
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "msnReports-v" + plugin.getDescription().getVersion() + " • ID: " + reportId, 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("MSN Reports")
                .addEmbeds(embed)
                .build();

        adminNotesClient.send(message).thenAccept(sentMessage -> 
            plugin.getLogger().info("Comment notification for report #" + reportId + " sent to Discord!")
        ).exceptionally(error -> {
            plugin.getLogger().severe("Failed to send comment notification to Discord: " + error.getMessage());
            return null;
        });
    }

    private Color getStatusColor(ReportStatus status) {
        return switch (status) {
            case OPEN -> new Color(0, 123, 255);         // Blue
            case IN_PROGRESS -> new Color(255, 193, 7); // Yellow
            case RESOLVED -> new Color(40, 167, 69);    // Green
            case REJECTED -> new Color(108, 117, 125);    // Gray
        };
    }

    private String getStatusEmoji(ReportStatus status) {
        return switch (status) {
            case OPEN -> "🆕";
            case IN_PROGRESS -> "🔄";
            case RESOLVED -> "✅";
            case REJECTED -> "❌";
        };
    }

    private String truncateText(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String truncateInventory(String inventory) {
        if (inventory.length() <= 1000) {
            return inventory;
        }
        return inventory.substring(0, 997) + "...";
    }

    private String formatLocation(String location) {
        return location.replace("World: ", "🌍 **World:** ")
                      .replace(", X:", "\n📐 **X:** ")
                      .replace(", Y:", "\n🔺 **Y:** ")
                      .replace(", Z:", "\n📍 **Z:** ");
    }

    private String getExperienceDescription(int level) {
        if (level < 10) return "Beginner";
        if (level < 30) return "Experienced";
        if (level < 50) return "Advanced";
        if (level < 100) return "Expert";
        return "Master";
    }
    
    public void sendReportDeletion(int reportId, Map<String, Object> report, org.bukkit.entity.Player deletedBy) {
        if (!plugin.isDiscordEnabled() || !plugin.isAdminChangesWebhookEnabled() || adminChangesClient == null) {
            return;
        }
        
        Color embedColor = new Color(220, 53, 69); // Bootstrap danger red
        
        // Strip color codes from text fields
        String cleanDescription = stripMinecraftColors(report.get("description").toString());
        String cleanPlayerName = stripMinecraftColors(report.get("playerName").toString());
        String cleanDeletedBy = stripMinecraftColors(deletedBy.getName());
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "🗑️ Report #" + reportId + " Deleted", 
                    null))
                .setDescription("Report permanently deleted by **" + cleanDeletedBy + "**")
                .addField(new WebhookEmbed.EmbedField(true, "�‍💼 **Action Performed By**", 
                    "**Admin:** `" + cleanDeletedBy + "`\n" +
                    "**UUID:** `" + deletedBy.getUniqueId().toString().substring(0, 8) + "...`\n" +
                    "**Timestamp:** <t:" + (System.currentTimeMillis() / 1000) + ":F>"))
                .addField(new WebhookEmbed.EmbedField(true, "Original Reporter", cleanPlayerName))
                .addField(new WebhookEmbed.EmbedField(false, "Original Description", 
                    truncateText(cleanDescription, 200)))

                .setColor(embedColor.getRGB())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "msnReports-v" + plugin.getDescription().getVersion() + " • ID: " + reportId, 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("MSN Reports")
                .addEmbeds(embed)
                .build();

        try {
            adminChangesClient.send(message).whenComplete((result, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Failed to send report deletion notification to Discord: " + throwable.getMessage());
                }
            });
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending report deletion notification: " + e.getMessage());
        }
    }

    public void close() {
        if (reportsClient != null) {
            reportsClient.close();
        }
        if (adminChangesClient != null) {
            adminChangesClient.close();
        }
        if (adminNotesClient != null) {
            adminNotesClient.close();
        }
        if (statusChangesClient != null) {
            statusChangesClient.close();
        }
    }
}