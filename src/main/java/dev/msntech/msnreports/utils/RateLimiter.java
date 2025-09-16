package dev.msntech.msnreports.utils;

import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RateLimiter {
    
    // Rate limits (in milliseconds)
    public static final long BUG_REPORT_COOLDOWN = TimeUnit.MINUTES.toMillis(2); // 2 minutes between reports
    public static final long DISCORD_WEBHOOK_COOLDOWN = TimeUnit.SECONDS.toMillis(5); // 5 seconds between webhook calls
    public static final long COMMENT_COOLDOWN = TimeUnit.SECONDS.toMillis(30); // 30 seconds between comments
    
    // Storage for last action times
    private static final ConcurrentHashMap<String, Long> bugReportCooldowns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> commentCooldowns = new ConcurrentHashMap<>();
    private static volatile long lastDiscordWebhookTime = 0;
    
    /**
     * Checks if a player can submit a bug report
     * @param player The player to check
     * @return true if allowed, false if still on cooldown
     */
    public static boolean canSubmitBugReport(Player player) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        Long lastTime = bugReportCooldowns.get(playerId);
        if (lastTime == null) {
            return true;
        }
        
        return (currentTime - lastTime) >= BUG_REPORT_COOLDOWN;
    }
    
    /**
     * Records that a player has submitted a bug report
     * @param player The player who submitted the report
     */
    public static void recordBugReport(Player player) {
        String playerId = player.getUniqueId().toString();
        bugReportCooldowns.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Gets the remaining cooldown time for bug reports
     * @param player The player to check
     * @return remaining time in seconds, 0 if no cooldown
     */
    public static long getBugReportCooldownRemaining(Player player) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        Long lastTime = bugReportCooldowns.get(playerId);
        if (lastTime == null) {
            return 0;
        }
        
        long elapsed = currentTime - lastTime;
        long remaining = BUG_REPORT_COOLDOWN - elapsed;
        
        return remaining > 0 ? TimeUnit.MILLISECONDS.toSeconds(remaining) : 0;
    }
    
    /**
     * Checks if a player can add a comment
     * @param player The player to check
     * @return true if allowed, false if still on cooldown
     */
    public static boolean canAddComment(Player player) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        Long lastTime = commentCooldowns.get(playerId);
        if (lastTime == null) {
            return true;
        }
        
        return (currentTime - lastTime) >= COMMENT_COOLDOWN;
    }
    
    /**
     * Records that a player has added a comment
     * @param player The player who added the comment
     */
    public static void recordComment(Player player) {
        String playerId = player.getUniqueId().toString();
        commentCooldowns.put(playerId, System.currentTimeMillis());
    }
    
    /**
     * Gets the remaining cooldown time for comments
     * @param player The player to check
     * @return remaining time in seconds, 0 if no cooldown
     */
    public static long getCommentCooldownRemaining(Player player) {
        String playerId = player.getUniqueId().toString();
        long currentTime = System.currentTimeMillis();
        
        Long lastTime = commentCooldowns.get(playerId);
        if (lastTime == null) {
            return 0;
        }
        
        long elapsed = currentTime - lastTime;
        long remaining = COMMENT_COOLDOWN - elapsed;
        
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * Checks if a Discord webhook can be sent
     * @return true if allowed, false if still on cooldown
     */
    public static boolean canSendDiscordWebhook() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastDiscordWebhookTime) >= DISCORD_WEBHOOK_COOLDOWN;
    }
    
    /**
     * Records that a Discord webhook was sent
     */
    public static void recordDiscordWebhook() {
        lastDiscordWebhookTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the remaining cooldown time for Discord webhooks
     * @return remaining time in seconds, 0 if no cooldown
     */
    public static long getDiscordWebhookCooldownRemaining() {
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - lastDiscordWebhookTime;
        long remaining = DISCORD_WEBHOOK_COOLDOWN - elapsed;
        
        return remaining > 0 ? remaining / 1000 : 0;
    }
    
    /**
     * Cleans up old entries to prevent memory leaks
     * Should be called periodically
     */
    public static void cleanup() {
        long currentTime = System.currentTimeMillis();
        
        // Remove bug report cooldowns older than the cooldown period
        bugReportCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > BUG_REPORT_COOLDOWN * 2);
        
        // Remove comment cooldowns older than the cooldown period
        commentCooldowns.entrySet().removeIf(entry -> 
                (currentTime - entry.getValue()) > COMMENT_COOLDOWN * 2);
    }
}