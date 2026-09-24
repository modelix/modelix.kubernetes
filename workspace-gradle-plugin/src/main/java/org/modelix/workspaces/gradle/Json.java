package org.modelix.workspaces.gradle;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal JSON support. The plugin doesn't use a JSON library to avoid conflicts with the build it is applied to.
 */
final class Json {
    private Json() {
    }

    /**
     * Serializes a flat object. Entries with a null value are omitted.
     */
    static String toJson(Map<String, Object> values) {
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (entry.getValue() == null) continue;
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  ").append(quote(entry.getKey())).append(": ");
            Object value = entry.getValue();
            if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else {
                sb.append(quote(value.toString()));
            }
        }
        return sb.append("\n}\n").toString();
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    /**
     * Extracts the value of a top-level string property. Sufficient for the flat responses the plugin reads.
     */
    static String readStringProperty(String json, String name) {
        Matcher matcher = Pattern.compile(Pattern.quote(quote(name)) + "\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
        if (!matcher.find()) return null;
        return unescape(matcher.group(1));
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\' || i + 1 >= s.length()) {
                sb.append(c);
                continue;
            }
            char next = s.charAt(++i);
            switch (next) {
                case 'n': sb.append('\n'); break;
                case 'r': sb.append('\r'); break;
                case 't': sb.append('\t'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                case 'u':
                    sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                    i += 4;
                    break;
                default: sb.append(next);
            }
        }
        return sb.toString();
    }
}
