package uk.co.extraspecialstudio.esl.lobby;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side lobby / party session. No packets or GUI — consumers sync via {@link EslLobbyEvent}.
 */
public final class EslLobby {

    public enum State { OPEN, READY_CHECK, COUNTDOWN, IN_PROGRESS, CLOSED }

    public static final int DEFAULT_INVITE_TIMEOUT_TICKS = 6000;

    private final UUID id = UUID.randomUUID();
    private final UUID hostUuid;
    private final ServerLevel level;
    private final BlockPos anchor;
    private final int maxSize;
    private final Map<UUID, EslLobbyMember> members = new LinkedHashMap<>();

    private State state = State.OPEN;
    private int inviteTimeoutTicks = DEFAULT_INVITE_TIMEOUT_TICKS;

    EslLobby(ServerLevel level, BlockPos anchor, UUID hostUuid, String hostDisplayName, int maxSize) {
        if (maxSize < 1) throw new IllegalArgumentException("maxSize must be >= 1");
        this.level = Objects.requireNonNull(level, "level");
        this.anchor = Objects.requireNonNull(anchor, "anchor").immutable();
        this.hostUuid = Objects.requireNonNull(hostUuid, "hostUuid");
        this.maxSize = maxSize;
        long gameTime = level.getGameTime();
        members.put(hostUuid, new EslLobbyMember(hostUuid, hostDisplayName, EslLobbyMember.Status.READY, gameTime));
    }

    public UUID id() { return id; }
    public UUID hostUuid() { return hostUuid; }
    public ServerLevel level() { return level; }
    public ResourceKey<Level> dimension() { return level.dimension(); }
    public BlockPos anchor() { return anchor; }
    public int maxSize() { return maxSize; }
    public State state() { return state; }
    public int inviteTimeoutTicks() { return inviteTimeoutTicks; }

    /** Per-lobby invite expiry in ticks (default {@link #DEFAULT_INVITE_TIMEOUT_TICKS}). */
    public void setInviteTimeoutTicks(int ticks) {
        this.inviteTimeoutTicks = Math.max(0, ticks);
    }

    /** Snapshot of current members (includes INVITED). */
    public Collection<EslLobbyMember> members() {
        return Collections.unmodifiableCollection(members.values());
    }

