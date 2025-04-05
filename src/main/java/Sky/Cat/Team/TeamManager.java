package Sky.Cat.Team;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TeamManager{
    // The singleton instance of TeamManager.
    private static TeamManager ManagerInstance;

    // Mapping of all existing teams.
    private Map<UUID, Team> teams;

    // Mapping between player's uuid and team uuid.
    private Map<UUID, UUID> playerTeamMap;

    private static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk");

    private static final String SAVE_FILE_NAME = "claim_that_chunk_teams.dat";

    private MinecraftServer server;

    private TeamManager() {
        this.teams = new ConcurrentHashMap<>();
        this.playerTeamMap = new ConcurrentHashMap<>();
    }

    public static TeamManager getInstance() {
        if (ManagerInstance == null) {
            ManagerInstance = new TeamManager();
        }
        return ManagerInstance;
    }

    // Permission codec
    public static final Codec<Permission> PERMISSION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("invite").forGetter(p -> p.hasPermission(PermType.INVITE)),
            Codec.BOOL.fieldOf("kick").forGetter(p -> p.hasPermission(PermType.KICK)),
            Codec.BOOL.fieldOf("claim").forGetter(p -> p.hasPermission(PermType.CLAIM)),
            Codec.BOOL.fieldOf("build").forGetter(p -> p.hasPermission(PermType.BUILD)),
            Codec.BOOL.fieldOf("break").forGetter(p -> p.hasPermission(PermType.BREAK)),
            Codec.BOOL.fieldOf("interact").forGetter(p -> p.hasPermission(PermType.INTERACT)),
            Codec.BOOL.fieldOf("modifyPermission").forGetter(p -> p.hasPermission(PermType.MODIFY_PERMISSION)),
            Codec.BOOL.fieldOf("disband").forGetter(p -> p.hasPermission(PermType.DISBAND))
    ).apply(instance, (invite, kick, claim, build, brk, interact, modifyPerm, disband) -> {
        Permission permission = new Permission();

        // Apply values to new permission instance.
        permission.setPermission(PermType.INVITE, invite);
        permission.setPermission(PermType.KICK, kick);
        permission.setPermission(PermType.CLAIM, claim);
        permission.setPermission(PermType.BUILD, build);
        permission.setPermission(PermType.BREAK, brk);
        permission.setPermission(PermType.INTERACT, interact);
        permission.setPermission(PermType.MODIFY_PERMISSION, modifyPerm);
        permission.setPermission(PermType.DISBAND, disband);

        return permission;
    }));

    // Team member codec.
    public static final Codec<TeamMember> TEAM_MEMBER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("uuid").forGetter(TeamMember::getPlayerUUID),
            Codec.STRING.fieldOf("name").forGetter(TeamMember::getPlayerName),
            PERMISSION_CODEC.fieldOf("permission").forGetter(TeamMember::getPermission),
            Codec.LONG.fieldOf("joinTime").forGetter(TeamMember::getTeamJoinTime)
    ).apply(instance, TeamMember::fromPersistence));

    // Team codec.
    public static final Codec<Team> TEAM_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("teamId").forGetter(Team::getTeamId),
            Codec.STRING.fieldOf("teamName").forGetter(Team::getTeamName),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("leaderUUID").forGetter(Team::getLeaderUUID),
            Codec.STRING.fieldOf("leaderName").forGetter(Team::getLeaderName),
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    TEAM_MEMBER_CODEC
            ).fieldOf("teamMembers").forGetter(Team::getTeamMember),
            PERMISSION_CODEC.fieldOf("defaultPermission").forGetter(Team::getDefaultPermission),
            Codec.INT.fieldOf("teamSizeLimit").forGetter(Team::getTeamSizeLimit),
            Codec.LONG.fieldOf("createTime").forGetter(Team::getCreateTime)
    ).apply(instance, Team::fromCodec));


    // Team mapping coded.
    public static final Codec<Map<UUID, Team>> TEAMS_CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            TEAM_CODEC
    );

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public Team createTeam(UUID creatorId, String creatorName, String teamName) {
        if (playerTeamMap.containsKey(creatorId)) {
            return null; // Cannot create team when is already in one.
        }

        Team newTeam = new Team(creatorId, creatorName, teamName);

        teams.put(newTeam.getTeamId(), newTeam);
        playerTeamMap.put(creatorId, newTeam.getTeamId());

        saveTeams();

        return newTeam;
    }

    public boolean disbandTeam(UUID teamId) {
        Team disbandTeam = teams.get(teamId);

        if (disbandTeam == null) return false;

        for (UUID playerId : disbandTeam.getTeamMember().keySet()) {
            playerTeamMap.remove(playerId);
        }

        teams.remove(teamId);

        saveTeams();

        return true;
    }

    /**
     * Get a team via its uuid.
     * @param teamId targeted team's uuid.
     * @return The team or null if such a team doesn't exist.
     */
    public Team getTeamById(UUID teamId) {
        return teams.get(teamId);
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
        return teams.get(teamId);
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

        Team team = teams.get(teamId);
        if (team == null) return false; // There is no such team in data.

        boolean result = team.addMember(playerUUID, playerName);

        if (result) {
            playerTeamMap.put(playerUUID, teamId);
            saveTeams();
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

        Team team = teams.get(teamId);

        // This condition is impossible to happen unless data is tampered outside the game's management,
        // (e.g., manually edited by user.)
        if (team == null) {
            playerTeamMap.remove(playerUUID);
            return false;
        }

        boolean result = team.removeMember(playerUUID);

        if (result) {
            playerTeamMap.remove(playerUUID);
            saveTeams();
        }

        return result;
    }

    /***
     * Rebuild player-team mapping from teams data.
     */
    private void rebuildPlayerTeamMap() {
        playerTeamMap.clear();

        for (Map.Entry<UUID, Team> entry : teams.entrySet()) {
            UUID teamId = entry.getKey();
            Team team = entry.getValue();

            for (UUID playerUUID : team.getTeamMember().keySet()) {
                playerTeamMap.put(playerUUID, teamId);
            }
        }
    }

    public void saveTeams() {
        if (server == null) {
            LOGGER.error("Cannot save teams: server is null.");
            return;
        }

        try {
            Path saveDir = server.getSavePath(WorldSavePath.ROOT).resolve("data");
            Files.createDirectories(saveDir);
            File saveFile = saveDir.resolve(SAVE_FILE_NAME).toFile();

            NbtCompound rootTag = new NbtCompound();

            NbtCompound teamsCompound = new NbtCompound();

            for (Map.Entry<UUID, Team> entry : teams.entrySet()) {
                String uuidString = entry.getKey().toString();
                Team team = entry.getValue();

                DataResult<NbtElement> result = TEAM_CODEC.encodeStart(NbtOps.INSTANCE, team);
                Optional<NbtElement> encoded = result.resultOrPartial(error -> LOGGER.error("Failed to encode teams: {}", error));

                encoded.ifPresent(nbtElement -> teamsCompound.put(uuidString, nbtElement));
            }

            rootTag.put("teams", teamsCompound);

            NbtIo.writeCompressed(rootTag, saveFile.toPath());

            LOGGER.info("Saved team data ({} teams)", teams.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save team data", e);
        }
    }

    public void loadTeams() {
        if (server == null) {
            LOGGER.error("Cannot load teams: server is null.");
            return;
        }

        try {
            Path saveDir = server.getSavePath(WorldSavePath.ROOT).resolve("data");
            File saveFile = saveDir.resolve(SAVE_FILE_NAME).toFile();

            if (!saveFile.exists()) {
                LOGGER.info("No team data file found, starting with empty data");
                return;
            }

            NbtCompound rootTag = NbtIo.readCompressed(saveFile.toPath(), NbtSizeTracker.ofUnlimitedBytes());

            if (rootTag.contains("teams")) {
                NbtCompound teamsCompound = rootTag.getCompoundOrEmpty("teams");
                teams.clear();

                for (String key : teamsCompound.getKeys()) {
                    try {
                        UUID teamId = UUID.fromString(key);
                        NbtElement teamElement = teamsCompound.get(key);

                        if (teamElement != null) {
                            DataResult<Team> result = TEAM_CODEC.parse(NbtOps.INSTANCE, teamElement);
                            Optional<Team> decodedTeam = result.resultOrPartial(error ->
                                    LOGGER.error("Failed to decode team {}: {}", key, error));

                            decodedTeam.ifPresent(team -> teams.put(teamId, team));
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.error("Invalid team UUID: {}", key);
                    }
                    rebuildPlayerTeamMap();
                    LOGGER.info("Loaded {} teams", teams.size());
                }
            } else {
                LOGGER.error("Failed to decode teams");
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load team data", e);
        }
    }
}
