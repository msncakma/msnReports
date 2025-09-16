package dev.msntech.msnreports.listeners;

import dev.msntech.msnreports.App;
import dev.msntech.msnreports.utils.ChatUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminLoginListener implements Listener {
    private final App plugin;

    public AdminLoginListener(App plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if admin notifications are enabled
        if (!plugin.isAdminNotificationsEnabled() || !plugin.isLoginNotificationsEnabled()) {
            return;
        }
        
        // Check if player has permission to receive notifications
        if (!player.hasPermission("msnreports.notify")) {
            return;
        }

        // Delay the notification to let the player fully load
        player.getScheduler().runDelayed(plugin, (task) -> {
            checkForUpdates(player);
        }, null, 40L); // 2 seconds delay
    }

    private void checkForUpdates(Player player) {
        try {
            int openReports = getOpenReportsCount();
            int recentReports = getRecentReportsCount(24); // Last 24 hours
            
            if (openReports > 0 || recentReports > 0) {
                sendNotification(player, openReports, recentReports);
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to check for report updates: " + e.getMessage());
        }
    }

    private int getOpenReportsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bug_reports WHERE status = 'OPEN'";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private int getRecentReportsCount(int hours) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bug_reports WHERE created_at >= datetime('now', '-" + hours + " hours')";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private void sendNotification(Player player, int openReports, int recentReports) {
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("🛡️ Admin Report Summary", NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
        
        if (openReports > 0) {
            Component openMessage = Component.text("📋 Open Reports: ", NamedTextColor.WHITE)
                    .append(Component.text(openReports, NamedTextColor.RED)
                            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" requiring attention", NamedTextColor.GRAY));
            
            Component clickableOpen = openMessage
                    .clickEvent(ClickEvent.runCommand("/managereports list"))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view open reports", NamedTextColor.YELLOW)));
            
            player.sendMessage(clickableOpen);
        }
        
        if (recentReports > 0) {
            Component recentMessage = Component.text("🕐 Recent Reports: ", NamedTextColor.WHITE)
                    .append(Component.text(recentReports, NamedTextColor.YELLOW)
                            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text(" in the last 24 hours", NamedTextColor.GRAY));
            
            player.sendMessage(recentMessage);
        }
        
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
        
        Component manageCommand = Component.text("💡 Use ", NamedTextColor.GREEN)
                .append(Component.text("/managereports", NamedTextColor.YELLOW)
                        .clickEvent(ClickEvent.suggestCommand("/managereports "))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to suggest command", NamedTextColor.YELLOW))))
                .append(Component.text(" to manage reports", NamedTextColor.GREEN));
        
        player.sendMessage(manageCommand);
        
        Component disableCommand = Component.text("🔇 Use ", NamedTextColor.GRAY)
                .append(Component.text("/managereports notifications", NamedTextColor.DARK_GRAY)
                        .clickEvent(ClickEvent.suggestCommand("/managereports notifications"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to toggle notifications", NamedTextColor.YELLOW))))
                .append(Component.text(" to toggle these notifications", NamedTextColor.GRAY));
        
        player.sendMessage(disableCommand);
    }
}