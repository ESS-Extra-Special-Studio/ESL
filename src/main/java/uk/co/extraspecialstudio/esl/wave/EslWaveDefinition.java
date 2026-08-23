package uk.co.extraspecialstudio.esl.wave;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable description of a single wave: what to spawn, how to detect completion, and timeout.
 */
public final class EslWaveDefinition {

    public enum CompletionCondition { ALL_KILLED, TIMER, CUSTOM }

    private final List<EslSpawnEntry> entries;
    private final CompletionCondition condition;
    private final int timeoutTicks;
    private final @Nullable String displayName;

    private EslWaveDefinition(Builder b) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(b.entries));
        this.condition = b.condition;
        this.timeoutTicks = b.timeoutTicks;
        this.displayName = b.displayName;
    }

    public List<EslSpawnEntry> entries() { return entries; }
    public CompletionCondition condition() { return condition; }
    public int timeoutTicks() { return timeoutTicks; }
    public @Nullable String displayName() { return displayName; }

    /** Total number of entities this wave will spawn across all entries. */
    public int totalSpawnCount() {
        int total = 0;
        for (EslSpawnEntry e : entries) total += e.count();
        return total;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private final List<EslSpawnEntry> entries = new ArrayList<>();
        private CompletionCondition condition = CompletionCondition.ALL_KILLED;
        private int timeoutTicks = 3 * 60 * 20; // 3 minutes default
        private @Nullable String displayName;

        private Builder() {}

        public Builder spawn(EslSpawnEntry entry) { entries.add(entry); return this; }
        public Builder spawn(EslSpawnEntry.Builder entry) { entries.add(entry.build()); return this; }
        public Builder condition(CompletionCondition c) { this.condition = c; return this; }
        public Builder timeout(int ticks) { this.timeoutTicks = ticks; return this; }
        public Builder displayName(String name) { this.displayName = name; return this; }

        public EslWaveDefinition build() {
            if (entries.isEmpty()) throw new IllegalStateException("EslWaveDefinition requires at least one spawn entry");
            return new EslWaveDefinition(this);
        }
    }
}
