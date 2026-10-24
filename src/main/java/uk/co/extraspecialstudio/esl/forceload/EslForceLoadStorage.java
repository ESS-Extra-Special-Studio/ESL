package uk.co.extraspecialstudio.esl.forceload;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persists {@link EslForceLoadSite} entries on the overworld data storage. */
@SuppressWarnings("null")
public final class EslForceLoadStorage extends SavedData {
    private static final String DATA_NAME = EslMod.MOD_ID + "_forceload";
    private final Map<String, EslForceLoadSite> sites = new LinkedHashMap<>();

    public static EslForceLoadStorage load(CompoundTag nbt) {
        EslForceLoadStorage data = new EslForceLoadStorage();
        ListTag list = nbt.getList("sites", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            EslForceLoadSite site = EslForceLoadSite.load(list.getCompound(i));
            if (site != null) data.sites.put(site.key(), site);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        ListTag list = new ListTag();
        for (EslForceLoadSite site : sites.values()) {
            list.add(site.save());
        }
        nbt.put("sites", list);
        return nbt;
    }

    public Collection<EslForceLoadSite> all() {
        return sites.values();
    }

    @Nullable
    public EslForceLoadSite get(String modId, String siteId) {
        return sites.get(modId + "|" + siteId);
    }

    public void put(EslForceLoadSite site) {
        sites.put(site.key(), site);
        setDirty();
    }

    @Nullable
    public EslForceLoadSite remove(String modId, String siteId) {
        EslForceLoadSite removed = sites.remove(modId + "|" + siteId);
        if (removed != null) setDirty();
        return removed;
    }

    public static EslForceLoadStorage get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(EslForceLoadStorage::new, EslForceLoadStorage::load, null),
            DATA_NAME);
    }
}
