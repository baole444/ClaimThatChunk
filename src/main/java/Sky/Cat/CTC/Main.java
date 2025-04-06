package Sky.Cat.CTC;

import Sky.Cat.CTC.Team.TeamManager;
import Sky.Cat.CTC.networking.TeamNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk");
    public static final String MOD_ID = "claim_that_chunk";
    private static MinecraftServer server;

    public static MinecraftServer getServer() {
        return server;
    }

    public static void setServer(MinecraftServer server) {
        Main.server = server;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initiali");

        // Register server lifecycle events for initializing TeamManager.
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Initializing Team Manager...");
            setServer(server);
            TeamManager.getInstance().initiateTeamState(server);
        });

        // Register network handlers.
        TeamNetworking.register();

        // Register player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // When the player joins, send them their team data if they are in a team.
            TeamManager.getInstance().sendTeamDataToPlayer(handler.player);
        });

        LOGGER.info("initialization completed");
    }


}
