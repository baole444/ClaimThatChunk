package Sky.Cat.CTC.client;

import Sky.Cat.CTC.client.team.TeamClientData;
import Sky.Cat.CTC.client.networking.TeamClientNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class MainClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Register client's network handlers.
        TeamClientNetworking.register();

        // Request team data when joining the server.
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Request team data from the server
            TeamClientNetworking.requestTeamData();
        });

        // Clear client team data when leaving the server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            TeamClientData.getInstance().clearTeamData();
        });
    }
}
