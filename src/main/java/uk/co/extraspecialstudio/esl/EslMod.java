package uk.co.extraspecialstudio.esl;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * ES Library — backend foundation for the Extra Special Studio stack.
 * No GUI, rendering, or screen-loop code belongs here.
 */
@Mod(EslMod.MOD_ID)
public final class EslMod {
    public static final String MOD_ID = "extraspeciallib";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EslMod() {
        LOGGER.info("ES Library (ESL) {} loaded", MOD_ID);
    }
}
