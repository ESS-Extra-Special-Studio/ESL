package uk.co.extraspecialstudio.esl.forceload;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

/**
 * One force-loaded chunk claim site. Consumers (Dead Air bases, etc.) own meaning;
 * ESL only persists and applies the chunk ticket.
 */
public final class EslForceLoadSite {
    public static final Codec<EslForceLoadSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("modId").forGetter(site -> site.modId),
            Codec.STRING.fieldOf("siteId").forGetter(site -> site.siteId),
            ResourceLocation.CODEC.fieldOf("dim").forGetter(site -> site.dimension.location()),
            Codec.INT.fieldOf("cx").forGetter(site -> site.chunkX),
            Codec.INT.fieldOf("cz").forGetter(site -> site.chunkZ)
    ).apply(instance, (modId, siteId, dim, cx, cz) -> new EslForceLoadSite(
            modId, siteId, ResourceKey.create(Registries.DIMENSION, dim), cx, cz)));

    public final String modId;
    public final String siteId;
    public final ResourceKey<Level> dimension;
    public final int chunkX;
    public final int chunkZ;

    public EslForceLoadSite(String modId, String siteId, ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        this.modId = modId;
        this.siteId = siteId;
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public String key() {
        return modId + "|" + siteId;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("modId", modId);
        tag.putString("siteId", siteId);
        tag.putString("dim", dimension.location().toString());
        tag.putInt("cx", chunkX);
        tag.putInt("cz", chunkZ);
        return tag;
    }

    @Nullable
    public static EslForceLoadSite load(CompoundTag tag) {
        if (tag == null || !tag.contains("modId") || !tag.contains("siteId") || !tag.contains("dim")) {
            return null;
        }
        ResourceLocation dimId = ResourceLocation.tryParse(tag.getStringOr("dim", ""));
        if (dimId == null) return null;
        ResourceKey<Level> dim = ResourceKey.create(Registries.DIMENSION, dimId);
        return new EslForceLoadSite(tag.getStringOr("modId", ""), tag.getStringOr("siteId", ""), dim,
            tag.getIntOr("cx", 0), tag.getIntOr("cz", 0));
    }
}
