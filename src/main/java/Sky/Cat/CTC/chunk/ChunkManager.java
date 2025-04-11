package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.Main;
import Sky.Cat.CTC.networking.ChunkNetworking;
import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private static ChunkManager ManagerInstance;

    public static final Logger LOGGER = LoggerFactory.getLogger("ClaimThatChunk/ChunkManager");

    private ChunkState chunkState;

    // Memory cache mapping for claimed chunk.
    private final Map<ChunkPosition, ClaimedChunk> claimedChunkMap;

    // Limit on how much chunk a team can claim.
    // In the future, load this as a setting from config.
    private int maxChunksPerTeam = 64;

    private ChunkManager() {
        this.claimedChunkMap = new ConcurrentHashMap<>();
    }

    public static ChunkManager getInstance() {
        if (ManagerInstance == null) {
            ManagerInstance = new ChunkManager();
            LOGGER.info("Chunk Manager initialization completed");
        }

        return ManagerInstance;
    }

    /**
     * Claim a chunk for a team.
     * @param position position of the chunk to claim.
     * @param teamId uuid of the team that will own this chunk.
     * @param playerEntity the player that executed the command.
     * @return true if claim successfully. If the team doesn't exist,
     * the chunk is claimed, or the player doesn't belong to a team or lacks claim permission, return false.
     */
    public boolean claimChunk(ChunkPosition position, UUID teamId, ServerPlayerEntity playerEntity) {
        // A team already claimed this chunk
        if (isChunkClaimed(position)) return false;

        Team team = TeamManager.getInstance().getTeamById(teamId);

        // The team doesn't exist.
        if (team == null) return false;

        // Player is not in a team or has no permission to claim.
        if (!team.getTeamMember().containsKey(playerEntity.getUuid())
                || !team.getTeamMember().get(playerEntity.getUuid())
                .getPermission().hasPermission(PermType.CLAIM)
        ) return false;

        // Exceeding chunk limit per team.
        if (maxChunksPerTeam > 0 && getTeamChunkCount(teamId) >= maxChunksPerTeam) return false;

        ClaimedChunk claimedChunk = new ClaimedChunk(position, teamId);

        chunkState.addClaimedChunk(claimedChunk);
        claimedChunkMap.put(position, claimedChunk);

        ChunkNetworking.broadcastChunkClaimed(claimedChunk);

        LOGGER.info("Chunk at {} claimed by {} for team '{}'", position, playerEntity.getName().getLiteralString(), team.getTeamName());

        return true;
    }

    /**
     * Unclaim a chunk for a team.
     * @param position position of the chunk to un-claim.
     * @param playerEntity the player that executed the command.
     * @return true if unclaim successfully. If the team doesn't exist or if the player lacks claim permission, return false.
     * Forcefully remove the chunk from the claimed state if its team doesn't exist and return false (require the player to have admin permission.)
     */
    public boolean unclaimChunk(ChunkPosition position, ServerPlayerEntity playerEntity) {
        ClaimedChunk chunk = getClaimedChunk(position);

        // The chunk is not claimed.
        if (chunk == null) return false;

        Team team = TeamManager.getInstance().getTeamById(chunk.getOwnerTeamId());

        // Ghost claimed chunk, remove it from the claimed state using admin permission.
        if (team == null) {
            if (playerEntity.hasPermissionLevel(2)) {
                return forceUnclaimChunk(position);
            }

            return false;
        }

        UUID playerUUID = playerEntity.getUuid();

        // The player lacking claim permission or is not in a team.
        if (!playerUUID.equals(team.getLeaderUUID()) &&
                (!team.getTeamMember().containsKey(playerUUID)
                        || !team.getTeamMember().get(playerUUID).getPermission().hasPermission(PermType.CLAIM))
        ) return false;

        // Remove from persistent state.
        chunkState.removeClaimedChunk(position);
        ClaimedChunk removed = claimedChunkMap.remove(position);

        // Sync with the client.
        if (removed != null) {
            ChunkNetworking.broadcastChunkUnclaimed(position);
            LOGGER.info("Chunk at {} unclaimed by {}", position, playerEntity.getName().toString());
            return true;
        }

        return false;
    }

    /**
     * Forcefully unclaim a chunk (admin only.)
     */
    public boolean forceUnclaimChunk(ChunkPosition position) {
        ClaimedChunk removed = claimedChunkMap.remove(position);
        boolean success = chunkState.removeClaimedChunk(position);

        if (success) {
            ChunkNetworking.broadcastChunkUnclaimed(position);
            LOGGER.info("Chunk at {} forcibly unclaimed", position);
        }

        return success;
    }

    /**
     * Unclaim all chunks owned by a team.
     * @param teamId uuid of the team to unclaim all the chunks from.
     * @return number of chunks unclaimed.
     */
    public int unclaimAllTeamChunks(UUID teamId) {
        int count = 0;
        List<ChunkPosition> tobeRemove = new ArrayList<>();

        // Find all chunks owned by the team
        for (ClaimedChunk chunk : claimedChunkMap.values()) {
            if (chunk.getOwnerTeamId().equals(teamId)) {
                tobeRemove.add(chunk.getPosition());
                count++;
            }
        }

        // Remove all those chunks.
        for (ChunkPosition pos : tobeRemove) {
            chunkState.removeClaimedChunk(pos);
            claimedChunkMap.remove(pos);
            ChunkNetworking.broadcastChunkUnclaimed(pos);
        }

        if (count > 0) {
            LOGGER.info("Unclaimed {} chunks for team {}", count, teamId);
        }

        return count;
    }

    /**
     * Check if a chunk is claimed or not.
     */
    public boolean isChunkClaimed(ChunkPosition position) {
        return claimedChunkMap.containsKey(position);
    }

    /**
     * Count the number of chunks the team has claimed.
     */
    public int getTeamChunkCount(UUID teamId) {
        int count = 0;
        for (ClaimedChunk chunk : claimedChunkMap.values()) {
            if (chunk.getOwnerTeamId().equals(teamId))  {
                count++;
            }
        }

        return count;
    }

    /**
     * Get a claimed chunk at a position.
     */
    public ClaimedChunk getClaimedChunk(ChunkPosition position) {
        return claimedChunkMap.get(position);
    }

    /**
     * Get a claimed chunk at a block position.
     */
    public ClaimedChunk getClaimedChunkAt(BlockPos pos, RegistryKey<World> dimension) {
        ChunkPosition chunkPosition = new ChunkPosition(pos, dimension);
        return getClaimedChunk(chunkPosition);
    }

    /**
     * Check if a player has permission to perform an action at a position.
     */
    public boolean hasPermission(PlayerEntity player, BlockPos pos, RegistryKey<World> dimension, PermType permType) {
        // player is admin
        if (player.hasPermissionLevel(2)) return true;

        ClaimedChunk chunk = getClaimedChunkAt(pos, dimension);

        // unclaimed chunk has no permission.
        if (chunk == null) return true;

        return chunk.hasPermission(player.getUuid(), permType);
    }

    /**
     * Get all chunks claimed by a team.
     */
    public List<ClaimedChunk> getTeamChunks(UUID teamId) {
        List<ClaimedChunk> result = new ArrayList<>();

        for (ClaimedChunk chunk : claimedChunkMap.values()) {
            if (chunk.getOwnerTeamId().equals(teamId)) {
                result.add(chunk);
            }
        }

        return result;
    }

    /**
     * Load claimed chunks from persistent storage.
     */
    public void loadChunks() {
        if (Main.getServer() == null) {
            LOGGER.error("Cannot load chunks: server is null");
            return;
        }

        this.chunkState = ChunkState.getOrCreate(Main.getServer());

        claimedChunkMap.clear();
        claimedChunkMap.putAll(chunkState.getAllClaimedChunks());

        LOGGER.info("Loaded {} claimed chunks from storage", claimedChunkMap.size());
    }

    /**
     * Get the max number of chunk a team can claim.
     */
    public int getMaxChunksPerTeam() {
        return maxChunksPerTeam;
    }

    /**
     * Set the max number of chunk a team can claim.
     */
    public void setMaxChunksPerTeam(int maxChunksPerTeam) {
        this.maxChunksPerTeam = maxChunksPerTeam;
    }

    /**
     * Get current claimed chunk stored in memory.
     */
    public Map<ChunkPosition, ClaimedChunk> getAllClaimedChunks() {
        return new HashMap<>(claimedChunkMap);
    }

    /**
     * Send chunk data to a player when they join.
     */
    public void sendChunkDataToPlayer(ServerPlayerEntity player) {
        ChunkNetworking.sendAllClaimedChunksToPlayer(player, claimedChunkMap.values());
    }
}