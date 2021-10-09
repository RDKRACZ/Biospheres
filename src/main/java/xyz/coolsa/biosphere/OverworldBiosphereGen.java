package xyz.coolsa.biosphere;
import java.util.function.Supplier;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public class OverworldBiosphereGen extends NoiseChunkGenerator {
    protected ChunkRandom chunkRandom;
    protected Supplier<ChunkGeneratorSettings> settings;
    protected int sphereDistance = 175;
    protected int sphereRadiusMax = 75;
    protected int sphereRadiusMin = 10;
    protected long seed;
	protected int oreSphereRadius;
	protected int lakeRadius;
	protected int shoreRadius;
	protected BiomeSource biomeSource;
	protected double generatedSphereHeight;

    public static final Codec<OverworldBiosphereGen> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
				   Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
                   ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter((generator) -> generator.settings))
			.apply(instance, instance.stable(OverworldBiosphereGen::new)));

    @Override
    protected Codec<? extends NoiseChunkGenerator> getCodec() {
        return OverworldBiosphereGen.CODEC;
    }

    private int getSphereRadius(int centerX, int centerY){
        if(centerX == 0 && centerY == 0){
            return sphereRadiusMax;
        }
        // make random sized spheres
        this.chunkRandom.setTerrainSeed(centerX, centerY);
        return this.chunkRandom.nextInt(sphereRadiusMax - sphereRadiusMin) + sphereRadiusMin;
    }

    @Override
    public NoiseChunkGenerator withSeed(long seed) {
        return new OverworldBiosphereGen(this.biomeSource.withSeed(seed), seed, settings);
    }

    public OverworldBiosphereGen(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
        super(biomeSource,seed,settings);
        this.seed = seed;
        this.settings = settings;
        this.biomeSource = biomeSource;
        this.chunkRandom = new ChunkRandom(seed);
     }

     @Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
        super.buildSurface(region, chunk);

		ChunkPos chunkPos = chunk.getPos();
        BlockPos centerPos = this.getNearestCenterSphere(chunkPos.getStartPos());
		BlockPos.Mutable current = new BlockPos.Mutable();

        // get the actual starting x position of the chunk.
        int xPos = chunkPos.getStartX();

        // get the actual starting z position of the chunk.
        int zPos = chunkPos.getStartZ();

        // for all vertical slices in the chunk, eat the terrain leaving only the sphere behind
        for(int x = 0; x < 16; x++){
            for(int z = 0; z < 16; z++){
                carveSphere(chunk, centerPos, x + xPos, z + zPos);

                // Set last 5 levels of the world to air from bedrock. Not dealing with it, nope.
                for(int y = getMinimumY(); y < getMinimumY() + 5; y++){
                    current.set(x + xPos, y, z + zPos);
                    if(chunk.getBlockState(current) == Blocks.BEDROCK.getDefaultState()){
                        chunk.setBlockState(current, Blocks.AIR.getDefaultState(), false);
                    }
                }
            }
        }

		// current.set(current.getX(), 100, current.getZ());
        // chunk.setBlockState(current, Blocks.SEA_LANTERN.getDefaultState(), false);
	}

    private void carveSphere(Chunk chunk, BlockPos centerPos, int x, int z){
        // -64 : 396 = 320
        BlockPos.Mutable pos = new BlockPos.Mutable();

        // make random sized spheres
        int sphereRadius = getSphereRadius(centerPos.getX(), centerPos.getZ());

        for(int y = getMinimumY(); y < getWorldHeight() - getMinimumY(); y++){
            // we set our current position to the current column.
            pos.set(x,y,z);

            // now get the 2d distance to the center block.
            double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));

            // if we are inside of said distance, we know we can generate at some positions
            // inside this chunk.
            if (radialDistance <= sphereRadius) {
                // we also calculate the "height" of the column of the sphere.
                double sphereHeight = Math.sqrt(sphereRadius * sphereRadius - (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX()) - (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));

                // Ignore blocks from [centerpos(y) + height, centerpos(y)-height]
                if(pos.getY() > centerPos.getY()+(int)sphereHeight || pos.getY() < centerPos.getY()-(int)sphereHeight) {
                    // by default, the block is air.
                    chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
                } else {
                    // this is our section we want to keep
                }
            } else {
                chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
            }
        }
     }

     @Override
     public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
        // Generate the normal terrain features
        super.generateFeatures(region, accessor);
        
        // encapsulate our spheres
        BlockPos chunkCenter = new BlockPos(region.getCenterPos().x * 16, 0, region.getCenterPos().z * 16);
        BlockPos.Mutable current = new BlockPos.Mutable();

        BlockPos centerPos = this.getNearestCenterSphere(chunkCenter);
        //setBlock(region, centerPos, Blocks.GLOWSTONE.getDefaultState());

        // make random sized spheres
        int sphereRadius = getSphereRadius(centerPos.getX(), centerPos.getZ());

		// get the actual starting x position of the chunk.
		int xPos = chunkCenter.getX();

		// get the actual starting z position of the chunk.
		int zPos = chunkCenter.getZ();
        
        // now lets iterate over every every column in the chunk.
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 0, zPos + 15)) {
            
			// we set our current position to the current column.
			current.set(pos);

			// now get the 2d distance to the center block.
			double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));

			// if we are inside of said distance, we know we can generate at some positions inside this chunk.
			if (radialDistance <= sphereRadius) {
				// we also calculate the "height" of the column of the sphere.
				double sphereHeight = Math.sqrt(sphereRadius * sphereRadius - (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX()) - (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));

				// now lets iterate over ever position inside of this sphere.
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					BlockState blockState;
                    
					if (newRadialDistance <= sphereRadius - 1) {
						continue;
					}

                    current.set(pos.getX(), y, pos.getZ());

                    this.chunkRandom.setDeepslateSeed(seed, pos.getX(), y, pos.getZ());
                    int rng = this.chunkRandom.nextInt(100);


                    if(region.getBlockState(current) == Blocks.AIR.getDefaultState()){
                        if (region.getBiome(centerPos).getCategory() == Biome.Category.UNDERGROUND || y < getSeaLevel()) {
                            if(rng <= 1){
                                blockState = Blocks.GLOWSTONE.getDefaultState();
                            } else {
                                blockState = Blocks.TINTED_GLASS.getDefaultState();
                            }
						} else {
                            switch(rng){
                                case 0: blockState = Blocks.WHITE_STAINED_GLASS.getDefaultState(); break;
                                case 1: blockState = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(); break;
                                case 2: blockState = Blocks.LIGHT_GRAY_STAINED_GLASS.getDefaultState(); break;
                                case 3: blockState = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(); break;
                                case 4: blockState = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState(); break;
                                default: blockState = Blocks.GLASS.getDefaultState(); break;
                            }
						}
                    } else if(region.getBlockState(current) == Blocks.WATER.getDefaultState()){
                        blockState = Blocks.BLUE_STAINED_GLASS.getDefaultState();
                    }else if(region.getBlockState(current) == Blocks.LAVA.getDefaultState()){
                        blockState = Blocks.MAGMA_BLOCK.getDefaultState();
                    } else {
                        if(rng <= 4){
                            blockState = Blocks.CRYING_OBSIDIAN.getDefaultState();
                        } else {
                            blockState = Blocks.OBSIDIAN.getDefaultState();
                        }
                    }

					region.setBlockState(current.set(pos.getX(), y, pos.getZ()), blockState, 0);
				}
            }
		}
     }

     // Just some error handling...
     public boolean setBlock(ChunkRegion region, BlockPos pos, BlockState state){
        if(pos.getY() > getMinimumY() && pos.getY() < getWorldHeight() - 1){
            if(region.getBlockState(pos) != state){
                region.setBlockState(pos, state, 0);
            }
            return true;
        } else {
            return false;
        }
     }

     public BlockPos getNearestCenterSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
        int centerY = getSeaLevel();
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
        
        int sphereRadius = getSphereRadius(centerX, centerZ);

        // Resolve Y level if not at 0,0. 0,0 must guarantee a place to spawn...
        if(centerX != 0 || centerY != 0){
            int upperbound = getWorldHeight() - sphereRadius; // 208
            int lowerbound = getMinimumY();// + sphereRadius;    // 0
            int sealevel = (upperbound - lowerbound) / 2; //getSeaLevel();                          // 63
            int range;

            if(sealevel - lowerbound > upperbound - sealevel){
                // top to sea level is shorter, so that is our range
                range = upperbound - sealevel;                   // 145
            } else {
                // sea level to bottom is shorter, so that is our range
                range = sealevel - lowerbound;                   // 15
            }

            // gaussian generates a bell curve from -1 to 1.
            double gauss = chunkRandom.nextGaussian(); // [-3,3]
            if(gauss>3){
                gauss = 3;
            }
            if(gauss<-3){
                gauss=-3;
            }
            
            double ywork = (gauss-0.5) * range/3; //[-15, 15]
            ywork = ywork + sealevel; // [-64, 192]

            centerY = (int)((ywork));
            if(centerY < sphereRadius){
                String s = "";
            }
        }

		//int centerY = (int) ((Math.pow((this.chunkRandom.nextFloat() % 0.7) - 0.5, 3) + 0.5) * (sphereRadius * 2 - sphereRadius * 4)) + sphereRadius * 2;
		return new BlockPos(centerX, centerY, centerZ);
	}

}
