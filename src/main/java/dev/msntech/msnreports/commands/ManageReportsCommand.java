package dev.msntech.msnreports.commands;

import dev.msntech.msnreports.App;
import dev.msntech.msnreports.managers.ReportManager;
import dev.msntech.msnreports.models.ReportStatus;
import dev.msntech.msnreports.utils.ChatUtils;
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
            showReportList(player, 1);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                int page = args.length > 1 ? Integer.parseInt(args[1]) : 1;
                showReportList(player, page);
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
                
            default:
                sendHelpMessage(player);
        }

        return true;
    }

    private void showReportList(Player player, int page) {
        List<Map<String, String>> reports = reportManager.getReports(page, REPORTS_PER_PAGE);
        
        player.sendMessage(ChatUtils.getPrefix().append(ChatUtils.createHeader("Bug Reports - Page " + page)));
        
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

    private void sendHelpMessage(Player player) {
        player.sendMessage(Component.text("=== MSNReports Management Commands ===")
                .color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/managereports list [page] - List all reports")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports view <id> - View report details")
                .color(NamedTextColor.WHITE));
        player.sendMessage(Component.text("/managereports status <id> <status> - Update report status")
                .color(NamedTextColor.WHITE));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.addAll(Arrays.asList("list", "view", "status"));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("status")) {
            for (ReportStatus status : ReportStatus.values()) {
                completions.add(status.name().toLowerCase());
            }
        }
        
        return completions;
    }
}