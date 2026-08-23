package uk.co.extraspecialstudio.esl.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Small helpers for grouped Forge configs. Structure only — no key opinions.
 */
public final class EslConfig {

    private EslConfig() {
    }

    public static ForgeConfigSpec.Builder begin(String rootComment) {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        if (rootComment != null && !rootComment.isEmpty()) {
            builder.comment(rootComment);
        }
        return builder;
    }

    public static ForgeConfigSpec.Builder push(ForgeConfigSpec.Builder builder, String section, String... commentLines) {
        if (commentLines != null && commentLines.length > 0) {
            builder.comment(commentLines);
        }
        return builder.push(section);
    }

    public static void pop(ForgeConfigSpec.Builder builder) {
        builder.pop();
    }
}
