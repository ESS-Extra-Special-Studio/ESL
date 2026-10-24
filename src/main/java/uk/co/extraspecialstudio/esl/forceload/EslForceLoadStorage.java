package uk.co.extraspecialstudio.esl.forceload;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persists {@link EslForceLoadSite} entries on the overworld data storage. */
@SuppressWarnings("null")
public final class EslForceLoadStorage extends WorldSavedData {
    private static final String DATA_NAME = EslMod.MOD_ID + "_forceload";
    /** 1.16.5 has no {@code Tag.TAG_COMPOUND} constant; compound lists use id 10. */
    private static final int TAG_COMPOUND = 10;
    private final Map<String, EslForceLoadSite> sites = new LinkedHashMap<String, EslForceLoadSite>();

    public EslForceLoadStorage() {
        super(DATA_NAME);
    }

    @Override
    public void load(CompoundNBT nbt) {
        sites.clear();
        ListNBT list = nbt.getList("sites", TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            EslForceLoadSite site = EslForceLoadSite.load(list.getCompound(i));
            if (site != null) sites.put(site.key(), site);
        }
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        ListNBT list = new ListNBT();
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
        ServerWorld overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(EslForceLoadStorage::new, DATA_NAME);
    }
}
