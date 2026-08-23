package uk.co.extraspecialstudio.esl.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Small helpers for grouped NeoForge configs. Structure only — no key opinions.
 */
public final class EslConfig {

    private EslConfig() {
    }

    public static ModConfigSpec.Builder begin(String rootComment) {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        if (rootComment != null && !rootComment.isEmpty()) {
            builder.comment(rootComment);
        }
        return builder;
    }

    public static ModConfigSpec.Builder push(ModConfigSpec.Builder builder, String section, String... commentLines) {
        if (commentLines != null && commentLines.length > 0) {
            builder.comment(commentLines);
        }
        return builder.push(section);
    }

    public static void pop(ModConfigSpec.Builder builder) {
        builder.pop();
    }
}
