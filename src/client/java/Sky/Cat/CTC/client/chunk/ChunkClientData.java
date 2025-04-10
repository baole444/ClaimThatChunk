package Sky.Cat.CTC.client.chunk;

import Sky.Cat.CTC.chunk.ChunkPosition;
import Sky.Cat.CTC.permission.Permission;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkClientData {
    private static ChunkClientData ChunkClientDataInstance;

    private final Map<ChunkPosition, ClientClaimedChunk> claimedChunks = new ConcurrentHashMap<>();

    private final Map<ChunkPosition, Map<UUID, Permission>> permissionOverrides = new ConcurrentHashMap<>();

    private ChunkClientData() {}

    public static ChunkClientData getInstance() {
        if (ChunkClientDataInstance == null) {
            ChunkClientDataInstance = new ChunkClientData();
        }

        return ChunkClientDataInstance;
    }

    /**
     * Add or update an existing claimed chunk.
     */
    public void addOrUpdateChunk(ChunkPosition position, UUID teamId, long claimedTime) {
        claimedChunks.put(position, new ClientClaimedChunk(position, teamId, claimedTime));
    }

    /**
     * Remove a claimed chunk.
     */
    public void removeChunk(ChunkPosition position) {
        claimedChunks.remove(position);
        permissionOverrides.remove(position);
    }

    /**
     * Update permissions override of a chunk for a player.
     */
    public void updatePermissionOverride(ChunkPosition position, UUID playerUUID, Permission permission) {
        permissionOverrides.computeIfAbsent(position, key -> new ConcurrentHashMap<>()).put(playerUUID, permission);
    }

    /**
     * Get a claimed chunk at a chunk position
     */
    public ClientClaimedChunk getChunkAt(ChunkPosition position) {
        return claimedChunks.get(position);
    }

    /**
     * Get a claimed chunk at a block position.
     */
    public ClientClaimedChunk getChunkAt(BlockPos pos, RegistryKey<World> dimension) {
        ChunkPosition chunkPosition = new ChunkPosition(pos, dimension);

        return getChunkAt(chunkPosition);
    }

    /**
     * Check if a chunk is claimed or not.
     */
    public boolean isChunkClaimed(ChunkPosition position) {
        return claimedChunks.containsKey(position);
    }

    /**
     * Check if a chunk is claimed by a team or not.
     */
    public boolean isChunkClaimedByTeam(ChunkPosition position, UUID teamId) {
        ClientClaimedChunk claimedChunk = claimedChunks.get(position);

        return claimedChunk != null && claimedChunk.teamId().equals(teamId);
    }

    /**
     * Get all chunks claimed by a team.
     */
    public Map<ChunkPosition, ClientClaimedChunk> getTeamClaimedChunks(UUID teamId) {
        Map<ChunkPosition, ClientClaimedChunk> result = new HashMap<>();

        for (Map.Entry<ChunkPosition, ClientClaimedChunk> entry : claimedChunks.entrySet()) {
            if (entry.getValue().teamId().equals(teamId)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        return result;
    }

    /**
     * Get permissions override for a player in a chunk.
     */
    public Permission getPermissionOverride(ChunkPosition position, UUID playerUUID) {
        Map<UUID, Permission> permissions = permissionOverrides.get(position);

        if (permissions != null) {
            return permissions.get(playerUUID);
        }

        return null;
    }

    /**
     * Get all claimed chunks.
     */
    public Map<ChunkPosition, ClientClaimedChunk> getAllClaimedChunks() {
        return new HashMap<>(claimedChunks);
    }

    /**
     * Clear all chunk data
     * Called when the player logs out from the server.
     */
    public void clearChunkData() {
        claimedChunks.clear();
        permissionOverrides.clear();
    }
}
