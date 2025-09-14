package dev.msntech.msnreports;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import java.awt.Color;

public class DiscordWebhookSender {
    private final WebhookClient client;
    private final App plugin;

    public DiscordWebhookSender(App plugin) {
        this.plugin = plugin;
        this.client = new WebhookClientBuilder(plugin.getWebhookUrl()).build();
    }

    public void sendBugReport(BugReport report) {
        WebhookEmbed embed = new WebhookEmbedBuilder()
                .setTitle(new WebhookEmbed.EmbedTitle("Bug Report from " + report.getPlayerName(), null))
                .setDescription(report.getDescription())
                .addField(new WebhookEmbed.EmbedField(true, "Player Info", 
                    String.format("UUID: %s\nHealth: %.1f\nLevel: %d\nGameMode: %s", 
                        report.getPlayerUUID(), report.getHealth(), 
                        report.getLevel(), report.getGameMode())))
                .addField(new WebhookEmbed.EmbedField(true, "Location", report.getLocation()))
                .addField(new WebhookEmbed.EmbedField(true, "Time", report.getTimestamp()))
                .addField(new WebhookEmbed.EmbedField(false, "Inventory", report.getInventory()))
                .setColor(Color.RED.getRGB())
                .setFooter(new WebhookEmbed.EmbedFooter("Report ID: " + System.currentTimeMillis(), null))
                .build();

        client.send(embed).thenAccept(message -> 
            plugin.getLogger().info("Bug report sent to Discord successfully!")
        ).exceptionally(error -> {
            plugin.getLogger().severe("Failed to send bug report to Discord: " + error.getMessage());
            return null;
        });
    }

    public void close() {
        if (client != null) {
            client.close();
        }
    }
}