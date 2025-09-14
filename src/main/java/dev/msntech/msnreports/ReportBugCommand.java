package dev.msntech.msnreports;

import dev.msntech.msnreports.utils.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportBugCommand implements CommandExecutor, Listener {
    private final App plugin;
    private final BugReportListener reportListener;
    private final Map<UUID, Boolean> awaitingReport;

    public ReportBugCommand(App plugin) {
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
        }

        return true;
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!awaitingReport.containsKey(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        if (message.equalsIgnoreCase("cancel")) {
            awaitingReport.remove(player.getUniqueId());
            player.sendMessage(Component.text("Bug report cancelled.")
                    .color(NamedTextColor.RED));
            return;
        }

        // Create the bug report
        BugReport report = new BugReport(player, message);
        awaitingReport.remove(player.getUniqueId());

        // Open confirmation GUI using Folia's region-based scheduling
        player.getScheduler().run(plugin, (task) -> {
            new BugReportGUI(plugin, player, report, reportListener).openConfirmationGUI();
        }, () -> {
            player.sendMessage(Component.text("Failed to open bug report GUI. Please try again.")
                    .color(NamedTextColor.RED));
        });
    }

    public BugReportListener getReportListener() {
        return reportListener;
    }
}