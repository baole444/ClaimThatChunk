package Sky.Cat.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.mojang.serialization.DataResult;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamManager{
    // The singleton instance of TeamManager.
    private static TeamManager ManagerInstance;

    // Mapping between player's uuid and team uuid.
    private Map<UUID, UUID> playerTeamMap;

    // Logger
    private static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk");

    // Reference for persistent state
    private TeamState teamState;

    private MinecraftServer server;

    // Private constructor for singleton
    private TeamManager() {
        this.playerTeamMap = new ConcurrentHashMap<>();
    }

    /**
     * Get or create the singleton instance of TeamManager.
     * @return the TeamManager instance.
     */
    public static TeamManager getInstance() {
        if (ManagerInstance == null) {
            ManagerInstance = new TeamManager();
        }
        return ManagerInstance;
    }

    /**
     * Set the server instance for TeamManager instance.
     * @param server the current minecraft server instance.
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
        if (server != null) {
            this.teamState = TeamState.getOrCreate(server);
            rebuildPlayerTeamMap();
        }
    }

    /**
     * Create a new team with the creator as the leader.
     * @param creatorId the uuid of the player that created the team.
     * @param creatorName the name of the player that created the team.
     * @param teamName the name of the team.
     * @return the newly created team or null if player already in a team.
     */
    public Team createTeam(UUID creatorId, String creatorName, String teamName) {
        if (playerTeamMap.containsKey(creatorId)) {
            return null; // Cannot create team when is already in one.
        }

        Team newTeam = new Team(creatorId, creatorName, teamName);

        teamState.addTeam(newTeam);

        playerTeamMap.put(creatorId, newTeam.getTeamId());

        return newTeam;
    }

    /**
     * Disband a team and remove all its members.
     * @param teamId the uuid of the team to disband.
     * @return true on disbanding successfully, false if no team founded.
     */
    public boolean disbandTeam(UUID teamId) {
        Team disbandTeam = getTeamById(teamId);

        if (disbandTeam == null) return false;

        for (UUID playerId : disbandTeam.getTeamMember().keySet()) {
            playerTeamMap.remove(playerId);
        }

        return teamState.removeTeam(teamId);
    }

    /**
     * Get a team via its uuid.
     * @param teamId targeted team's uuid.
     * @return The team or null if such a team doesn't exist.
     */
    public Team getTeamById(UUID teamId) {
        return teamState.getTeams().get(teamId);
    }

    /**
     * Get a team via a player's uuid.
     * @param playerUUID the player's uuid
     * @return The team the player belong to or null if the player doesn't belong to any team.
     */
    public Team getTeamByPlayer(UUID playerUUID) {
        UUID teamId = playerTeamMap.get(playerUUID);
        if (teamId == null) {
            return null;
        }
        return getTeamById(teamId);
    }

    /***
     * Add a player to a team.
     * @param teamId targeted team to add the player to.
     * @param playerUUID uuid of the player that will be added.
     * @param playerName name of the player.
     * @return true when added successfully. If the team doesn't exist or player already belongs to a team then false.
     */
    public boolean addPlayerToTeam(UUID teamId, UUID playerUUID, String playerName) {
        if (playerTeamMap.containsKey(playerUUID)) return false; // Player already belong to a team

        Team team = getTeamById(teamId);
        if (team == null) return false; // There is no such team in data.

        boolean result = team.addMember(playerUUID, playerName);

        if (result) {
            playerTeamMap.put(playerUUID, teamId);
            teamState.markDirty();
        }
        return result;
    }

    /**
     * Remove a player from a team.
     * @param playerUUID uuid of the player that will be removed.
     * @return true when remove successfully.
     * If the team doesn't exist, return false.
     * Forcefully removed if the team they are in doesn't exist in data.
     */
    public boolean removePlayerFromTeam(UUID playerUUID) {
        UUID teamId = playerTeamMap.get(playerUUID);

        if (teamId == null) return false; // There is no team to remove from.

        Team team = getTeamById(teamId);

        // This condition is impossible to happen unless data is tampered outside the game's management,
        // (e.g., manually edited by user.)
        if (team == null) {
            playerTeamMap.remove(playerUUID);
            return false;
        }

        boolean result = team.removeMember(playerUUID);

        if (result) {
            playerTeamMap.remove(playerUUID);
            teamState.markDirty();
        }

        return result;
    }

    /***
     * Rebuild player-team mapping from teams data.
     */
    private void rebuildPlayerTeamMap() {
        playerTeamMap.clear();

        for (Map.Entry<UUID, Team> entry : teamState.getTeams().entrySet()) {
            UUID teamId = entry.getKey();
            Team team = entry.getValue();

            for (UUID playerUUID : team.getTeamMember().keySet()) {
                playerTeamMap.put(playerUUID, teamId);
            }
        }

        LOGGER.info("Rebuilt player-team map with {} player mappings", playerTeamMap.size());
    }

    /**
     * Load teams from persistent storage.
     * Called by server lifecycle event.
     */
    public void loadTeams() {
        if (server == null) {
            LOGGER.error("Cannot load team: server is null");
            return;
        }

        this.teamState = TeamState.getOrCreate(server);
        rebuildPlayerTeamMap();

        LOGGER.info("Loaded {} teams from storage", teamState.getTeams().size());
    }

    // For backward compatibility
    public void saveTeams() {
        LOGGER.info("Team saving is done automatically");
    }



}
