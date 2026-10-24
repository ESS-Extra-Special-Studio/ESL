package uk.co.extraspecialstudio.esl.forceload;

import net.minecraft.util.registry.Registry;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * One force-loaded chunk claim site. Consumers (Dead Air bases, etc.) own meaning;
 * ESL only persists and applies the chunk ticket.
 */
public final class EslForceLoadSite {
    public final String modId;
    public final String siteId;
    public final RegistryKey<World> dimension;
    public final int chunkX;
    public final int chunkZ;

    public EslForceLoadSite(String modId, String siteId, RegistryKey<World> dimension, int chunkX, int chunkZ) {
        this.modId = modId;
        this.siteId = siteId;
        this.dimension = dimension;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public String key() {
        return modId + "|" + siteId;
    }

    public CompoundNBT save() {
        CompoundNBT tag = new CompoundNBT();
        tag.putString("modId", modId);
        tag.putString("siteId", siteId);
        tag.putString("dim", dimension.location().toString());
        tag.putInt("cx", chunkX);
        tag.putInt("cz", chunkZ);
        return tag;
    }

    @Nullable
    public static EslForceLoadSite load(CompoundNBT tag) {
        if (tag == null || !tag.contains("modId") || !tag.contains("siteId") || !tag.contains("dim")) {
            return null;
        }
        ResourceLocation dimId = ResourceLocation.tryParse(tag.getString("dim"));
        if (dimId == null) return null;
        RegistryKey<World> dim = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimId);
        return new EslForceLoadSite(tag.getString("modId"), tag.getString("siteId"), dim,
            tag.getInt("cx"), tag.getInt("cz"));
    }
}
