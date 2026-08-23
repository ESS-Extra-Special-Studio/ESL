package uk.co.extraspecialstudio.esl.mod;

import net.neoforged.fml.ModList;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Safe mod-list helpers for optional integrations.
 */
public final class EslMods {

    private EslMods() {
    }

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Optional<String> getDisplayName(String modId) {
        return ModList.get().getModContainerById(modId).map(c -> c.getModInfo().getDisplayName());
    }

    public static void runIfLoaded(String modId, Runnable action) {
        if (isLoaded(modId) && action != null) {
            action.run();
        }
    }

    public static <T> void acceptIfLoaded(String modId, Consumer<T> consumer, T value) {
        if (isLoaded(modId) && consumer != null) {
            consumer.accept(value);
        }
    }
}
