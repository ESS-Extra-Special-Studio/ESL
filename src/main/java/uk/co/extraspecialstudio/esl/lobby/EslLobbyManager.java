package uk.co.extraspecialstudio.esl.lobby;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static registry of active {@link EslLobby} instances.
 * Registers itself on the Forge event bus to tick invite timeouts and clean up on world unload.
 */
public final class EslLobbyManager {

    private static final Map<UUID, EslLobby> LOBBIES = new ConcurrentHashMap<>();
    private static boolean registered = false;

    private EslLobbyManager() {}

    /** Ensure event handlers are registered. Safe to call multiple times. */
    public static synchronized void init() {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(EslLobbyManager.class);
            registered = true;
            EslMod.LOGGER.info("[ESL] Lobby manager initialized");
        }
    }

    /**
     * Create a new lobby with the host already present as READY.
     *
     * @param level           server level (dimension + game-time source)
     * @param anchor          world anchor (e.g. tower / panel block)
     * @param hostUuid        host player id
     * @param hostDisplayName host name for member list
     * @param maxSize         max members including host (must be >= 1)
     */
    public static EslLobby create(ServerLevel level, BlockPos anchor,
                                  UUID hostUuid, String hostDisplayName, int maxSize) {
        init();
        EslLobby lobby = new EslLobby(level, anchor, hostUuid, hostDisplayName, maxSize);
        LOBBIES.put(lobby.id(), lobby);
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Created(lobby));
        EslMod.LOGGER.info("[ESL] Lobby {} created by {} at {} in {}",
                lobby.id(), hostUuid, anchor, level.dimension().location());
        return lobby;
    }

    public static @Nullable EslLobby find(UUID lobbyId) {
        return LOBBIES.get(lobbyId);
    }

    /** First non-closed lobby that contains this player (any status). */
    public static @Nullable EslLobby findByMember(UUID playerUuid) {
        for (EslLobby lobby : LOBBIES.values()) {
            if (lobby.state() != EslLobby.State.CLOSED && lobby.getMember(playerUuid) != null) {
                return lobby;
            }
        }
        return null;
    }

    /** All non-closed lobbies in a dimension. */
    public static List<EslLobby> activeIn(ResourceKey<Level> dimension) {
        List<EslLobby> result = new ArrayList<>();
        for (EslLobby lobby : LOBBIES.values()) {
            if (lobby.state() != EslLobby.State.CLOSED && lobby.dimension().equals(dimension)) {
                result.add(lobby);
            }
        }
        return result;
    }

    /** Close and unregister a lobby. */
    public static void cancel(EslLobby lobby) {
        lobby.close();
        LOBBIES.remove(lobby.id(), lobby);
    }

    /** Close all active lobbies (e.g. server stop). */
    public static void cancelAll() {
        for (EslLobby lobby : new ArrayList<>(LOBBIES.values())) {
            cancel(lobby);
        }
    }

    // ---- Event handlers ----

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, EslLobby>> it = LOBBIES.entrySet().iterator();
        while (it.hasNext()) {
            EslLobby lobby = it.next().getValue();
            if (lobby.state() == EslLobby.State.CLOSED) {
                it.remove();
                continue;
            }
            ServerLevel level = server.getLevel(lobby.dimension());
            if (level != null) {
                lobby.tickInviteTimeouts(level.getGameTime());
            }
        }
    }

    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        ResourceKey<Level> dim = serverLevel.dimension();
        for (EslLobby lobby : new ArrayList<>(LOBBIES.values())) {
            if (lobby.dimension().equals(dim)) {
                lobby.onWorldUnload();
                LOBBIES.remove(lobby.id(), lobby);
            }
        }
    }
}
