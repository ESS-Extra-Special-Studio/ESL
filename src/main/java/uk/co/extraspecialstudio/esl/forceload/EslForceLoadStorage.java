package uk.co.extraspecialstudio.esl.forceload;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import uk.co.extraspecialstudio.esl.EslMod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists {@link EslForceLoadSite} entries on the server data storage. */
@SuppressWarnings("null")
public final class EslForceLoadStorage extends SavedData {
    public static final Codec<EslForceLoadStorage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EslForceLoadSite.CODEC.listOf().optionalFieldOf("sites", List.of()).forGetter(EslForceLoadStorage::sitesList)
    ).apply(instance, EslForceLoadStorage::new));

    public static final SavedDataType<EslForceLoadStorage> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(EslMod.MOD_ID, "forceload"),
            EslForceLoadStorage::new,
            CODEC);

    private final Map<String, EslForceLoadSite> sites = new LinkedHashMap<>();

    public EslForceLoadStorage() {}

    public EslForceLoadStorage(List<EslForceLoadSite> loaded) {
        for (EslForceLoadSite site : loaded) {
            if (site != null) sites.put(site.key(), site);
        }
    }

    private List<EslForceLoadSite> sitesList() {
        return List.copyOf(sites.values());
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
        return server.getDataStorage().computeIfAbsent(TYPE);
    }
}
