package uk.co.extraspecialstudio.esl.lobby;

import java.util.Objects;
import java.util.UUID;

/** One participant in an {@link EslLobby} — invited, accepted, or ready. */
public final class EslLobbyMember {

    public enum Status { INVITED, JOINED, READY }

    private final UUID uuid;
    private String displayName;
    private Status status;
    private long invitedAtGameTime;

    public EslLobbyMember(UUID uuid, String displayName, Status status, long invitedAtGameTime) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.displayName = displayName != null ? displayName : uuid.toString();
        this.status = Objects.requireNonNull(status, "status");
        this.invitedAtGameTime = invitedAtGameTime;
    }

    public UUID uuid() { return uuid; }
    public String displayName() { return displayName; }
    public Status status() { return status; }
    public long invitedAtGameTime() { return invitedAtGameTime; }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName;
        }
    }

    public void setStatus(Status status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void setInvitedAtGameTime(long invitedAtGameTime) {
        this.invitedAtGameTime = invitedAtGameTime;
    }

    public boolean isAccepted() {
        return status == Status.JOINED || status == Status.READY;
    }
}
