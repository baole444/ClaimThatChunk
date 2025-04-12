package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.permission.PermType;
import Sky.Cat.CTC.permission.Permission;
import Sky.Cat.CTC.team.Team;
import Sky.Cat.CTC.team.TeamManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A chunk claimed by a team knows its team UUID and permissions override.
 * The chunk's position is stored using ChunkPosition Object.
 */
public class ClaimedChunk {
    // The claimed chunk's position
    private ChunkPosition position;

    // uuid of the team that claimed the chunk.
    private UUID ownerTeamId;

    // Time stamp of when the chunk was claimed.
    private final long claimedTime;

    // For overriding team member's specific permission.
    private Map<UUID, Permission> permissionOverrides;

    /**
     * ClaimedChunk's CODEC for serialization and deserialization.
     */
    public static final Codec<ClaimedChunk> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ChunkPosition.CODEC.fieldOf("position").forGetter(ClaimedChunk::getPosition),
            Codec.STRING.xmap(UUID::fromString, UUID::toString).fieldOf("ownerTeamId").forGetter(ClaimedChunk::getOwnerTeamId),
            Codec.LONG.fieldOf("claimedTime").forGetter(ClaimedChunk::getClaimedTime),
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    Permission.CODEC
            ).fieldOf("permissionOverrides").forGetter(ClaimedChunk::getPermissionOverrides)
    ).apply(instance, ClaimedChunk::new));

    /**
     * Constructor of ClaimedChunk.
     */
    public ClaimedChunk(ChunkPosition position, UUID ownerTeamId, long claimedTime, Map<UUID, Permission> permissionOverrides) {
        this.position = position;
        this.ownerTeamId = ownerTeamId;
        this.claimedTime = claimedTime;
        this.permissionOverrides = permissionOverrides != null ? permissionOverrides : new HashMap<>();
    }

    /**
     * Create a claimed chunk with current time and no permission overrides.
     */
    public ClaimedChunk(ChunkPosition position, UUID ownerTeamId) {
        this(position, ownerTeamId, System.currentTimeMillis(), new HashMap<>());
    }

    /**
     * Check if a player has their permissions override for this chunk.
     * @param playerUUID uuid of the player to check on.
     * @param permType type of permission to check for.
     * @return true if the player has permission.
     */
    public boolean hasPermission(UUID playerUUID, PermType permType) {
        // Check for override permission
        if (permissionOverrides.containsKey(playerUUID))
            return permissionOverrides.
                    get(playerUUID).
                    hasPermission(permType);

        Team ownerTeam = TeamManager.getInstance().getTeamById(ownerTeamId);

        // If the team no longer existed.
        if (ownerTeam == null) return false;

        // Get the player's permission in the team
        if (ownerTeam.getTeamMember().containsKey(playerUUID))
            return ownerTeam.
                    getTeamMember().
                    get(playerUUID).
                    getPermission().
                    hasPermission(permType);

        // If the player is not in a team.
        return false;
    }

    public boolean hasPermission(PlayerEntity player, PermType permType) {
        return hasPermission(player.getUuid(), permType);
    }

    public void setPermissionOverride(UUID playerUUID, Permission permission) {
        permissionOverrides.put(playerUUID, permission);
    }

    public boolean removePermissionOverride(UUID playerUUID) {
        return permissionOverrides.remove(playerUUID) != null;
    }

    public boolean isBlockPosInChunk(BlockPos pos, RegistryKey<World> dimension) {
        return position.isBlockPosInChunk(pos, dimension);
    }

    public ChunkPosition getPosition() {
        return position;
    }

    public UUID getOwnerTeamId() {
        return ownerTeamId;
    }

    public long getClaimedTime() {
        return claimedTime;
    }

    public Map<UUID, Permission> getPermissionOverrides() {
        return permissionOverrides;
    }
}
