package me.f1nal.trinity.execution.compile;

import imgui.ImGui;
import me.f1nal.trinity.decompiler.output.colors.ColoredString;
import me.f1nal.trinity.theme.CodeColorScheme;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Console {
    private final List<ColoredString> logs = new ArrayList<>();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy/HH:mm");

    public void draw() {
        synchronized (logs) {
            for (ColoredString log : logs) {
                ImGui.textColored(log.getColor(), log.getText());
            }
        }
    }

    private void addLog(int color, String info) {
        synchronized (logs) {
            this.logs.add(0, new ColoredString(info, color));
        }
    }

    public void clear() {
        synchronized (logs) {
            logs.clear();
        }
    }

    public void error(String info, String... args) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String formattedMessage = String.format("[%s] (ERROR) %s", timestamp, fmt(info, args));
        this.addLog(CodeColorScheme.NOTIFY_ERROR, formattedMessage);
    }

    public void warn(String info, String... args) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String formattedMessage = String.format("[%s] (WARN) %s", timestamp, fmt(info, args));
        this.addLog(CodeColorScheme.NOTIFY_WARN, formattedMessage);
    }

    public void info(String info, String... args) {
        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        String formattedMessage = String.format("[%s] (INFO) %s", timestamp, fmt(info, args));
        this.addLog(CodeColorScheme.TEXT, formattedMessage);
    }

    private static String fmt(String format, String[] args) {
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < format.length()) {
            if (format.charAt(i) == '{' && i + 1 < format.length() && format.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex]);
                    argIndex++;
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(format.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}