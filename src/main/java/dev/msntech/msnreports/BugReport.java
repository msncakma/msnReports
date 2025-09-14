package dev.msntech.msnreports;

import org.bukkit.entity.Player;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BugReport {
    private final String playerName;
    private final String playerUUID;
    private final String description;
    private final String location;
    private final String timestamp;
    private final String ipAddress;
    private final String gameMode;
    private final double health;
    private final int level;
    private final String inventory;

    public BugReport(Player player, String description) {
        this.playerName = player.getName();
        this.playerUUID = player.getUniqueId().toString();
        this.description = description;
        this.location = String.format("World: %s, X: %.2f, Y: %.2f, Z: %.2f",
                player.getWorld().getName(),
                player.getLocation().getX(),
                player.getLocation().getY(),
                player.getLocation().getZ());
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.ipAddress = player.getAddress() != null ? player.getAddress().getAddress().getHostAddress() : "unknown";
        this.gameMode = player.getGameMode().toString();
        this.health = player.getHealth();
        this.level = player.getLevel();
        this.inventory = serializeInventory(player);
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPlayerUUID() {
        return playerUUID;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getGameMode() {
        return gameMode;
    }

    public double getHealth() {
        return health;
    }

    public int getLevel() {
        return level;
    }

    public String getInventory() {
        return inventory;
    }

    private String serializeInventory(Player player) {
        StringBuilder inv = new StringBuilder();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            var item = player.getInventory().getItem(i);
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                inv.append(String.format("Slot %d: %s x%d", 
                    i, 
                    item.getType().toString(), 
                    item.getAmount()
                )).append(", ");
            }
        }
        return inv.length() > 0 ? inv.substring(0, inv.length() - 2) : "Empty";
    }
}