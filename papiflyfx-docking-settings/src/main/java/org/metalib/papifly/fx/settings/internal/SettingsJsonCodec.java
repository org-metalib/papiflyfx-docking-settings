package org.metalib.papifly.fx.settings.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SettingsJsonCodec {

    public String toJson(Map<String, Object> map) {
        return toJsonValue(map, 0);
    }

    public Map<String, Object> fromJson(String json) {
        return new Parser(json).parseObject();
    }

    @SuppressWarnings("unchecked")
    private String toJsonValue(Object value, int indent) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String string) {
            return "\"" + escape(string) + "\"";
        }
        if (value instanceof Number number) {
            return number.toString();
        }
        if (value instanceof Boolean bool) {
            return bool.toString();
        }
        if (value instanceof Map<?, ?> map) {
            return toJsonObject((Map<String, Object>) map, indent);
        }
        if (value instanceof List<?> list) {
            return toJsonArray((List<Object>) list, indent);
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    private String toJsonObject(Map<String, Object> map, int indent) {
        if (map.isEmpty()) {
            return "{}";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        String itemIndent = "  ".repeat(indent + 1);
        String closingIndent = "  ".repeat(indent);
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            builder.append(itemIndent)
                .append("\"")
                .append(escape(entry.getKey()))
                .append("\": ")
                .append(toJsonValue(entry.getValue(), indent + 1));
            if (iterator.hasNext()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(closingIndent).append('}');
        return builder.toString();
    }

    private String toJsonArray(List<Object> list, int indent) {
        if (list.isEmpty()) {
            return "[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");
        String itemIndent = "  ".repeat(indent + 1);
        String closingIndent = "  ".repeat(indent);
        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            builder.append(itemIndent).append(toJsonValue(iterator.next(), indent + 1));
            if (iterator.hasNext()) {
                builder.append(',');
            }
            builder.append('\n');
        }
        builder.append(closingIndent).append(']');
        return builder.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private static final class Parser {
        private final String json;
        private int position;

        private Parser(String json) {
            this.json = json == null ? "" : json.trim();
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> result = new LinkedHashMap<>();
            expect('{');
            skipWhitespace();
            if (peek() != '}') {
                do {
                    String key = parseString();
                    skipWhitespace();
                    expect(':');
                    skipWhitespace();
                    result.put(key, parseValue());
                    skipWhitespace();
                } while (consume(','));
            }
            expect('}');
            return result;
        }

        private List<Object> parseArray() {
            List<Object> result = new ArrayList<>();
            expect('[');
            skipWhitespace();
            if (peek() != ']') {
                do {
                    result.add(parseValue());
                    skipWhitespace();
                } while (consume(','));
            }
            expect(']');
            return result;
        }

        private Object parseValue() {
            skipWhitespace();
            char next = peek();
            return switch (next) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't', 'f' -> parseBoolean();
                case 'n' -> parseNull();
                default -> parseNumber();
            };
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (position < json.length()) {
                char ch = json.charAt(position++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    char escaped = json.charAt(position++);
                    switch (escaped) {
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalStateException("Unterminated JSON string");
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", position)) {
                position += 4;
                return Boolean.TRUE;
            }
            if (json.startsWith("false", position)) {
                position += 5;
                return Boolean.FALSE;
            }
            throw new IllegalStateException("Invalid boolean at position " + position);
        }

        private Object parseNull() {
            if (!json.startsWith("null", position)) {
                throw new IllegalStateException("Invalid null at position " + position);
            }
            position += 4;
            return null;
        }

        private Number parseNumber() {
            int start = position;
            while (position < json.length() && isNumberChar(json.charAt(position))) {
                position++;
            }
            String token = json.substring(start, position);
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        }

        private boolean isNumberChar(char ch) {
            return ch >= '0' && ch <= '9' || ch == '.' || ch == '-' || ch == '+' || ch == 'e' || ch == 'E';
        }

        private char peek() {
            skipWhitespace();
            return position < json.length() ? json.charAt(position) : '\0';
        }

        private boolean consume(char expected) {
            skipWhitespace();
            if (position < json.length() && json.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (position >= json.length() || json.charAt(position) != expected) {
                throw new IllegalStateException("Expected '" + expected + "' at position " + position);
            }
            position++;
        }

        private void skipWhitespace() {
            while (position < json.length() && Character.isWhitespace(json.charAt(position))) {
                position++;
            }
        }
    }
}
