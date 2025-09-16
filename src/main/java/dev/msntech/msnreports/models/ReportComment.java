package dev.msntech.msnreports.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReportComment {
    private final String author;
    private final String content;
    private final String timestamp;
    private final CommentType type;

    public enum CommentType {
        STAFF_NOTE("Staff Note", "📝"),
        STATUS_CHANGE("Status Change", "🔄"),
        ASSIGNMENT("Assignment", "👤"),
        RESOLUTION("Resolution", "✅");

        private final String display;
        private final String emoji;

        CommentType(String display, String emoji) {
            this.display = display;
            this.emoji = emoji;
        }

        public String getDisplay() {
            return display;
        }

        public String getEmoji() {
            return emoji;
        }
    }

    public ReportComment(String author, String content, CommentType type) {
        this.author = author;
        this.content = content;
        this.type = type;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters
    public String getAuthor() {
        return author;
    }

    public String getContent() {
        return content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public CommentType getType() {
        return type;
    }

    public String getFormattedComment() {
        return String.format("[%s] %s %s: %s", 
            timestamp, 
            type.getEmoji(), 
            author, 
            content
        );
    }
}