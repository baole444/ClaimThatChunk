package Sky.Cat.CTC.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import java.util.Objects;

/**
 * Store claimed chunk's coordinate and dimension.
 */
public class ChunkPosition {
    private final int x;
    private final int z;
    private final String dimension;

    /**
     * ChunkPosition's CODEC for serialization and deserialization.
     */
    public static final Codec<ChunkPosition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("x").forGetter(ChunkPosition::getX),
        Codec.INT.fieldOf("z").forGetter(ChunkPosition::getZ),
        Codec.STRING.fieldOf("dimension").forGetter(ChunkPosition::getDimension)
    ).apply(instance, ChunkPosition::new));

    /**
     * ChunkPosition's PACKET_CODEC for networking.
     */
    public static final PacketCodec<RegistryByteBuf, ChunkPosition> PACKET_CODEC = PacketCodec.tuple(
            PacketCodecs.INTEGER,
            ChunkPosition::getX,

            PacketCodecs.INTEGER,
            ChunkPosition::getZ,

            PacketCodecs.STRING,
            ChunkPosition::getDimension,

            ChunkPosition::new
    );

    /**
     * Constructor for chunk position
     * @param x coordinate of the x-axis.
     * @param z coordinate of the z-axis.
     * @param dimension dimension that the chunk was in.
     */
    public ChunkPosition(int x, int z, String dimension) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
    }

    /**
     * Create a chunk position in overworld.
     * @param pos the block position within a chunk.
     */
    public ChunkPosition(BlockPos pos) {
        this(pos.getX() >> 4, pos.getZ() >> 4, "minecraft:overworld");
    }

    /**
     * Create a chunk position in the overworld.
     * @param pos the position of the chunk.
     */
    public ChunkPosition(ChunkPos pos) {
        this(pos.x, pos.z, "minecraft:overworld");
    }

    /**
     * Create a chunk position in the given dimension.
     * @param pos the block position within a chunk.
     * @param dimension dimension that the chunk was in.
     */
    public ChunkPosition(BlockPos pos, RegistryKey<World> dimension) {
        this(pos.getX() >> 4, pos.getZ() >> 4, dimension.getValue().toString());
    }

    /**
     * Create a chunk position in the given dimension.
     * @param pos the position of the chunk.
     * @param dimension dimension that the chunk was in.
     */
    public ChunkPosition(ChunkPos pos, RegistryKey<World> dimension) {
        this(pos.x, pos.z, dimension.getValue().toString());
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public String getDimension() {
        return dimension;
    }

    /**
     * Convert ChunkPosition's x and z to a ChunkPos object
     * @return a ChunkPos object from x and z coordinate.
     */
    public ChunkPos toChunkPos() {
        return new ChunkPos(x, z);
    }

    //<editor-fold defaultstate="collapsed" desc="Visualization methods">
    public BlockPos getMinBlockPos() {
        return new BlockPos(x << 4, World.MIN_Y, z << 4);
    }

    public BlockPos getMaxBlockPos() {
        return new BlockPos((x << 4) + 15, World.MAX_Y, (z << 4) + 15);
    }

    public BlockPos getCenterBlockPos() {
        return new BlockPos((x << 4) + 8, 64, (z << 4) + 8);
    }
    //</editor-fold>

    public RegistryKey<World> getDimensionKey() {
        return RegistryKey.of(
                RegistryKey.ofRegistry(Identifier.of("dimension")),
                Identifier.of(dimension)
        );
    }

    public boolean isBlockPosInChunk(BlockPos pos, RegistryKey<World> dimension) {
        if (!this.dimension.equals(dimension.getValue().toString())) return false;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        return chunkX == x && chunkZ == z;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (obj == null || getClass() != obj.getClass()) return false;

        ChunkPosition other = (ChunkPosition) obj;
        return x == other.x && z == other.z && Objects.equals(dimension, other.dimension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, z, dimension);
    }

    @Override
    public String toString() {
        return "ChunkPosition{ "
                + "x = " + x
                + ", z = " + z
                + ", dimension = '"
                + dimension + "'"
                + " }";
    }
}
