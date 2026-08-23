package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.eventbus.api.Event;

/**
 * Forge events fired on {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} during wave lifecycle.
 * Consuming mods subscribe to these to react to wave progress without polling.
 */
public abstract class EslWaveEvent extends Event {

    private final EslWaveContext context;

    protected EslWaveEvent(EslWaveContext context) {
        this.context = context;
    }

    public EslWaveContext context() { return context; }

    /** Fired when a wave sequence begins (before the first wave spawns). */
    public static class SequenceStart extends EslWaveEvent {
        public SequenceStart(EslWaveContext ctx) { super(ctx); }
    }

    /** Fired when an individual wave starts spawning. */
    public static class WaveStart extends EslWaveEvent {
        private final int waveIndex;
        private final EslWaveDefinition definition;

        public WaveStart(EslWaveContext ctx, int waveIndex, EslWaveDefinition definition) {
            super(ctx);
            this.waveIndex = waveIndex;
            this.definition = definition;
        }

        public int waveIndex() { return waveIndex; }
        public EslWaveDefinition definition() { return definition; }
    }

    /** Fired when all enemies in a single wave are killed (before the next wave or sequence completion). */
    public static class WaveComplete extends EslWaveEvent {
        private final int waveIndex;

        public WaveComplete(EslWaveContext ctx, int waveIndex) {
            super(ctx);
            this.waveIndex = waveIndex;
        }

        public int waveIndex() { return waveIndex; }
    }

    /** Fired when all waves in the sequence are cleared successfully. */
    public static class SequenceComplete extends EslWaveEvent {
        public SequenceComplete(EslWaveContext ctx) { super(ctx); }
    }

    /** Fired when the sequence is aborted or a wave times out with enemies still alive. */
    public static class SequenceFailed extends EslWaveEvent {
        private final Reason reason;

        public SequenceFailed(EslWaveContext ctx, Reason reason) {
            super(ctx);
            this.reason = reason;
        }

        public Reason reason() { return reason; }

        public enum Reason { TIMEOUT, CANCELLED, WORLD_UNLOAD }
    }

    /** Fired for each entity spawned by the wave controller. The mob can be modified. */
    public static class EntitySpawned extends EslWaveEvent {
        private final Mob entity;

        public EntitySpawned(EslWaveContext ctx, Mob entity) {
            super(ctx);
            this.entity = entity;
        }

        public Mob entity() { return entity; }
    }

    /** Fired when a tracked wave entity dies. */
    public static class EntityKilled extends EslWaveEvent {
        private final Mob entity;
        private final int remaining;

        public EntityKilled(EslWaveContext ctx, Mob entity, int remaining) {
            super(ctx);
            this.entity = entity;
            this.remaining = remaining;
        }

        public Mob entity() { return entity; }
        public int remaining() { return remaining; }
    }
}
