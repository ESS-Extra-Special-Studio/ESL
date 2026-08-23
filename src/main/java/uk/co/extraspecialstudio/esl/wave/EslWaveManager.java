package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry of active {@link EslWaveController} instances.
 * Registers itself on the NeoForge event bus to tick controllers and clean up on world unload.
 */
public final class EslWaveManager {

    private static final Map<ResourceKey<Level>, List<EslWaveController>> CONTROLLERS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private EslWaveManager() {}

    public static synchronized void init() {
        if (!registered) {
            NeoForge.EVENT_BUS.register(EslWaveManager.class);
            registered = true;
            EslMod.LOGGER.info("[ESL] Wave manager initialized");
        }
    }

    public static EslWaveController start(ServerLevel level, BlockPos center,
                                           EslWaveSequence sequence, @Nullable ServerPlayer player) {
        init();
        EslWaveController controller = new EslWaveController(level, center, sequence, player);
        CONTROLLERS.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(controller);
        controller.start();
        EslMod.LOGGER.info("[ESL] Wave sequence started at {} in {} ({} waves)",
                center, level.dimension().location(), sequence.totalWaves());
        return controller;
    }

    public static List<EslWaveController> activeControllers() {
        List<EslWaveController> result = new ArrayList<>();
        for (List<EslWaveController> list : CONTROLLERS.values()) {
            for (EslWaveController c : list) {
                if (c.state() != EslWaveController.State.COMPLETED
                    && c.state() != EslWaveController.State.FAILED) {
                    result.add(c);
                }
            }
        }
        return result;
    }

    public static List<EslWaveController> activeControllersIn(ResourceKey<Level> dimension) {
        List<EslWaveController> list = CONTROLLERS.get(dimension);
        if (list == null) return Collections.emptyList();
        List<EslWaveController> result = new ArrayList<>();
        for (EslWaveController c : list) {
            if (c.state() != EslWaveController.State.COMPLETED
                && c.state() != EslWaveController.State.FAILED) {
                result.add(c);
            }
        }
        return result;
    }

    public static @Nullable EslWaveController findById(UUID controllerId) {
        for (List<EslWaveController> list : CONTROLLERS.values()) {
            for (EslWaveController c : list) {
                if (c.id().equals(controllerId)) return c;
            }
        }
        return null;
    }

    public static void cancel(EslWaveController controller) {
        controller.cancel();
    }

    public static void cancelAll() {
        for (List<EslWaveController> list : CONTROLLERS.values()) {
            for (EslWaveController c : list) {
                if (c.state() != EslWaveController.State.COMPLETED
                    && c.state() != EslWaveController.State.FAILED) {
                    c.cancel();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        for (Map.Entry<ResourceKey<Level>, List<EslWaveController>> entry : CONTROLLERS.entrySet()) {
            Iterator<EslWaveController> it = entry.getValue().iterator();
            while (it.hasNext()) {
                EslWaveController c = it.next();
                if (c.state() == EslWaveController.State.COMPLETED
                    || c.state() == EslWaveController.State.FAILED) {
                    it.remove();
                    continue;
                }
                c.tick();
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ResourceKey<Level> dim = serverLevel.dimension();
        List<EslWaveController> list = CONTROLLERS.remove(dim);
        if (list != null) {
            for (EslWaveController c : list) {
                c.onWorldUnload();
            }
        }
    }
}
