package dev.msntech.msnreports.managers;

import dev.msntech.msnreports.App;
import dev.msntech.msnreports.DiscordWebhookSender;
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
        return getFilteredReports(page, perPage, null);
    }
    
    public List<Map<String, String>> getFilteredReports(int page, int perPage, String statusFilter) {
        List<Map<String, String>> reports = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, player_name, description, status, created_at FROM bug_reports");
        
        List<String> conditions = new ArrayList<>();
        if (statusFilter != null && !statusFilter.isEmpty()) {
            conditions.add("status = ?");
        }
        
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (statusFilter != null && !statusFilter.isEmpty()) {
                stmt.setString(paramIndex++, statusFilter);
            }
            
            stmt.setInt(paramIndex++, perPage);
            stmt.setInt(paramIndex, (page - 1) * perPage);
            
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
            plugin.getLogger().severe("Failed to fetch filtered reports: " + e.getMessage());
        }
        
        return reports;
    }

    public boolean updateReportStatus(int reportId, ReportStatus newStatus, Player staff) {
        // First get the current status
        String getCurrentStatusSql = "SELECT status FROM bug_reports WHERE id = ?";
        ReportStatus oldStatus = null;
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement getStmt = conn.prepareStatement(getCurrentStatusSql)) {
            
            getStmt.setInt(1, reportId);
            ResultSet rs = getStmt.executeQuery();
            if (rs.next()) {
                oldStatus = ReportStatus.valueOf(rs.getString("status"));
            } else {
                return false; // Report not found
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get current report status: " + e.getMessage());
            return false;
        }
        
        // Update the status
        String updateSql = "UPDATE bug_reports SET status = ?, handler = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            
            stmt.setString(1, newStatus.name());
            stmt.setString(2, staff.getName());
            stmt.setInt(3, reportId);
            
            int updated = stmt.executeUpdate();
            if (updated > 0) {
                notifyStaffOfStatusChange(reportId, oldStatus, newStatus, staff);
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update report status: " + e.getMessage());
        }
        
        return false;
    }

    private void notifyStaffOfStatusChange(int reportId, ReportStatus oldStatus, ReportStatus newStatus, Player updatedBy) {
        String message = String.format("§6Report #%d status updated from %s to %s by %s", 
            reportId, oldStatus.getDisplay(), newStatus.getDisplay(), updatedBy.getName());
        
        // Notify online staff
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            if (player.hasPermission("msnreports.manage")) {
                player.sendMessage(Component.text(message));
            }
        });
        
        // Send Discord notification
        try {
            App app = (App) plugin;
            DiscordWebhookSender webhookSender = app.getWebhookSender();
            if (webhookSender != null) {
                webhookSender.sendStatusUpdate(reportId, oldStatus, newStatus, updatedBy.getName());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord status update: " + e.getMessage());
        }
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

    public boolean addReportComment(int reportId, String author, String comment) {
        String sql = "SELECT comments FROM bug_reports WHERE id = ?";
        String updateSql = "UPDATE bug_reports SET comments = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Get existing comments
            String existingComments = "";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reportId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    String encrypted = rs.getString("comments");
                    if (encrypted != null && !encrypted.isEmpty()) {
                        existingComments = plugin.getDatabaseManager().decrypt(encrypted);
                    }
                } else {
                    return false; // Report not found
                }
            }
            
            // Create new comment entry
            String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String newComment = String.format("[%s] 📝 %s: %s", timestamp, author, comment);
            
            // Append to existing comments
            String updatedComments = existingComments.isEmpty() ? 
                newComment : existingComments + "\n" + newComment;
            
            // Update the database
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setString(1, plugin.getDatabaseManager().encrypt(updatedComments));
                updateStmt.setInt(2, reportId);
                boolean success = updateStmt.executeUpdate() > 0;
                
                if (success) {
                    // Send Discord notification for comment
                    try {
                        App app = (App) plugin;
                        DiscordWebhookSender webhookSender = app.getWebhookSender();
                        if (webhookSender != null) {
                            webhookSender.sendCommentNotification(reportId, author, comment);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send Discord comment notification: " + e.getMessage());
                    }
                }
                
                return success;
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to add comment: " + e.getMessage());
        }
        
        return false;
    }

    public List<String> getReportComments(int reportId) {
        List<String> comments = new ArrayList<>();
        String sql = "SELECT comments FROM bug_reports WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reportId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                String encrypted = rs.getString("comments");
                if (encrypted != null && !encrypted.isEmpty()) {
                    String decrypted = plugin.getDatabaseManager().decrypt(encrypted);
                    String[] commentLines = decrypted.split("\n");
                    for (String line : commentLines) {
                        if (!line.trim().isEmpty()) {
                            comments.add(line);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch comments: " + e.getMessage());
        }
        
        return comments;
    }
    
    public java.util.Optional<Map<String, Object>> getReport(int reportId) {
        String sql = "SELECT * FROM bug_reports WHERE id = ?";
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, reportId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                Map<String, Object> report = new HashMap<>();
                report.put("id", rs.getInt("id"));
                report.put("playerName", rs.getString("player_name"));
                report.put("playerUUID", rs.getString("player_uuid"));
                report.put("description", rs.getString("description"));
                
                // Construct location from separate world, x, y, z columns
                String location = "World: " + rs.getString("world") + 
                                ", X: " + rs.getDouble("x") + 
                                ", Y: " + rs.getDouble("y") + 
                                ", Z: " + rs.getDouble("z");
                report.put("location", location);
                
                report.put("gameMode", rs.getString("game_mode"));
                report.put("health", rs.getDouble("health"));
                report.put("level", rs.getInt("level"));
                report.put("inventory", rs.getString("inventory"));
                report.put("status", ReportStatus.valueOf(rs.getString("status")));
                report.put("createdAt", rs.getTimestamp("created_at"));
                
                return java.util.Optional.of(report);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to fetch report #" + reportId + ": " + e.getMessage());
        }
        return java.util.Optional.empty();
    }
    
    public boolean deleteReport(int reportId) {
        String deleteReportSql = "DELETE FROM bug_reports WHERE id = ?";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // Start transaction
            conn.setAutoCommit(false);
            
            try {
                // Delete the report (comments are stored in the same table, so they'll be deleted automatically)
                try (PreparedStatement stmt = conn.prepareStatement(deleteReportSql)) {
                    stmt.setInt(1, reportId);
                    int rowsAffected = stmt.executeUpdate();
                    
                    if (rowsAffected > 0) {
                        conn.commit();
                        plugin.getLogger().info("Successfully deleted report #" + reportId + " and its comments");
                        return true;
                    } else {
                        conn.rollback();
                        plugin.getLogger().warning("No report found with ID #" + reportId);
                        return false;
                    }
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete report #" + reportId + ": " + e.getMessage());
            return false;
        }
    }
}