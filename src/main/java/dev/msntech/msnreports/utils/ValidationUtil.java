package dev.msntech.msnreports.utils;

import org.bukkit.Location;

import java.util.regex.Pattern;

public class ValidationUtil {
    
    // Length limits
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_COMMENT_LENGTH = 500;
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    
    // Coordinate limits (reasonable Minecraft world bounds)
    public static final double MAX_COORDINATE = 30000000;
    public static final double MIN_COORDINATE = -30000000;
    
    // Patterns for validation
    private static final Pattern SAFE_TEXT_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{S}\\s]+$");
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    
    /**
     * Validates and sanitizes a bug report description
     * @param description The description to validate
     * @return The sanitized description
     * @throws IllegalArgumentException if validation fails
     */
    public static String validateDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be empty");
        }
        
        String trimmed = description.trim();
        
        if (trimmed.length() < MIN_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description must be at least " + MIN_DESCRIPTION_LENGTH + " characters");
        }
        
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("Description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        
        // Check for potentially dangerous patterns
        if (containsDangerousContent(trimmed)) {
            throw new IllegalArgumentException("Description contains invalid characters or patterns");
        }
        
        return sanitizeText(trimmed);
    }
    
    /**
     * Validates a comment
     * @param comment The comment to validate
     * @return The sanitized comment
     * @throws IllegalArgumentException if validation fails
     */
    public static String validateComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty");
        }
        
        String trimmed = comment.trim();
        
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException("Comment cannot exceed " + MAX_COMMENT_LENGTH + " characters");
        }
        
        if (containsDangerousContent(trimmed)) {
            throw new IllegalArgumentException("Comment contains invalid characters or patterns");
        }
        
        return sanitizeText(trimmed);
    }
    
    /**
     * Validates a player name
     * @param playerName The player name to validate
     * @return The validated player name
     * @throws IllegalArgumentException if validation fails
     */
    public static String validatePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Player name cannot be empty");
        }
        
        String trimmed = playerName.trim();
        
        if (!PLAYER_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid player name format");
        }
        
        return trimmed;
    }
    
    /**
     * Validates coordinates from a location
     * @param location The location to validate
     * @throws IllegalArgumentException if coordinates are invalid
     */
    public static void validateLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        
        if (x < MIN_COORDINATE || x > MAX_COORDINATE) {
            throw new IllegalArgumentException("X coordinate out of valid range");
        }
        
        if (y < -64 || y > 320) { // Minecraft world height limits
            throw new IllegalArgumentException("Y coordinate out of valid range");
        }
        
        if (z < MIN_COORDINATE || z > MAX_COORDINATE) {
            throw new IllegalArgumentException("Z coordinate out of valid range");
        }
        
        if (location.getWorld() == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
    }
    
    /**
     * Validates a report ID
     * @param reportId The report ID to validate
     * @return The validated report ID
     * @throws IllegalArgumentException if validation fails
     */
    public static int validateReportId(String reportIdStr) {
        try {
            int reportId = Integer.parseInt(reportIdStr);
            if (reportId <= 0) {
                throw new IllegalArgumentException("Report ID must be a positive number");
            }
            return reportId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Report ID must be a valid number");
        }
    }
    
    /**
     * Checks for dangerous content patterns
     * @param text The text to check
     * @return true if dangerous content is found
     */
    private static boolean containsDangerousContent(String text) {
        // Check for SQL injection patterns
        String lowerText = text.toLowerCase();
        String[] sqlPatterns = {"drop table", "delete from", "update set", "insert into", 
                               "union select", "script>", "<iframe", "javascript:", "data:"};
        
        for (String pattern : sqlPatterns) {
            if (lowerText.contains(pattern)) {
                return true;
            }
        }
        
        // Check for excessive special characters (potential encoding attacks)
        long specialCharCount = text.chars()
                .filter(ch -> !Character.isLetterOrDigit(ch) && !Character.isWhitespace(ch))
                .count();
        
        return specialCharCount > text.length() * 0.3; // More than 30% special chars
    }
    
    /**
     * Sanitizes text by removing potentially dangerous characters
     * @param text The text to sanitize
     * @return The sanitized text
     */
    private static String sanitizeText(String text) {
        // Remove null bytes and other control characters except newlines and tabs
        return text.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
    }
}