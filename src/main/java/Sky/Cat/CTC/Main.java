package Sky.Cat.CTC;

import Sky.Cat.Team.TeamManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk");

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ClaimThatChunk mod.");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Server starting - initializing TeamManager.");
            TeamManager manager = TeamManager.getInstance();
            manager.setServer(server);
            manager.loadTeams();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            LOGGER.info("Server stopping - saving TeamManager data.");
            TeamManager.getInstance().saveTeams();
        });

        LOGGER.info("ClaimThatChunk initialization completed.");
    }
}
