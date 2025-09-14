package dev.msntech.msnreports.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class ChatUtils {
    private static final String PREFIX = "§8[§6MSNReports§8] ";
    
    public static Component getPrefix() {
        return Component.text(PREFIX);
    }
    
    public static Component createClickableCommand(String text, String command, String hoverText) {
        return Component.text(text)
                .color(NamedTextColor.YELLOW)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hoverText)
                        .color(NamedTextColor.GRAY)));
    }
    
    public static Component createButton(String text, String command, NamedTextColor color) {
        return Component.text("[ ")
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(text)
                        .color(color)
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to " + text.toLowerCase())
                                .color(NamedTextColor.GRAY))))
                .append(Component.text(" ]")
                        .color(NamedTextColor.DARK_GRAY));
    }

    public static Component createHeader(String text) {
        return Component.text("════════ ")
                .color(NamedTextColor.DARK_GRAY)
                .append(Component.text(text)
                        .color(NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD))
                .append(Component.text(" ════════")
                        .color(NamedTextColor.DARK_GRAY));
    }

    public static Component createInfoLine(String label, String value) {
        return Component.text(label + ": ")
                .color(NamedTextColor.GRAY)
                .append(Component.text(value)
                        .color(NamedTextColor.WHITE));
    }
    
    public static Component createStatusBadge(String status) {
        NamedTextColor color = switch(status.toUpperCase()) {
            case "OPEN" -> NamedTextColor.GREEN;
            case "IN_PROGRESS" -> NamedTextColor.YELLOW;
            case "RESOLVED" -> NamedTextColor.AQUA;
            case "REJECTED" -> NamedTextColor.RED;
            default -> NamedTextColor.GRAY;
        };
        
        return Component.text("• " + status)
                .color(color)
                .decorate(TextDecoration.BOLD);
    }
}