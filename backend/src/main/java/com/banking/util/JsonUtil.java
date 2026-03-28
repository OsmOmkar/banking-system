package com.banking.util;

// =============================================
// SYLLABUS: Unit II - Static methods, Unit IV - String manipulation
// Simple JSON builder (no external libraries = pure Java)
// =============================================
public class JsonUtil {

    public static String success(String dataJson) {
        return "{\"success\":true,\"data\":" + dataJson + "}";
    }

    public static String error(String message) {
        return "{\"success\":false,\"error\":\"" + escapeJson(message) + "\"}";
    }

    public static String field(String key, String value) {
        return "\"" + key + "\":\"" + escapeJson(value) + "\"";
    }

    public static String field(String key, double value) {
        return "\"" + key + "\":" + value;
    }

    public static String field(String key, int value) {
        return "\"" + key + "\":" + value;
    }

    public static String field(String key, boolean value) {
        return "\"" + key + "\":" + value;
    }

    // SYLLABUS: Unit IV - String manipulation (StringBuilder)
    public static String object(String... fields) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String array(java.util.List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i));
            if (i < items.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
