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
        // Store the report for later use
        listener.addPendingReport(player, report);

        Inventory gui = Bukkit.createInventory(null, 27, Component.text("Confirm Bug Report"));

        // Report details (paper)
        ItemStack reportDetails = new ItemStack(Material.PAPER);
        ItemMeta reportMeta = reportDetails.getItemMeta();
        reportMeta.displayName(Component.text("Bug Report Details").color(NamedTextColor.YELLOW));
        reportMeta.lore(Arrays.asList(
            Component.text("Description: " + report.getDescription()).color(NamedTextColor.GRAY),
            Component.text("Location: " + report.getLocation()).color(NamedTextColor.GRAY),
            Component.text("Time: " + report.getTimestamp()).color(NamedTextColor.GRAY)
        ));
        reportDetails.setItemMeta(reportMeta);

        // Confirm button (green wool)
        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.displayName(Component.text("Submit Report").color(NamedTextColor.GREEN));
        confirm.setItemMeta(confirmMeta);

        // Cancel button (red wool)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.displayName(Component.text("Cancel").color(NamedTextColor.RED));
        cancel.setItemMeta(cancelMeta);

        // Place items in GUI
        gui.setItem(13, reportDetails);
        gui.setItem(11, confirm);
        gui.setItem(15, cancel);

        player.openInventory(gui);
    }
}