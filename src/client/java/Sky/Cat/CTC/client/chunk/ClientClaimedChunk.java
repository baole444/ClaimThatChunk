package Sky.Cat.CTC.client.chunk;

import Sky.Cat.CTC.chunk.ChunkPosition;

import java.util.UUID;

public record ClientClaimedChunk(ChunkPosition position, UUID teamId, long claimedTime) {
}
