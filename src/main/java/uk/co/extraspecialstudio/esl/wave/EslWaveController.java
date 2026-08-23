package uk.co.extraspecialstudio.esl.wave;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;

/**
 * Runtime state machine that orchestrates a single {@link EslWaveSequence}.
 * <p>
 * One controller per active sequence. Created via {@link EslWaveManager#start}.
 * All methods are main-thread only (called from server tick).
 */
public final class EslWaveController {

    public enum State { IDLE, WAVE_ACTIVE, BETWEEN_WAVES, COMPLETED, FAILED }

    private static final int MIN_TICKS_BEFORE_COMPLETION = 200; // 10 seconds — prevent false early completion
    private static final int COMPLETION_DEBOUNCE_TICKS = 60;    // 3 seconds between completion checks

    private final UUID controllerId = UUID.randomUUID();
    private final ServerLevel level;
    private final BlockPos center;
    private final EslWaveSequence sequence;
    private final @Nullable ServerPlayer triggeringPlayer;

    private State state = State.IDLE;
    private int currentWaveIndex = -1;
    private final Set<UUID> trackedEntities = new HashSet<>();
    private final Set<UUID> deadEntities = new HashSet<>();

    private int waveAgeTicks = 0;
    private int betweenWaveTimer = 0;
    private int lastCompletionCheckTick = 0;
    private int spawnQueueIndex = 0;
    private int spawnDelayCounter = 0;

    // Flattened spawn list for the current wave
    private final List<SpawnJob> currentSpawnJobs = new ArrayList<>();

    EslWaveController(ServerLevel level, BlockPos center, EslWaveSequence sequence,
                      @Nullable ServerPlayer triggeringPlayer) {
        this.level = level;
        this.center = center;
        this.sequence = sequence;
        this.triggeringPlayer = triggeringPlayer;
    }

    public UUID id() { return controllerId; }
    public ServerLevel level() { return level; }
    public BlockPos center() { return center; }
    public EslWaveSequence sequence() { return sequence; }
    public State state() { return state; }
    public int currentWaveIndex() { return currentWaveIndex; }
    public int totalWaves() { return sequence.totalWaves(); }
    public @Nullable ServerPlayer triggeringPlayer() { return triggeringPlayer; }

    /** Number of tracked entities still alive in the current wave. */
    public int remainingEnemies() {
        return trackedEntities.size() - deadEntities.size();
    }

    /** Seconds remaining before the current wave times out, or -1 if not in a wave. */
    public int secondsRemaining() {
        if (state != State.WAVE_ACTIVE || currentWaveIndex < 0) return -1;
        EslWaveDefinition def = sequence.waves().get(currentWaveIndex);
        int remaining = def.timeoutTicks() - waveAgeTicks;
        return Math.max(0, remaining / 20);
    }

    EslWaveContext createContext() {
        return new EslWaveContext(this, level, center, triggeringPlayer);
    }

