package dev.msntech.msnreports;

import dev.msntech.msnreports.utils.ChatUtils;
import dev.msntech.msnreports.utils.ValidationUtil;
import dev.msntech.msnreports.utils.RateLimiter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportCommand implements CommandExecutor, TabCompleter, Listener {
    private final App plugin;
    private final BugReportListener reportListener;
    private final Map<UUID, Boolean> awaitingReport;

    public ReportCommand(App plugin) {
        this.plugin = plugin;
        this.reportListener = new BugReportListener(plugin);
        this.awaitingReport = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("This command can only be used by players!")
                            .color(NamedTextColor.RED)));
            return true;
        }

        if (!player.hasPermission("msnreports.report")) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("You don't have permission to report bugs!")
                            .color(NamedTextColor.RED)));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Usage: /report <bug|suggestion> [description]")
                            .color(NamedTextColor.YELLOW)));
            return true;
        }

        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "bug":
                handleBugReport(player, args);
                break;
            case "suggestion":
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Suggestion reporting is not yet implemented!")
                                .color(NamedTextColor.YELLOW)));
                break;
            default:
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Unknown subcommand! Use: /report bug or /report suggestion")
                                .color(NamedTextColor.RED)));
        }

        return true;
    }

    private void handleBugReport(Player player, String[] args) {
        // Check rate limiting
        if (!RateLimiter.canSubmitBugReport(player)) {
            long remainingSeconds = RateLimiter.getBugReportCooldownRemaining(player);
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Please wait " + remainingSeconds + " seconds before submitting another bug report.")
                            .color(NamedTextColor.RED)));
            return;
        }

        if (args.length == 1) {
            // Start the report process
            awaitingReport.put(player.getUniqueId(), true);
            player.sendMessage(ChatUtils.getPrefix()
                    .append(Component.text("Please type your bug report in chat.")
                            .color(NamedTextColor.GREEN))
                    .append(Component.text(" Type ")
                            .color(NamedTextColor.GRAY))
                    .append(Component.text("'cancel'")
                            .color(NamedTextColor.RED))
                    .append(Component.text(" to cancel.")
                            .color(NamedTextColor.GRAY)));
        } else {
            // Bug description provided directly in command
            StringBuilder description = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                description.append(args[i]);
                if (i < args.length - 1) description.append(" ");
            }
            
            try {
                String validatedDescription = ValidationUtil.validateDescription(description.toString());
                ValidationUtil.validateLocation(player.getLocation());
                
                BugReport report = new BugReport(player, validatedDescription);
                
                // Show the GUI confirmation screen
                reportListener.showConfirmationGUI(player, report);
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Invalid bug report: " + e.getMessage())
                                .color(NamedTextColor.RED)));
            } catch (Exception e) {
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Failed to create bug report. Please try again.")
                                .color(NamedTextColor.RED)));
                plugin.getLogger().severe("Failed to create bug report: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Handle awaiting bug report input
        if (awaitingReport.containsKey(playerId)) {
            event.setCancelled(true);
            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            if (message.equalsIgnoreCase("cancel")) {
                awaitingReport.remove(playerId);
                player.sendMessage(Component.text("Bug report cancelled.")
                        .color(NamedTextColor.RED));
                return;
            }

            try {
                String validatedDescription = ValidationUtil.validateDescription(message);
                ValidationUtil.validateLocation(player.getLocation());
                
                BugReport report = new BugReport(player, validatedDescription);
                awaitingReport.remove(playerId);

                // Show the GUI confirmation screen
                reportListener.showConfirmationGUI(player, report);
                
            } catch (IllegalArgumentException e) {
                awaitingReport.remove(playerId);
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Invalid bug report: " + e.getMessage())
                                .color(NamedTextColor.RED)));
            } catch (Exception e) {
                awaitingReport.remove(playerId);
                player.sendMessage(ChatUtils.getPrefix()
                        .append(Component.text("Failed to create bug report. Please try again.")
                                .color(NamedTextColor.RED)));
                plugin.getLogger().severe("Failed to create bug report: " + e.getMessage());
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("bug", "suggestion");
        }
        return Arrays.asList();
    }

    public BugReportListener getReportListener() {
        return reportListener;
    }
}