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

public class DiscordWebhookSender {
    private final WebhookClient reportsClient;
    private final WebhookClient adminChangesClient;
    private final WebhookClient adminNotesClient;
    private final WebhookClient statusChangesClient;
    private final App plugin;

    public DiscordWebhookSender(App plugin) {
        this.plugin = plugin;
        
        // Initialize webhook clients based on configuration
        this.reportsClient = initializeClient(plugin.getReportsWebhookUrl());
        this.adminChangesClient = initializeClient(plugin.getAdminChangesWebhookUrl());
        this.adminNotesClient = initializeClient(plugin.getAdminNotesWebhookUrl());
        this.statusChangesClient = initializeClient(plugin.getStatusChangesWebhookUrl());
    }

    private WebhookClient initializeClient(String webhookUrl) {
        if (webhookUrl != null && !webhookUrl.isEmpty() && !webhookUrl.startsWith("YOUR_")) {
            return new WebhookClientBuilder(webhookUrl).build();
        }
        return null;
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
        if (!plugin.isDiscordEnabled() || !plugin.isReportsWebhookEnabled() || reportsClient == null) {
            // Still notify the player that the report was saved
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Your bug report has been saved! ")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text("Report ID: #" + reportId)
                            .color(NamedTextColor.AQUA)));
            
            if (!plugin.isReportsWebhookEnabled()) {
                plugin.getLogger().info("Reports webhook is disabled in config.yml");
            }
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
        
        Color embedColor = new Color(255, 102, 102); // Red color for bug reports
        String categoryEmoji = "🐛"; // Bug emoji
        
        // Strip color codes from text fields
        String cleanDescription = stripMinecraftColors(report.getDescription());
        String cleanPlayerName = stripMinecraftColors(report.getPlayerName());
        String cleanGameMode = stripMinecraftColors(report.getGameMode());
        String cleanLocation = stripMinecraftColors(formatLocation(report.getLocation()));
        String cleanInventory = stripMinecraftColors(truncateInventory(report.getInventory()));
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "🐛 Bug Report #" + reportId, 
                    null))
                .setDescription("```\n" + cleanDescription + "\n```")
                .addField(new WebhookEmbed.EmbedField(true, "📊 Status", 
                    "🆕 **OPEN**"))
                .addField(new WebhookEmbed.EmbedField(true, "⏰ Priority", 
                    "🔔 **Normal**"))
                .addField(new WebhookEmbed.EmbedField(true, "👤 Reporter Information", 
                    "**Player:** " + cleanPlayerName + "\n" +
                    "**UUID:** `" + report.getPlayerUUID() + "`\n" +
                    "**GameMode:** " + cleanGameMode))
                .addField(new WebhookEmbed.EmbedField(true, "❤️ Player Status", 
                    "**Health:** " + String.format("%.1f", report.getHealth()) + " ❤️\n" +
                    "**Level:** " + report.getLevel() + " ⭐\n" +
                    "**Experience:** " + getExperienceDescription(report.getLevel())))
                .addField(new WebhookEmbed.EmbedField(true, "🗺️ Location Details", 
                    cleanLocation))
                .addField(new WebhookEmbed.EmbedField(false, "🎒 Player Inventory", 
                    "```\n" + cleanInventory + "\n```"))
                .setColor(embedColor.getRGB())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "Report ID: " + reportId + " • msnReports-v" + plugin.getDescription().getVersion(), 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("🔧 MSN Reports")
                .setAvatarUrl("https://i.imgur.com/QfTn5hP.png") // Bug icon
                .addEmbeds(embed)
                .setContent("## 🚨 New Bug Report Alert!\n*A player has submitted a new bug report that needs attention.*")
                .build();

        reportsClient.send(message).thenAccept(sentMessage -> {
            plugin.getLogger().info("Bug report #" + reportId + " sent to Discord successfully!");
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
        String oldStatusEmoji = getStatusEmoji(oldStatus);
        String cleanHandlerName = stripMinecraftColors(handlerName);
        
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "📋 Bug Report #" + reportId + " Status Update", 
                    null))
                .setDescription("The status of this bug report has been updated by a staff member.")
                .addField(new WebhookEmbed.EmbedField(true, "� Status Change", 
                    oldStatusEmoji + " **" + oldStatus.getDisplay() + "**\n" +
                    "⬇️\n" +
                    statusEmoji + " **" + newStatus.getDisplay() + "**"))
                .addField(new WebhookEmbed.EmbedField(true, "👨‍💼 Handler", 
                    "🛡️ **" + cleanHandlerName + "**\n" +
                    "*Staff Member*"))
                .addField(new WebhookEmbed.EmbedField(true, "⏰ Updated At", 
                    "🕐 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))))
                .setColor(embedColor.getRGB())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "Report ID: " + reportId + " • Updated by " + cleanHandlerName + " • msnReports-v" + plugin.getDescription().getVersion(), 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("🔧 MSN Reports")
                .addEmbeds(embed)
                .setContent("## " + statusEmoji + " Report Status Updated!\n*Bug report #" + reportId + " has been updated to **" + newStatus.getDisplay() + "***")
                .build();

        statusChangesClient.send(message).thenAccept(sentMessage -> 
            plugin.getLogger().info("Status update for report #" + reportId + " sent to Discord!")
        ).exceptionally(error -> {
            plugin.getLogger().severe("Failed to send status update to Discord: " + error.getMessage());
            return null;
        });
    }

    public void sendCommentNotification(int reportId, String commenterName, String comment) {
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle(
                    "💬 New Comment on Bug Report #" + reportId, 
                    null))
                .setDescription("A new comment has been added to this bug report.")
                .addField(new WebhookEmbed.EmbedField(true, "👤 Commenter", 
                    "🛡️ **" + commenterName + "**\n*Staff Member*"))
                .addField(new WebhookEmbed.EmbedField(true, "⏰ Posted At", 
                    "🕐 " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))))
                .addField(new WebhookEmbed.EmbedField(false, "📝 Comment Content", 
                    "```\n" + truncateText(comment, 500) + "\n```"))
                .setColor(new Color(52, 152, 219).getRGB()) // Blue color
                .setTimestamp(OffsetDateTime.now())
                .setFooter(new WebhookEmbed.EmbedFooter(
                    "Report ID: " + reportId + " • Comment by " + commenterName + " • msnReports-v" + plugin.getDescription().getVersion(), 
                    null))
                .build();

        WebhookMessage message = new WebhookMessageBuilder()
                .setUsername("🔧 MSN Reports")
                .addEmbeds(embed)
                .setContent("## 💬 New Comment Added!\n*" + commenterName + " has commented on bug report #" + reportId + "*")
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