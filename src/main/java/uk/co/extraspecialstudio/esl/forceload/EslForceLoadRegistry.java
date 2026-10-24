package uk.co.extraspecialstudio.esl.forceload;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Reusable force-chunk-load registry (claim-mod style).
 * No GUI / packets — consumers supply modId + siteId and a chunk.
 */
public final class EslForceLoadRegistry {
    private static boolean registered;

    private EslForceLoadRegistry() {}

    public static synchronized void init() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(EslForceLoadRegistry.class);
            registered = true;
            EslMod.LOGGER.info("[ESL] Force-load registry initialized");
        }
    }

    /** Force-load the chunk containing {@code pos}. Replaces any prior site with the same modId+siteId. */
    public static void set(MinecraftServer server, String modId, String siteId,
                           RegistryKey<World> dimension, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        set(server, modId, siteId, dimension, chunk.x, chunk.z);
    }

    public static void set(MinecraftServer server, String modId, String siteId,
                           RegistryKey<World> dimension, int chunkX, int chunkZ) {
        init();
        EslForceLoadStorage storage = EslForceLoadStorage.get(server);
        EslForceLoadSite previous = storage.get(modId, siteId);
        if (previous != null) {
            applyForced(server, previous, false);
        }
        EslForceLoadSite site = new EslForceLoadSite(modId, siteId, dimension, chunkX, chunkZ);
        storage.put(site);
        applyForced(server, site, true);
    }

    public static void clear(MinecraftServer server, String modId, String siteId) {
        init();
        EslForceLoadSite removed = EslForceLoadStorage.get(server).remove(modId, siteId);
        if (removed != null) {
            applyForced(server, removed, false);
        }
    }

    @Nullable
    public static EslForceLoadSite get(MinecraftServer server, String modId, String siteId) {
        return EslForceLoadStorage.get(server).get(modId, siteId);
    }

    public static Collection<EslForceLoadSite> all(MinecraftServer server) {
        return Collections.unmodifiableList(new ArrayList<EslForceLoadSite>(EslForceLoadStorage.get(server).all()));
    }

    public static Collection<EslForceLoadSite> allForMod(MinecraftServer server, String modId) {
        List<EslForceLoadSite> out = new ArrayList<>();
        for (EslForceLoadSite site : EslForceLoadStorage.get(server).all()) {
            if (site.modId.equals(modId)) out.add(site);
        }
        return out;
    }

    private static void applyForced(MinecraftServer server, EslForceLoadSite site, boolean forced) {
        ServerWorld level = server.getLevel(site.dimension);
        if (level == null) return;
        level.setChunkForced(site.chunkX, site.chunkZ, forced);
    }

    private static void reapplyAll(MinecraftServer server) {
        for (EslForceLoadSite site : EslForceLoadStorage.get(server).all()) {
            applyForced(server, site, true);
        }
        EslMod.LOGGER.info("[ESL] Re-applied {} force-load site(s)", EslForceLoadStorage.get(server).all().size());
    }

    private static void releaseAllTickets(MinecraftServer server) {
        for (EslForceLoadSite site : EslForceLoadStorage.get(server).all()) {
            applyForced(server, site, false);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(FMLServerStartedEvent event) {
        reapplyAll(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(FMLServerStoppingEvent event) {
        // Clear live tickets; WorldSavedData keeps sites for next boot.
        releaseAllTickets(event.getServer());
    }

    /** Convenience when the current server is already running. */
    @Nullable
    public static MinecraftServer currentServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }
}
