package uk.co.extraspecialstudio.esl.lobby;

import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

/**
 * Forge events fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} during lobby lifecycle.
 * Consuming mods subscribe to these to sync UI / networking without polling.
 */
public abstract class EslLobbyEvent extends Event {

    private final EslLobby lobby;

    protected EslLobbyEvent(EslLobby lobby) {
        this.lobby = lobby;
    }

    public EslLobby lobby() { return lobby; }

    /** Fired when a lobby is created (host already present as READY). */
    public static class Created extends EslLobbyEvent {
        public Created(EslLobby lobby) { super(lobby); }
    }

    /** Fired when an invite is sent (member status INVITED). */
    public static class InviteSent extends EslLobbyEvent {
        private final UUID invitee;

        public InviteSent(EslLobby lobby, UUID invitee) {
            super(lobby);
            this.invitee = invitee;
        }

        public UUID invitee() { return invitee; }
    }

    /** Fired when an invitee accepts and becomes JOINED. */
    public static class Joined extends EslLobbyEvent {
        private final UUID member;

        public Joined(EslLobby lobby, UUID member) {
            super(lobby);
            this.member = member;
        }

        public UUID member() { return member; }
    }

    /** Fired when an accepted member leaves voluntarily. */
    public static class Left extends EslLobbyEvent {
        private final UUID member;

        public Left(EslLobby lobby, UUID member) {
            super(lobby);
            this.member = member;
        }

        public UUID member() { return member; }
    }

    /** Fired when the host kicks a member (invited or accepted). */
    public static class Kicked extends EslLobbyEvent {
        private final UUID member;

        public Kicked(EslLobby lobby, UUID member) {
            super(lobby);
            this.member = member;
        }

        public UUID member() { return member; }
    }

    /** Fired when a member's ready status changes to READY. */
    public static class ReadyChanged extends EslLobbyEvent {
        private final UUID member;
        private final boolean ready;

        public ReadyChanged(EslLobby lobby, UUID member, boolean ready) {
            super(lobby);
            this.member = member;
            this.ready = ready;
        }

        public UUID member() { return member; }
        public boolean ready() { return ready; }
    }

    /** Fired when {@link EslLobby#canStart()} becomes true after a ready change. */
    public static class AllReady extends EslLobbyEvent {
        public AllReady(EslLobby lobby) { super(lobby); }
    }

    /** Fired when the lobby enters COUNTDOWN (start was accepted). */
    public static class Starting extends EslLobbyEvent {
        public Starting(EslLobby lobby) { super(lobby); }
    }

    /** Fired when the lobby is closed (cancel, host leave, world unload, etc.). */
    public static class Closed extends EslLobbyEvent {
        public Closed(EslLobby lobby) { super(lobby); }
    }
}