    public @Nullable EslLobbyMember getMember(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isFull() {
        return members.size() >= maxSize;
    }

    /**
     * Accepted members only ({@link EslLobbyMember.Status#JOINED} + {@link EslLobbyMember.Status#READY}).
     */
    public Set<UUID> memberUuids() {
        Set<UUID> result = new LinkedHashSet<>();
        for (EslLobbyMember m : members.values()) {
            if (m.status() == EslLobbyMember.Status.JOINED || m.status() == EslLobbyMember.Status.READY) {
                result.add(m.uuid());
            }
        }
        return result;
    }

    /** All member UUIDs including pending {@link EslLobbyMember.Status#INVITED}. */
    public Set<UUID> allMemberUuids() {
        return Collections.unmodifiableSet(members.keySet());
    }

    /**
     * True when there are no pending invites and every accepted member is READY.
     * Solo host (only member) is READY by creation, so this returns true for an empty party.
     */
    public boolean canStart() {
        if (state != State.OPEN && state != State.READY_CHECK) return false;
        if (members.isEmpty()) return false;
        for (EslLobbyMember m : members.values()) {
            if (m.status() == EslLobbyMember.Status.INVITED) return false;
            if (m.status() == EslLobbyMember.Status.JOINED) return false;
        }
        return true;
    }

    /**
     * Invite a player. Allowed only while {@link State#OPEN} or {@link State#READY_CHECK},
     * and only if the lobby is not full and the uuid is not already a member.
     */
    public boolean invite(UUID uuid, String displayName) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(displayName, "displayName");
        if (state != State.OPEN && state != State.READY_CHECK) return false;
        if (isFull() || members.containsKey(uuid)) return false;
        EslLobbyMember member = new EslLobbyMember(
                uuid, displayName, EslLobbyMember.Status.INVITED, level.getGameTime());
        members.put(uuid, member);
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.InviteSent(this, uuid));
        return true;
    }

    /** Remove a pending invite without firing kick/leave events. */
    public boolean cancelInvite(UUID uuid) {
        EslLobbyMember member = members.get(uuid);
        if (member == null || member.status() != EslLobbyMember.Status.INVITED) return false;
        if (uuid.equals(hostUuid)) return false;
        members.remove(uuid);
        return true;
    }

    /** Accept a pending invite → {@link EslLobbyMember.Status#JOINED}. */
    public boolean join(UUID uuid) {
        EslLobbyMember member = members.get(uuid);
        if (member == null || member.status() != EslLobbyMember.Status.INVITED) return false;
        if (state != State.OPEN && state != State.READY_CHECK) return false;
        member.setStatus(EslLobbyMember.Status.JOINED);
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Joined(this, uuid));
        return true;
    }

    /**
     * Mark an accepted member ready. Host is already READY at creation.
     * Transitions {@link State#OPEN} → {@link State#READY_CHECK} on first ready change.
     */
    public boolean setReady(UUID uuid) {
        EslLobbyMember member = members.get(uuid);
        if (member == null) return false;
        if (state != State.OPEN && state != State.READY_CHECK) return false;
        if (member.status() == EslLobbyMember.Status.INVITED) return false;
        if (member.status() == EslLobbyMember.Status.READY) return true;
        member.setStatus(EslLobbyMember.Status.READY);
        if (state == State.OPEN) state = State.READY_CHECK;
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.ReadyChanged(this, uuid, true));
        if (canStart()) {
            MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.AllReady(this));
        }
        return true;
    }

    /** Kick invited or accepted members (not the host). */
    public boolean kick(UUID uuid) {
        if (uuid.equals(hostUuid)) return false;
        EslLobbyMember member = members.remove(uuid);
        if (member == null) return false;
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Kicked(this, uuid));
        return true;
    }

    /**
     * Voluntary leave. Host leave closes the lobby.
     * Pending invitees may also leave (decline) — fires {@link EslLobbyEvent.Left}.
     */
    public boolean leave(UUID uuid) {
        EslLobbyMember member = members.get(uuid);
        if (member == null) return false;
        if (uuid.equals(hostUuid)) {
            close();
            return true;
        }
        members.remove(uuid);
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Left(this, uuid));
        return true;
    }

    /**
     * Begin countdown if {@link #canStart()}. Sets {@link State#COUNTDOWN} and fires {@link EslLobbyEvent.Starting}.
     */
    public boolean start() {
        if (!canStart()) return false;
        state = State.COUNTDOWN;
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Starting(this));
        return true;
    }

    /** Advance from countdown into the active run. */
    public void markInProgress() {
        if (state == State.COUNTDOWN) {
            state = State.IN_PROGRESS;
        }
    }

    /** Close the lobby. Idempotent. */
    public void close() {
        if (state == State.CLOSED) return;
        state = State.CLOSED;
        MinecraftForge.EVENT_BUS.post(new EslLobbyEvent.Closed(this));
    }

    /** Cancel stale {@link EslLobbyMember.Status#INVITED} entries past {@link #inviteTimeoutTicks}. */
    void tickInviteTimeouts(long gameTime) {
        if (state == State.CLOSED || inviteTimeoutTicks <= 0) return;
        List<UUID> expired = new ArrayList<>();
        for (EslLobbyMember m : members.values()) {
            if (m.status() == EslLobbyMember.Status.INVITED
                && gameTime - m.invitedAtGameTime() >= inviteTimeoutTicks) {
                expired.add(m.uuid());
            }
        }
        for (UUID uuid : expired) {
            cancelInvite(uuid);
        }
    }

    void onWorldUnload() {
        close();
    }
}
