package dev.msntech.msnreports.managers;

import dev.msntech.msnreports.App;
import dev.msntech.msnreports.models.ReportStatus;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ReportManager {
    private final App plugin;

    public ReportManager(App plugin) {
        this.plugin = plugin;
    }

    public List<Map<String, String>> getReports(int page, int perPage) {
        List<Map<String, String>> reports = new ArrayList<>();
        String sql = "SELECT id, player_name, description, status, created_at FROM bug_reports ORDER BY created_at DESC LIMIT ? OFFSET ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, perPage);
            stmt.setInt(2, (page - 1) * perPage);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, String> report = new HashMap<>();
                report.put("id", rs.getString("id"));
                report.put("player", plugin.getDatabaseManager().decrypt(rs.getString("player_name")));
                report.put("description", plugin.getDatabaseManager().decrypt(rs.getString("description")));
                report.put("status", rs.getString("status"));
                report.put("created_at", rs.getString("created_at"));
                reports.add(report);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch reports: " + e.getMessage());
        }
        
        return reports;
    }

    public boolean updateReportStatus(int reportId, ReportStatus newStatus, Player staff) {
        String sql = "UPDATE bug_reports SET status = ? WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newStatus.name());
            stmt.setInt(2, reportId);
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                notifyStaffOfStatusChange(reportId, newStatus, staff);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update report status: " + e.getMessage());
        }
        
        return false;
    }

    private void notifyStaffOfStatusChange(int reportId, ReportStatus newStatus, Player updatedBy) {
        String message = String.format("§6Report #%d status updated to %s by %s", 
            reportId, newStatus.getDisplay(), updatedBy.getName());
        
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("msnreports.manage")) {
                player.sendMessage(Component.text(message));
            }
        });
    }

    public Map<String, String> getReportDetails(int reportId) {
        String sql = "SELECT * FROM bug_reports WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reportId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, String> details = new HashMap<>();
                details.put("id", rs.getString("id"));
                details.put("player", plugin.getDatabaseManager().decrypt(rs.getString("player_name")));
                details.put("description", plugin.getDatabaseManager().decrypt(rs.getString("description")));
                details.put("world", plugin.getDatabaseManager().decrypt(rs.getString("world")));
                details.put("x", rs.getString("x"));
                details.put("y", rs.getString("y"));
                details.put("z", rs.getString("z"));
                details.put("status", rs.getString("status"));
                details.put("created_at", rs.getString("created_at"));
                details.put("inventory", plugin.getDatabaseManager().decrypt(rs.getString("inventory")));
                return details;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch report details: " + e.getMessage());
        }
        
        return null;
    }
}