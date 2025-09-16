package dev.msntech.msnreports;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import dev.msntech.msnreports.database.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
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

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        // Check if this is our bug report GUI
        String title = event.getView().title().toString();
        if (!title.contains("Confirm Bug Report")) return;
        
        // Always cancel the event to prevent item theft/movement
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        BugReport pendingReport = pendingReports.get(player.getUniqueId());
        if (pendingReport == null) {
            player.closeInventory();
            player.sendMessage(Component.text("No pending bug report found. Please try again.")
                    .color(NamedTextColor.RED));
            return;
        }
        
        // Handle clicks based on item type
        switch (clickedItem.getType()) {
            case EMERALD_BLOCK:
                // Confirm button clicked
                player.closeInventory();
                player.sendMessage(Component.text("✅ Submitting your bug report...")
                        .color(NamedTextColor.GREEN));
                submitBugReport(pendingReport, player);
                pendingReports.remove(player.getUniqueId());
                break;
                
            case REDSTONE_BLOCK:
                // Cancel button clicked
                player.closeInventory();
                player.sendMessage(Component.text("❌ Bug report cancelled.")
                        .color(NamedTextColor.YELLOW));
                pendingReports.remove(player.getUniqueId());
                break;
                
            default:
                // For other items, just play a sound to indicate they clicked something
                player.playSound(player.getLocation(), 
                    org.bukkit.Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                break;
        }
    }
    
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        // Check if this is our bug report GUI
        String title = event.getView().title().toString();
        if (title.contains("Confirm Bug Report")) {
            event.setCancelled(true); // Prevent dragging items
        }
    }
    
    @EventHandler  
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        // Check if this involves our bug report GUI
        String title = event.getDestination().toString();
        if (title.contains("Confirm Bug Report")) {
            event.setCancelled(true); // Prevent item movement
        }
    }
    
    public void showConfirmationGUI(Player player, BugReport report) {
        pendingReports.put(player.getUniqueId(), report);
        
        // Schedule on main thread to avoid async inventory issues
        player.getScheduler().run(plugin, (task) -> {
            BugReportGUI gui = new BugReportGUI(plugin, player, report, this);
            gui.openConfirmationGUI();
        }, () -> {
            // Fallback if scheduling fails
            player.sendMessage(Component.text("Failed to open confirmation GUI. Please try again.")
                    .color(NamedTextColor.RED));
            pendingReports.remove(player.getUniqueId());
        });
    }

    public void submitBugReport(BugReport report, Player player) {
        player.getScheduler().run(plugin, (task) -> {
            // Save to database first to get the report ID
            int reportId = plugin.getDatabaseManager().saveBugReport(report);
            if (reportId > 0) {
                // Send enhanced Discord notification with report ID
                webhookSender.sendBugReport(report, reportId, player);
                player.sendMessage(Component.text("Your bug report #" + reportId + " has been submitted. Thank you!")
                        .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to save bug report. Please try again.")
                        .color(NamedTextColor.RED));
            }
        }, () -> {
            player.sendMessage(Component.text("Failed to send bug report. Please try again.")
                    .color(NamedTextColor.RED));
        });
    }
}