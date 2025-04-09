package Sky.Cat.CTC.chunk;

import Sky.Cat.CTC.Main;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.World;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkState extends PersistentState {
    public static final Codec<ChunkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(
                    Codec.STRING,
                    ClaimedChunk.CODEC
            ).fieldOf("claimedChunks").forGetter(state -> state.claimedChunksMap)
    ).apply(instance, ChunkState::new));

    private static final PersistentStateType<ChunkState> TYPE = new PersistentStateType<>(Main.MOD_ID + "_chunks", ChunkState::new, CODEC, null);

    private final Map<String, ClaimedChunk> claimedChunksMap;

    public ChunkState() {
        this.claimedChunksMap = new ConcurrentHashMap<>();
    }

    public ChunkState(Map<String, ClaimedChunk> claimedChunksMap) {
        this.claimedChunksMap = new ConcurrentHashMap<>(claimedChunksMap);
    }

    private static String positionToString(ChunkPosition pos) {
        return pos.getX() + "," + pos.getZ() + "," + pos.getDimension();
    }

    private static ChunkPosition stringToPosition(String str) {
        String[] args = str.split(",");

        if (args.length != 3) {
            throw new IllegalArgumentException("Invalid position string: " + str);
        }

        int x = Integer.parseInt(args[0]);
        int z = Integer.parseInt(args[1]);
        String dimension = args[2];

        return new ChunkPosition(x, z, dimension);
    }

    /**
     * Add a claimed chunk to the state.
     */
    public void addClaimedChunk(ClaimedChunk chunk) {
        String key = positionToString(chunk.getPosition());
        claimedChunksMap.put(key, chunk);
        markDirty();
    }

    /**
     * Remove a claimed chunk from the state.
     */
    public boolean removeClaimedChunk(ChunkPosition position) {
        String key = positionToString(position);
        ClaimedChunk removed = claimedChunksMap.remove(key);

        if (removed != null) {
            markDirty();
            return true;
        }

        return false;
    }

    /**
     * Get a claimed chunk via position.
     */
    public ClaimedChunk getClaimedChunk(ChunkPosition position) {
        String key = positionToString(position);
        return claimedChunksMap.get(key);
    }

    /**
     * Check if a chunk is claimed or not.
     */
    public boolean isChunkClaimed(ChunkPosition position) {
        String key = positionToString(position);
        return claimedChunksMap.containsKey(key);
    }

    /**
     * Get and create a new map for all claimed chunks.
     */
    public Map<ChunkPosition, ClaimedChunk> getAllClaimedChunks() {
        Map<ChunkPosition, ClaimedChunk> result = new ConcurrentHashMap<>();

        for (Map.Entry<String, ClaimedChunk> entry : claimedChunksMap.entrySet()) {
            ChunkPosition pos = entry.getValue().getPosition();
            result.put(pos, entry.getValue());
        }

        return result;
    }

    public static ChunkState getOrCreate(MinecraftServer server) {
        return Objects.requireNonNull(server.getWorld(World.OVERWORLD)).
                getPersistentStateManager().
                getOrCreate(TYPE);
    }
}
