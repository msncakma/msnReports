package dev.msntech.msnreports.commands;

import dev.msntech.msnreports.App;
import dev.msntech.msnreports.managers.ReportManager;
import dev.msntech.msnreports.models.ReportStatus;
import dev.msntech.msnreports.utils.ChatUtils;
import dev.msntech.msnreports.utils.ValidationUtil;
import dev.msntech.msnreports.utils.RateLimiter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;

public class ManageReportsCommand implements CommandExecutor, TabCompleter {
    private final App plugin;
    private final ReportManager reportManager;
    private static final int REPORTS_PER_PAGE = 5;

    public ManageReportsCommand(App plugin) {
        this.plugin = plugin;
        this.reportManager = plugin.getReportManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("This command can only be used by players!")
                            .color(NamedTextColor.RED)));
            return true;
        }

        if (!player.hasPermission("msnreports.manage")) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("You don't have permission to manage reports!")
                            .color(NamedTextColor.RED)));
            return true;
        }

        if (args.length == 0) {
            showReportList(player, 1, null);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                showReportList(player, page, null);
                break;
                
            case "filter":
                if (args.length < 2) {
                    showFilterHelp(player);
                    return true;
                }
                handleFilterCommand(player, args);
                break;
                
            case "comment":
                if (!player.hasPermission("msnreports.manage.comment")) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("You don't have permission to add comments!")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                
                // Check rate limiting for comments
                if (!RateLimiter.canAddComment(player)) {
                    long remainingSeconds = RateLimiter.getCommentCooldownRemaining(player);
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("Please wait " + remainingSeconds + " seconds before adding another comment.")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                
                if (args.length < 3) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("Usage: /managereports comment <id> <message>")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                addReportComment(player, args);
                break;
                
            case "view":
                if (args.length < 2) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("Usage: /managereports view <id>")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                showReportDetails(player, Integer.parseInt(args[1]));
                break;
                
            case "status":
                if (args.length < 3) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("Usage: /managereports status <id> <status>")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                updateReportStatus(player, Integer.parseInt(args[1]), args[2]);
                break;
                
            case "reload":
                if (!player.hasPermission("msnreports.admin.reload")) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("You don't have permission to reload the config!")
                                    .color(NamedTextColor.RED)));
                    return true;
                }
                reloadConfig(player);
                break;
                
            case "notifications":
                handleNotificationsCommand(player);
                break;
                
            default:
                sendHelpMessage(player);
        }

        return true;
    }

    private void showReportList(Player player, int page, String statusFilter) {
        List<Map<String, String>> reports = reportManager.getFilteredReports(page, REPORTS_PER_PAGE, statusFilter);
        
        // Build title with filter info
        StringBuilder titleBuilder = new StringBuilder("Bug Reports - Page " + page);
        if (statusFilter != null) {
            titleBuilder.append(" (Filtered");
            if (statusFilter != null) {
                titleBuilder.append(" Status: ").append(statusFilter);
            }
            titleBuilder.append(")");
        }
        
        player.sendMessage(ChatUtils.getPrefix().append(ChatUtils.createHeader(titleBuilder.toString())));
        
        if (reports.isEmpty()) {
            player.sendMessage(Component.text("No reports found!")
                    .color(NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.empty());
        for (Map<String, String> report : reports) {
            String id = report.get("id");
            String desc = report.get("description").substring(0, Math.min(30, report.get("description").length()));
            
            Component reportLine = Component.text("#" + id + " ")
                    .color(NamedTextColor.GOLD)
                    .append(ChatUtils.createStatusBadge(report.get("status")))
                    .append(Component.text(" " + report.get("player") + ": ")
                            .color(NamedTextColor.YELLOW))
                    .append(Component.text(desc)
                            .color(NamedTextColor.WHITE))
                    .clickEvent(ClickEvent.runCommand("/mr view " + id))
                    .hoverEvent(HoverEvent.showText(Component.text("Click to view details")
                            .color(NamedTextColor.GRAY)));
            
            player.sendMessage(reportLine);
        }
        
        // Navigation buttons
        player.sendMessage(Component.empty());
        Component navigation = Component.empty();
        if (page > 1) {
            navigation = navigation.append(ChatUtils.createButton("◀ Previous", "/mr list " + (page - 1), NamedTextColor.AQUA))
                    .append(Component.text(" "));
        }
        navigation = navigation.append(ChatUtils.createButton("Refresh", "/mr list " + page, NamedTextColor.GREEN))
                .append(Component.text(" "));
        if (!reports.isEmpty() && reports.size() >= REPORTS_PER_PAGE) {
            navigation = navigation.append(ChatUtils.createButton("Next ▶", "/mr list " + (page + 1), NamedTextColor.AQUA));
        }
        player.sendMessage(navigation);
    }

    private void showReportDetails(Player player, int reportId) {
        Map<String, String> details = reportManager.getReportDetails(reportId);
        
        if (details == null) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Report not found!")
                            .color(NamedTextColor.RED)));
            return;
        }

        player.sendMessage(ChatUtils.getPrefix()
                .append(ChatUtils.createHeader("Report #" + reportId)));
        
        player.sendMessage(Component.empty());
        // Info lines
        player.sendMessage(ChatUtils.createInfoLine("Reporter", details.get("player")));
        player.sendMessage(ChatUtils.createInfoLine("Status", ReportStatus.fromString(details.get("status")).getDisplay()));
        player.sendMessage(ChatUtils.createInfoLine("Description", details.get("description")));
        player.sendMessage(ChatUtils.createInfoLine("Location", 
                String.format("%s at %.1f, %.1f, %.1f", 
                    details.get("world"),
                    Double.parseDouble(details.get("x")),
                    Double.parseDouble(details.get("y")),
                    Double.parseDouble(details.get("z")))));
        player.sendMessage(ChatUtils.createInfoLine("Time", details.get("created_at")));
        player.sendMessage(ChatUtils.createInfoLine("Inventory", details.get("inventory")));
        
        // Comments section
        List<String> comments = reportManager.getReportComments(reportId);
        if (!comments.isEmpty()) {
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("Comments:")
                    .color(NamedTextColor.GOLD)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            
            for (String comment : comments) {
                player.sendMessage(Component.text("  " + comment)
                        .color(NamedTextColor.GRAY));
            }
        }
        
        // Action buttons
        player.sendMessage(Component.empty());
        Component actions = Component.text("Status: ")
                .color(NamedTextColor.GRAY);
        
        for (ReportStatus status : ReportStatus.values()) {
            if (!status.name().equals(details.get("status"))) {
                actions = actions.append(ChatUtils.createButton(
                        status.getDisplay(), 
                        "/mr status " + reportId + " " + status.name(), 
                        NamedTextColor.WHITE))
                        .append(Component.text(" "));
            }
        }
        
        player.sendMessage(actions);
        
        // Comment button
        player.sendMessage(Component.empty());
        Component commentActions = Component.text("Actions: ")
                .color(NamedTextColor.GRAY)
                .append(ChatUtils.createSuggestButton("💬 Add Comment", "/mr comment " + reportId + " ", NamedTextColor.YELLOW));
        player.sendMessage(commentActions);
        
        // Navigation
        player.sendMessage(Component.empty());
        player.sendMessage(ChatUtils.createButton("◀ Back to List", "/mr list 1", NamedTextColor.AQUA));
    }

    private void updateReportStatus(Player player, int reportId, String status) {
        try {
            ReportStatus newStatus = ReportStatus.valueOf(status.toUpperCase());
            if (reportManager.updateReportStatus(reportId, newStatus, player)) {
                player.sendMessage(Component.text("Report status updated successfully!")
                        .color(NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Failed to update report status!")
                        .color(NamedTextColor.RED));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid status! Valid statuses: " +
                    Arrays.toString(ReportStatus.values()))
                    .color(NamedTextColor.RED));
        }
    }

    private void addReportComment(Player player, String[] args) {
        try {
            int reportId = ValidationUtil.validateReportId(args[1]);
            
            // Join all remaining arguments as the comment message
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                messageBuilder.append(args[i]);
                if (i < args.length - 1) {
                    messageBuilder.append(" ");
                }
            }
            
            // Validate the comment
            String comment = ValidationUtil.validateComment(messageBuilder.toString());
            
            if (reportManager.addReportComment(reportId, player.getName(), comment)) {
                // Record the rate limit after successful comment
                RateLimiter.recordComment(player);
                
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Comment added successfully!")
                                .color(NamedTextColor.GREEN)));
                        
                // Show updated report details
                showReportDetails(player, reportId);
            } else {
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Failed to add comment or report not found!")
                                .color(NamedTextColor.RED)));
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Invalid input: " + e.getMessage())
                            .color(NamedTextColor.RED)));
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== MSNReports Management Commands ===")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/managereports list [page] - List all reports")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports filter status <status> [page] - Filter by status")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports view <id> - View report details")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports comment <id> <message> - Add comment to report")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports status <id> <status> - Update report status")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports notifications - View notification settings")
                .color(NamedTextColor.WHITE));
        if (player.hasPermission("msnreports.admin.reload")) {
            player.sendMessage(Component.text("/managereports reload - Reload plugin configuration")
                    .color(NamedTextColor.YELLOW));
        }
    }

    private void handleFilterCommand(Player player, String[] args) {
        if (args.length < 3) {
            showFilterHelp(player);
            return;
        }

        String filterType = args[1].toLowerCase();
        String filterValue = args[2].toUpperCase();
        int page = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        switch (filterType) {
            case "status":
                try {
                    ReportStatus.valueOf(filterValue);
                    showReportList(player, page, filterValue);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ChatUtils.getPrefix()
                            .append(Component.text("Invalid status! Valid statuses: " + 
                                    Arrays.toString(ReportStatus.values()))
                                    .color(NamedTextColor.RED)));
                }
                break;
            default:
                showFilterHelp(player);
        }
    }

    private void showFilterHelp(Player player) {
        player.sendMessage(ChatUtils.getPrefix()
                .append(ChatUtils.createHeader("Report Filters")));
        player.sendMessage(Component.text("/mr filter status <status> [page]")
                .color(NamedTextColor.YELLOW));
        player.sendMessage(Component.text("  Available statuses: " + Arrays.toString(ReportStatus.values()))
                .color(NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "view", "status", "filter", "comment", "notifications"));
            if (sender.hasPermission("msnreports.admin.reload")) {
                completions.add("reload");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("filter")) {
                completions.addAll(Arrays.asList("status"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("status") && sender.hasPermission("msnreports.manage.status")) {
                completions.addAll(Arrays.stream(ReportStatus.values())
                        .map(status -> status.name().toLowerCase())
                        .toList());
            } else if (args[0].equalsIgnoreCase("filter")) {
                if (args[1].equalsIgnoreCase("status")) {
                    completions.addAll(Arrays.stream(ReportStatus.values())
                            .map(status -> status.name().toLowerCase())
                            .toList());
                }
            }
        }
        
        return completions;
    }
    
    private void reloadConfig(Player player) {
        try {
            // Reload the plugin configuration
            plugin.reloadConfig();
            
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Configuration reloaded successfully!")
                            .color(NamedTextColor.GREEN)));
            
            plugin.getLogger().info("Configuration reloaded by " + player.getName());
            
        } catch (Exception e) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Failed to reload configuration: " + e.getMessage())
                            .color(NamedTextColor.RED)));
            
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleNotificationsCommand(Player player) {
        if (!player.hasPermission("msnreports.notify")) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("You don't have permission to receive notifications!")
                            .color(NamedTextColor.RED)));
            return;
        }
        
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("🔔 Admin Notification Settings", NamedTextColor.YELLOW)
                .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
        
        boolean adminNotifications = plugin.isAdminNotificationsEnabled();
        boolean loginNotifications = plugin.isLoginNotificationsEnabled();
        
        Component adminStatus = Component.text("📋 Admin Notifications: ", NamedTextColor.WHITE)
                .append(Component.text(adminNotifications ? "ENABLED" : "DISABLED", 
                        adminNotifications ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        
        Component loginStatus = Component.text("🚪 Login Notifications: ", NamedTextColor.WHITE)
                .append(Component.text(loginNotifications ? "ENABLED" : "DISABLED", 
                        loginNotifications ? NamedTextColor.GREEN : NamedTextColor.RED)
                        .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
        
        player.sendMessage(adminStatus);
        player.sendMessage(loginStatus);
        
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("💡 To change these settings, edit the config.yml file", NamedTextColor.GRAY));
        player.sendMessage(Component.text("   and use /managereports reload to apply changes", NamedTextColor.GRAY));
        player.sendMessage(Component.text("=".repeat(50)).color(NamedTextColor.GOLD));
    }
}