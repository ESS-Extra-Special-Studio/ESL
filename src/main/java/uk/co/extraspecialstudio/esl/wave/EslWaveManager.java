package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry of active {@link EslWaveController} instances.
 * Registers itself on the Forge event bus to tick controllers and clean up on world unload.
 */
public final class EslWaveManager {

    private static final Map<RegistryKey<World>, List<EslWaveController>> CONTROLLERS = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private EslWaveManager() {}

    /** Ensure event handlers are registered. Safe to call multiple times. */
    public static synchronized void init() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(EslWaveManager.class);
            registered = true;
            EslMod.LOGGER.info("[ESL] Wave manager initialized");
        }
    }

    /**
     * Start a new wave sequence.
     *
     * @param level    the server level where mobs will spawn
     * @param center   the center position for ring spawning
     * @param sequence the wave sequence definition
     * @param player   the player who triggered this (nullable; used for aggro targeting callbacks)
     * @return the running controller
     */
    public static EslWaveController start(ServerWorld level, BlockPos center,
                                           EslWaveSequence sequence, @Nullable ServerPlayerEntity player) {
        init();
        EslWaveController controller = new EslWaveController(level, center, sequence, player);
        CONTROLLERS.computeIfAbsent(level.dimension(), k -> new ArrayList<>()).add(controller);
        controller.start();
        EslMod.LOGGER.info("[ESL] Wave sequence started at {} in {} ({} waves)",
                center, level.dimension().location(), sequence.totalWaves());
        return controller;
    }

    /** All currently active (non-terminal) controllers across all dimensions. */
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

    /** All active controllers in a specific dimension. */
    public static List<EslWaveController> activeControllersIn(RegistryKey<World> dimension) {
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

    /** Find a controller by its UUID. */
    public static @Nullable EslWaveController findById(UUID controllerId) {
        for (List<EslWaveController> list : CONTROLLERS.values()) {
            for (EslWaveController c : list) {
                if (c.id().equals(controllerId)) return c;
            }
        }
        return null;
    }

    /** Cancel a specific controller. */
    public static void cancel(EslWaveController controller) {
        controller.cancel();
    }

    /** Cancel all active controllers (e.g. on server shutdown). */
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

    // ---- Event handlers ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        for (Map.Entry<RegistryKey<World>, List<EslWaveController>> entry : CONTROLLERS.entrySet()) {
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
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        ServerWorld serverLevel = (ServerWorld) event.getWorld();
        RegistryKey<World> dim = serverLevel.dimension();
        List<EslWaveController> list = CONTROLLERS.remove(dim);
        if (list != null) {
            for (EslWaveController c : list) {
                c.onWorldUnload();
            }
        }
    }
}
