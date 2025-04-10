package Sky.Cat.CTC;

import Sky.Cat.CTC.chunk.ChunkEventHandlers;
import Sky.Cat.CTC.chunk.ChunkManager;
import Sky.Cat.CTC.command.TeamCommand;
import Sky.Cat.CTC.networking.ChunkNetworking;
import Sky.Cat.CTC.team.TeamManager;
import Sky.Cat.CTC.networking.TeamNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
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
        LOGGER.info("Initializing...");

        // Register server lifecycle events for initializing TeamManager.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            setServer(server);

            // Load team data
            TeamManager.getInstance().loadTeams();

            // Load chunk data
            ChunkManager.getInstance().loadChunks();
        });

        // Register network handlers.
        TeamNetworking.register();
        ChunkNetworking.register();

        // Register event handlers for chunk interactions.
        ChunkEventHandlers.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TeamCommand.register(dispatcher);
        });

        // Register player connection events
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Send team data
            TeamManager.getInstance().sendTeamDataToPlayer(handler.player);

            // Send chunk data
            ChunkManager.getInstance().sendChunkDataToPlayer(handler.player);
        });

        LOGGER.info("initialization completed");
    }


}
