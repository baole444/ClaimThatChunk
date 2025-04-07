package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.permission.Permission;

import java.util.Map;
import java.util.UUID;

public class ClaimedChunk {
    // The claimed chunk's position
    private ChunkPosition position;

    // uuid of the team that claimed the chunk.
    private UUID ownerTeamId;

    // For overriding member's specific permission.
    // For example, this chunk will let Ben but does not for Dover to build on it.
    // Likely initiate to null if there is no override to
    // reduce assigning excessive permission that will be the same with the team's default.
    private Map<UUID, Permission> permissionOverride;


}
