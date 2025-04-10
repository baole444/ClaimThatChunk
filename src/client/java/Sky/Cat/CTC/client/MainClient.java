package Sky.Cat.CTC.client;

import Sky.Cat.CTC.client.chunk.ChunkClientData;
import Sky.Cat.CTC.client.networking.ChunkClientNetworking;
import Sky.Cat.CTC.client.team.TeamClientData;
import Sky.Cat.CTC.client.networking.TeamClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class MainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client's network handlers.
        TeamClientNetworking.register();
        ChunkClientNetworking.register();

        // Request data when joining the server.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Request team data
            TeamClientNetworking.requestTeamData();

            // Request chunk data
            ChunkClientNetworking.requestChunkData();
        });

        // Clear client data when leaving the server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TeamClientData.getInstance().clearTeamData();
            ChunkClientData.getInstance().clearChunkData();
        });
    }
}
