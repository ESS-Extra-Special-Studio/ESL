package uk.co.extraspecialstudio.esl.util;

/**
 * Tiny null-safe helpers. Keep this package small — not a kitchen sink.
 */
public final class EslStrings {

    private EslStrings() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String emptyToNull(String s) {
        return isBlank(s) ? null : s;
    }
}
