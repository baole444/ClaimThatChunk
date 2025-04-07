package Sky.Cat.CTC.chunk;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ChunkPosition {
    private final int x;
    private final int z;
    private final String dimension;

    public ChunkPosition(int x, int z, String dimension) {
        this.x = x;
        this.z = z;
        this.dimension = dimension;
    }

    public ChunkPosition(BlockPos pos) {
        this(pos.getX() >> 4, pos.getZ() >> 4, "overworld");
    }

    public ChunkPosition(BlockPos pos, RegistryKey<World> dimension) {
        this(pos.getX() >> 4, pos.getZ() >> 4, dimension.getValue().toString());
    }

    // TODO: this is just draft implement, need to add methods and other necessary detail.

}
