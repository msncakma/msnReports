package dev.msntech.msnreports;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import java.util.Arrays;

public class BugReportGUI {
    private final App plugin;
    private final BugReport report;
    private final Player player;
    private final BugReportListener listener;

    public BugReportGUI(App plugin, Player player, BugReport report, BugReportListener listener) {
        this.plugin = plugin;
        this.player = player;
        this.report = report;
        this.listener = listener;
    }

    public void openConfirmationGUI() {
        Inventory gui = Bukkit.createInventory(null, 45, Component.text("✓ Confirm Bug Report ✓").color(NamedTextColor.GOLD));

        // Fill background with gray glass panes
        ItemStack background = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta backgroundMeta = background.getItemMeta();
        backgroundMeta.displayName(Component.text(" "));
        background.setItemMeta(backgroundMeta);
        
        // Fill entire inventory with background
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, background);
        }

        // Report details (enchanted book for a special look)
        ItemStack reportDetails = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta reportMeta = reportDetails.getItemMeta();
        reportMeta.displayName(Component.text("📋 Bug Report Details").color(NamedTextColor.AQUA));
        reportMeta.lore(Arrays.asList(
            Component.text(""),
            Component.text("👤 Reporter: " + report.getPlayerName()).color(NamedTextColor.GRAY),
            Component.text("📝 Description:").color(NamedTextColor.YELLOW),
            Component.text("  " + report.getDescription()).color(NamedTextColor.WHITE),
            Component.text(""),
            Component.text("📍 Location: " + report.getLocation()).color(NamedTextColor.GRAY),
            Component.text("⏰ Time: " + report.getTimestamp()).color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("❓ Game Mode: " + report.getGameMode()).color(NamedTextColor.GRAY),
            Component.text("❤ Health: " + String.format("%.1f", report.getHealth())).color(NamedTextColor.GRAY),
            Component.text("⭐ Level: " + report.getLevel()).color(NamedTextColor.GRAY)
        ));
        reportDetails.setItemMeta(reportMeta);

        // Confirm button (emerald block for better visibility)
        ItemStack confirm = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("✅ SUBMIT REPORT").color(NamedTextColor.GREEN));
        confirmMeta.lore(Arrays.asList(
            Component.text(""),
            Component.text("Click to submit your bug report").color(NamedTextColor.GRAY),
            Component.text("to the administrators!").color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("Your report will be reviewed").color(NamedTextColor.YELLOW),
            Component.text("and you'll receive feedback.").color(NamedTextColor.YELLOW)
        ));
        confirm.setItemMeta(confirmMeta);

        // Cancel button (redstone block for better visibility)
        ItemStack cancel = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("❌ CANCEL REPORT").color(NamedTextColor.RED));
        cancelMeta.lore(Arrays.asList(
            Component.text(""),
            Component.text("Click to cancel and discard").color(NamedTextColor.GRAY),
            Component.text("this bug report.").color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("⚠ This action cannot be undone!").color(NamedTextColor.YELLOW)
        ));
        cancel.setItemMeta(cancelMeta);

        // Information item (knowledge book)
        ItemStack info = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.displayName(Component.text("ℹ Information").color(NamedTextColor.BLUE));
        infoMeta.lore(Arrays.asList(
            Component.text(""),
            Component.text("Please review your bug report").color(NamedTextColor.GRAY),
            Component.text("carefully before submitting.").color(NamedTextColor.GRAY),
            Component.text(""),
            Component.text("✓ Make sure the description is clear").color(NamedTextColor.GREEN),
            Component.text("✓ Include steps to reproduce if possible").color(NamedTextColor.GREEN),
            Component.text("✓ Check if this hasn't been reported").color(NamedTextColor.GREEN)
        ));
        info.setItemMeta(infoMeta);

        // Place items in a nice layout
        gui.setItem(13, reportDetails); // Center top
        gui.setItem(19, confirm);       // Left side
        gui.setItem(25, cancel);        // Right side
        gui.setItem(31, info);          // Center bottom

        // Add decorative elements
        ItemStack decoration = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta decorationMeta = decoration.getItemMeta();
        decorationMeta.displayName(Component.text(""));
        decoration.setItemMeta(decorationMeta);
        
        // Add decorative border around main items
        gui.setItem(4, decoration);   // Top
        gui.setItem(40, decoration);  // Bottom
        gui.setItem(10, decoration);  // Left
        gui.setItem(16, decoration);  // Right

        player.openInventory(gui);
    }
}