package uk.co.extraspecialstudio.esl.util;

/**
 * Tiny null-safe helpers. Keep this package small — not a kitchen sink.
 */
public final class EslStrings {

    private EslStrings() {
    }

    public static boolean isBlank(String s) {
        if (s == null) return true;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) return false;
        }
        return true;
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }
}
