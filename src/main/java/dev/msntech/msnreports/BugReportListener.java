package dev.msntech.msnreports;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import dev.msntech.msnreports.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

public class BugReportListener implements Listener {
    private final App plugin;
    private final DiscordWebhookSender webhookSender;
    private final Map<UUID, BugReport> pendingReports;

    public BugReportListener(App plugin) {
        this.plugin = plugin;
        this.webhookSender = new DiscordWebhookSender(plugin);
        this.pendingReports = new HashMap<>();
    }

    public void addPendingReport(Player player, BugReport report) {
        pendingReports.put(player.getUniqueId(), report);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!Component.text("Confirm Bug Report").equals(event.getView().title())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getCurrentItem() == null) {
            return;
        }

        // Handle button clicks
        if (event.getSlot() == 11) { // Confirm button
            player.closeInventory();
            // Send report via webhook
            BugReport report = pendingReports.remove(player.getUniqueId());
            if (report != null) {
                player.getScheduler().run(plugin, (task) -> {
                    webhookSender.sendBugReport(report);
                    plugin.getDatabaseManager().saveBugReport(report);
                    player.sendMessage(Component.text("Your bug report has been submitted. Thank you!")
                            .color(NamedTextColor.GREEN));
                }, () -> {
                    player.sendMessage(Component.text("Failed to send bug report. Please try again.")
                            .color(NamedTextColor.RED));
                });
            }
        } else if (event.getSlot() == 15) { // Cancel button
            pendingReports.remove(player.getUniqueId());
            player.closeInventory();
            player.sendMessage(Component.text("Bug report cancelled.")
                    .color(NamedTextColor.RED));
        }
    }
}