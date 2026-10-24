package uk.co.extraspecialstudio.esl;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.co.extraspecialstudio.esl.forceload.EslForceLoadRegistry;
import uk.co.extraspecialstudio.esl.lobby.EslLobbyManager;
import uk.co.extraspecialstudio.esl.wave.EslWaveManager;

/**
 * ES Library — backend foundation for the Extra Special Studio stack.
 * No GUI, rendering, or screen-loop code belongs here.
 */
@Mod(EslMod.MOD_ID)
public final class EslMod {
    public static final String MOD_ID = "extraspeciallib";
    public static final Logger LOGGER = LogManager.getLogger();

    public EslMod() {
        EslLobbyManager.init();
        EslWaveManager.init();
        EslForceLoadRegistry.init();
        LOGGER.info("ES Library (ESL) {} loaded", MOD_ID);
    }
}