    /** Called by {@link EslWaveManager} to begin the sequence. */
    void start() {
        state = State.IDLE;
        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.SequenceStart(createContext()));
        advanceToNextWave();
    }

    /** Called every server tick by {@link EslWaveManager}. */
    void tick() {
        switch (state) {
            case WAVE_ACTIVE -> tickWaveActive();
            case BETWEEN_WAVES -> tickBetweenWaves();
            default -> {}
        }
    }

    /** Cancel the sequence, clean up all tracked mobs. */
    public void cancel() {
        if (state == State.COMPLETED || state == State.FAILED) return;
        cleanupTrackedEntities();
        state = State.FAILED;
        EslWaveContext ctx = createContext();
        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.SequenceFailed(ctx, EslWaveEvent.SequenceFailed.Reason.CANCELLED));
        invokeCallback(sequence.onFailed(), ctx);
    }

    void onWorldUnload() {
        if (state == State.COMPLETED || state == State.FAILED) return;
        cleanupTrackedEntities();
        state = State.FAILED;
        EslWaveContext ctx = createContext();
        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.SequenceFailed(ctx, EslWaveEvent.SequenceFailed.Reason.WORLD_UNLOAD));
        invokeCallback(sequence.onFailed(), ctx);
    }

    // ---- Tick logic ----

    private void tickWaveActive() {
        waveAgeTicks++;

        // Spawn queued mobs
        if (spawnQueueIndex < currentSpawnJobs.size()) {
            SpawnJob job = currentSpawnJobs.get(spawnQueueIndex);
            if (spawnDelayCounter >= job.delayTicks) {
                spawnMob(job);
                spawnQueueIndex++;
                spawnDelayCounter = 0;
            } else {
                spawnDelayCounter++;
            }
        }

        // Poll alive status of tracked entities
        pollDeadEntities();

        EslWaveDefinition def = sequence.waves().get(currentWaveIndex);

        // Timeout check
        if (waveAgeTicks >= def.timeoutTicks()) {
            if (remainingEnemies() > 0) {
                cleanupTrackedEntities();
                state = State.FAILED;
                EslWaveContext ctx = createContext();
                MinecraftForge.EVENT_BUS.post(new EslWaveEvent.SequenceFailed(ctx, EslWaveEvent.SequenceFailed.Reason.TIMEOUT));
                invokeCallback(sequence.onFailed(), ctx);
                return;
            }
        }

        // Completion check — only after all spawned + min age + debounce
        if (spawnQueueIndex >= currentSpawnJobs.size()
            && waveAgeTicks >= MIN_TICKS_BEFORE_COMPLETION
            && (waveAgeTicks - lastCompletionCheckTick) >= COMPLETION_DEBOUNCE_TICKS) {
            lastCompletionCheckTick = waveAgeTicks;
            if (remainingEnemies() <= 0) {
                onWaveCleared();
            }
        }
    }

    private void tickBetweenWaves() {
        betweenWaveTimer++;
        if (betweenWaveTimer >= sequence.delayBetweenWavesTicks()) {
            advanceToNextWave();
        }
    }

    private void finishSequence() {
        state = State.COMPLETED;
        EslWaveContext ctx = createContext();
        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.SequenceComplete(ctx));
        invokeCallback(sequence.onComplete(), ctx);
    }

    private void advanceToNextWave() {
        currentWaveIndex++;
        if (currentWaveIndex >= sequence.totalWaves()) {
            finishSequence();
            return;
        }

        state = State.WAVE_ACTIVE;
        waveAgeTicks = 0;
        lastCompletionCheckTick = 0;
        spawnQueueIndex = 0;
        spawnDelayCounter = 0;
        trackedEntities.clear();
        deadEntities.clear();
        currentSpawnJobs.clear();

        EslWaveDefinition def = sequence.waves().get(currentWaveIndex);
        buildSpawnQueue(def);

        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.WaveStart(createContext(), currentWaveIndex, def));
    }

    private void onWaveCleared() {
        EslWaveContext ctx = createContext();
        MinecraftForge.EVENT_BUS.post(new EslWaveEvent.WaveComplete(ctx, currentWaveIndex));
        invokeCallback(sequence.onWaveCleared(), ctx);

        // Last wave: deliver immediately. Do not wait delayBetween or pretend another wave is coming.
        if (currentWaveIndex >= sequence.totalWaves() - 1) {
            finishSequence();
            return;
        }

        state = State.BETWEEN_WAVES;
        betweenWaveTimer = 0;
    }

    // ---- Spawning ----

    private void buildSpawnQueue(EslWaveDefinition def) {
        for (EslSpawnEntry entry : def.entries()) {
            for (int i = 0; i < entry.count(); i++) {
                currentSpawnJobs.add(new SpawnJob(entry, i == 0 ? 0 : entry.spawnDelayTicks()));
            }
        }
    }

    private void spawnMob(SpawnJob job) {
        EslSpawnEntry entry = job.entry;
        Mob mob;
        if (entry.factory() != null) {
            mob = entry.factory().get();
            if (mob == null) return;
        } else if (entry.entityType() != null) {
            mob = entry.entityType().create(level);
            if (mob == null) return;
        } else {
            return;
        }

        BlockPos spawnPos = findRingSpawnPos(entry.radiusMin(), entry.radiusMax());
        mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                   level.random.nextFloat() * 360f, 0f);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.MOB_SUMMONED, null, null);

        if (entry.postSpawnModifier() != null) {
            entry.postSpawnModifier().accept(mob);
        }

        if (level.addFreshEntity(mob)) {
            trackedEntities.add(mob.getUUID());
            MinecraftForge.EVENT_BUS.post(new EslWaveEvent.EntitySpawned(createContext(), mob));
        }
    }

    private BlockPos findRingSpawnPos(double minR, double maxR) {
        double angle = level.random.nextDouble() * Math.PI * 2;
        double dist = minR + level.random.nextDouble() * (maxR - minR);
        int x = center.getX() + (int)(Math.cos(angle) * dist);
        int z = center.getZ() + (int)(Math.sin(angle) * dist);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, y, z);
    }

    // ---- Entity tracking ----

    private void pollDeadEntities() {
        for (UUID uuid : trackedEntities) {
            if (deadEntities.contains(uuid)) continue;
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive() || entity.isRemoved()) {
                deadEntities.add(uuid);
                Mob mob = (entity instanceof Mob m) ? m : null;
                int remaining = remainingEnemies();
                MinecraftForge.EVENT_BUS.post(new EslWaveEvent.EntityKilled(createContext(),
                        mob, remaining));
            }
        }
    }

    private void cleanupTrackedEntities() {
        for (UUID uuid : trackedEntities) {
            if (deadEntities.contains(uuid)) continue;
            Entity entity = level.getEntity(uuid);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        trackedEntities.clear();
        deadEntities.clear();
    }

    private static void invokeCallback(@Nullable Consumer<EslWaveContext> cb, EslWaveContext ctx) {
        if (cb != null) {
            try { cb.accept(ctx); } catch (Exception ignored) {}
        }
    }

    // ---- Internal ----

    private record SpawnJob(EslSpawnEntry entry, int delayTicks) {}
}
