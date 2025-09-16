package dev.msntech.msnreports.utils;

import dev.msntech.msnreports.App;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/msncakma/msnReports/releases/latest";
    private final App plugin;
    private String latestVersion;
    private boolean updateAvailable = false;

    public UpdateChecker(App plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Boolean> checkForUpdates() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(GITHUB_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    // Simple JSON parsing for tag_name
                    String responseStr = response.toString();
                    int tagStart = responseStr.indexOf("\"tag_name\":\"") + 12;
                    int tagEnd = responseStr.indexOf("\"", tagStart);
                    
                    if (tagStart > 11 && tagEnd > tagStart) {
                        latestVersion = responseStr.substring(tagStart, tagEnd);
                        String currentVersion = plugin.getDescription().getVersion();
                        
                        updateAvailable = !latestVersion.equals(currentVersion) && !latestVersion.equals("v" + currentVersion);
                        
                        plugin.getLogger().info("Update check completed. Current: " + currentVersion + ", Latest: " + latestVersion);
                        return updateAvailable;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
            }
            return false;
        });
    }

    public void notifyAdminsIfUpdateAvailable() {
        if (!updateAvailable || !plugin.shouldNotifyAdminsOfUpdates()) {
            return;
        }

        Component updateMessage = Component.text("🎉 ", NamedTextColor.GOLD)
                .append(Component.text("A new version of MSNReports is available!", NamedTextColor.GREEN))
                .append(Component.text("\n📦 Current: ", NamedTextColor.GRAY))
                .append(Component.text(plugin.getDescription().getVersion(), NamedTextColor.YELLOW))
                .append(Component.text(" → Latest: ", NamedTextColor.GRAY))
                .append(Component.text(latestVersion, NamedTextColor.GREEN))
                .append(Component.text("\n🔗 ", NamedTextColor.GRAY))
                .append(Component.text("Click here to download", NamedTextColor.AQUA)
                        .clickEvent(ClickEvent.openUrl("https://github.com/msncakma/msnReports/releases/latest"))
                        .hoverEvent(HoverEvent.showText(Component.text("Open download page in browser"))));

        // Notify online admins
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("msnreports.admin.notify")) {
                player.sendMessage(updateMessage);
            }
        }

        plugin.getLogger().info("Update notification sent to online administrators.");
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}