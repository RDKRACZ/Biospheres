package xyz.coolsa.biosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

public class BiospheresChunkGenerator extends ChunkGenerator {
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final int oreSphereRadius;
	protected final int lakeRadius;
	protected final int shoreRadius;
	protected final BiomeSource biomeSource;
	protected final ChunkRandom chunkRandom;
	protected final OctavePerlinNoiseSampler noiseSampler;
	protected final BlockState defaultBlock;
	protected final BlockState defaultNetherBlock;
	protected final BlockState defaultFluid;
	protected final BlockState defaultBridge;
	protected final BlockState defaultEdge;
	protected final BlockState defaultEdgeX;
	protected final BlockState defaultEdgeZ;
	public static final Codec<BiospheresChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
					Codec.INT.fieldOf("sphere_distance").forGetter((generator) -> generator.sphereDistance),
					Codec.INT.fieldOf("sphere_radius").forGetter((generator) -> generator.sphereRadius),
					Codec.INT.fieldOf("lake_radius").forGetter((generator) -> generator.lakeRadius),
					Codec.INT.fieldOf("shore_radius").forGetter((generator) -> generator.shoreRadius))
			.apply(instance, instance.stable(BiospheresChunkGenerator::new)));

	public BiospheresChunkGenerator(BiomeSource biomeSource, long seed, int sphereDistance, int sphereRadius,
			int lakeRadius, int shoreRadius) {
		super(biomeSource, new StructuresConfig(false));
		this.biomeSource = biomeSource;
		this.seed = seed;
		this.sphereDistance = sphereRadius * 4;
		this.sphereRadius = sphereRadius;
		this.oreSphereRadius = 8; // TODO: add in ore spheres. also set to -ve to do no ore spheres
		this.lakeRadius = lakeRadius;
		this.shoreRadius = shoreRadius;
		this.defaultBlock = Blocks.STONE.getDefaultState();
		this.defaultNetherBlock = Blocks.NETHERRACK.getDefaultState();
		this.defaultFluid = Blocks.WATER.getDefaultState();
		this.defaultBridge = Blocks.OAK_PLANKS.getDefaultState();
		this.defaultEdge = Blocks.OAK_FENCE.getDefaultState();
		this.defaultEdgeX = Blocks.OAK_FENCE.getDefaultState().with(Properties.EAST, true).with(Properties.WEST, true);
		this.defaultEdgeZ = Blocks.OAK_FENCE.getDefaultState().with(Properties.NORTH, true).with(Properties.SOUTH, true);
		this.chunkRandom = new ChunkRandom(this.seed);
		this.chunkRandom.consume(1000);
		this.noiseSampler = new OctavePerlinNoiseSampler(this.chunkRandom, IntStream.rangeClosed(-3, 0));
	}
	
	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		BlockPos centerPos = this.getNearestCenterSphere(chunk.getPos().getStartPos());
		BlockPos.Mutable current = new BlockPos.Mutable();
		for (BlockPos pos : BlockPos.iterate(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(),
				chunk.getPos().getEndX(), 0, chunk.getPos().getEndZ())) {

			if (region.getBiome(centerPos).getCategory() == Biome.Category.NETHER){
				region.getBiome(current.set(pos)).buildSurface(this.chunkRandom, chunk, pos.getX(), pos.getZ(),
						centerPos.getY() * 4, 0.0625, this.defaultNetherBlock, this.defaultFluid, -10, this.seed);
			} else {
				region.getBiome(current.set(pos)).buildSurface(this.chunkRandom, chunk, pos.getX(), pos.getZ(),
						centerPos.getY() * 4, 0.0625, this.defaultBlock, this.defaultFluid, -10, this.seed);
			}
		}
	}

	@Override
	public int getHeight(int x, int z, Type heightmapType) {
		return 0;
	}

	@Override
	public BlockView getColumnSample(int x, int z) {
		return null;
	}


	@Override
	public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {
		// get the starting position of the chunk we will generate.
		ChunkPos chunkPos = chunk.getPos();
		// also get the current working block.
		BlockPos.Mutable current = new BlockPos.Mutable();
		// get the actual starting x position of the chunk.
		int xPos = chunkPos.getStartX();
		// get the actual starting z position of the chunk.
		int zPos = chunkPos.getStartZ();
		// find the center of the nearest sphere.
		BlockPos centerPos = this.getNearestCenterSphere(chunkPos.getStartPos());
		// TODO: ore sphere conditional generator.
		BlockPos oreCenterPos = this.getNearestOreSphere(chunkPos.getStartPos());
		// get the block that should be at the center.
		BlockState fluidBlock = this.getLakeBlock(centerPos, chunk.getBiomeArray().getBiomeForNoiseGen(chunkPos.x, centerPos.getY() ,chunkPos.z));
		// begin keeping track of the heightmap.
		Heightmap oceanHeight = chunk.getHeightmap(Type.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getHeightmap(Type.WORLD_SURFACE_WG);
		// now lets iterate over every every column in the chunk.
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 0, zPos + 15)) {
			// we set our current position to the current column.
			current.set(pos);
			// now get the 2d distance to the center block.
			double radialDistance = Math
					.sqrt(pos.getSquaredDistance(centerPos.getX(), pos.getY(), centerPos.getZ(), false));
			double oreRadialDistance = Math
					.sqrt(pos.getSquaredDistance(oreCenterPos.getX(), pos.getY(), oreCenterPos.getZ(), false));
			// if we are inside of said distance, we know we can generate at some positions
			// inside this chunk.
			if (radialDistance <= this.sphereRadius) {
				// so we sample the noise height.
				double noise = this.noiseSampler.sample(pos.getX() / 8.0, pos.getZ() / 8.0, 1 / 16.0, 1 / 16.0) / 16;
				// we also calculate the "height" of the column of the sphere.
				double sphereHeight = Math.sqrt(this.sphereRadius * this.sphereRadius
						- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
						- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
				// now lets iterate over ever position inside of this sphere.
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					// calculate the radial distance for lake gen.
					double lakeDistance = Math.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					// calculate the radial distance for lake gen in 2d space.
					double lakeDistance2d = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));

					// also lets do some math for our noise generator.
					double noiseTemp = (noise + y / centerPos.getY());
					// by default, the block is air.
					BlockState blockState = Blocks.AIR.getDefaultState();
					// if we are below the noise gradient, we can set this block to stone!
					if (y * noiseTemp < centerPos.getY()) {
						if (chunk.getBiomeArray().getBiomeForNoiseGen(chunkPos.x, centerPos.getY(), chunkPos.z).getCategory() == Biome.Category.NETHER) {
							blockState = this.defaultNetherBlock;
						} else {
							blockState = this.defaultBlock;
						}
					}
					// now lets check if we can do our lake gen.
					if (((blockState.equals(this.defaultBlock) || blockState.equals(this.defaultNetherBlock))
							&& (lakeDistance2d <= this.lakeRadius))	&& !fluidBlock.equals(Blocks.AIR.getDefaultState())) {
						// if we are above the height and noise value, we will generate air.
						if (y >= centerPos.getY() && !fluidBlock.equals(Blocks.STONE.getDefaultState())) {
							blockState = Blocks.AIR.getDefaultState();
						}
						// otherwise, we are inside of a valid position, so we go ahead and generate our
						// fluids.
						else if (lakeDistance <= this.lakeRadius) {
							blockState = fluidBlock;
						}
					}
					chunk.setBlockState(current.set(pos.getX(), y, pos.getZ()), blockState, false);
					oceanHeight.trackUpdate(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF, blockState);
					worldSurface.trackUpdate(pos.getX() & 0xF, y & 0xF, pos.getZ() & 0xF, blockState);
				}
			}
			if (oreRadialDistance <= this.oreSphereRadius) {
			//		blockState = this.defaultBlock;
			}
		}
	}

	public BlockPos getNearestCenterSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = (int) ((Math.pow((this.chunkRandom.nextFloat() % 1.0) - 0.5, 3) + 0.5)
				* (this.sphereRadius * 2 - this.sphereRadius * 4)) + this.sphereRadius * 2;
		return new BlockPos(centerX, centerY, centerZ);
	}

	public BlockPos getNearestOreSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance - 0.5) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance - 0.5) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = this.chunkRandom.nextInt(256 - this.oreSphereRadius * 4) + this.oreSphereRadius * 2;
		return new BlockPos(centerX + this.sphereDistance / 2, centerY, centerZ + this.sphereDistance / 2);
	}

	public BlockState getLakeBlock(BlockPos center, Biome biome) {
		this.chunkRandom.setTerrainSeed(center.getX(), center.getZ());
		int rng = this.chunkRandom.nextInt(10);
		BlockState state;
		if (biome.getCategory() == Biome.Category.NETHER) {
			state = Blocks.NETHERRACK.getDefaultState();
		} else {
			state = Blocks.STONE.getDefaultState();
		}
		if (rng >= 1 && rng <= 8)
			state = this.defaultFluid;
		else if (rng == 9) {
			state = Blocks.LAVA.getDefaultState();
		}
		return state;
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new BiospheresChunkGenerator(this.biomeSource.withSeed(seed), seed, this.sphereRadius * 4,
				this.sphereRadius, this.lakeRadius, this.shoreRadius);
	}

	@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
		BlockPos chunkCenter = new BlockPos(region.getCenterChunkX() * 16, 0, region.getCenterChunkZ() * 16);

//		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES, Biospheres.oreIronBiosphere);
		Biome biome = this.biomeSource.getBiomeForNoiseGen(chunkCenter.getX() / 4 + 2, 2, chunkCenter.getZ() / 4 + 2);

		//RangeDecoratorConfig(UniformHeightProvider.create(YOffset.aboveBottom(0), YOffset.fixed(15)))).spreadHorizontally().repeat(4);
		//20, 32, 0, 128
//		biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES,
//				Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE,
//						Blocks.REDSTONE_ORE.getDefaultState(), 8)).createDecoratedFeature(
//								Decorator.COUNT_RANGE.configure(new RangeDecoratorConfig(8, 96, 0, 16))));
//		if (!biome.equals(Biomes.THE_VOID)) {
//			biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE
//					.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE,
//							Blocks.LAPIS_ORE.getDefaultState(), 7))
//					.createDecoratedFeature(
//							Decorator.COUNT_DEPTH_AVERAGE.configure(new CountDepthDecoratorConfig(2, 128, 32))));
//			biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE
//					.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE,
//							Blocks.GOLD_ORE.getDefaultState(), 9))
//					.createDecoratedFeature(
//							Decorator.COUNT_DEPTH_AVERAGE.configure(new CountDepthDecoratorConfig(2, 128, 32))));
//			biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE
//					.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE,
//							Blocks.DIAMOND_ORE.getDefaultState(), 8))
//					.createDecoratedFeature(
//							Decorator.COUNT_DEPTH_AVERAGE.configure(new CountDepthDecoratorConfig(1, 128, 32))));
//		}
		super.generateFeatures(region, accessor);
//		BlockPos chunkCenter = new BlockPos(region.getCenterChunkX() * 16, 0, region.getCenterChunkZ() * 16);
//		Biome biome = this.biomeSource.getBiomeForNoiseGen(chunkCenter.getX() / 4 + 2, 2, chunkCenter.getZ() / 4 + 2);
//		
//		long populationSeed = this.chunkRandom.setPopulationSeed(region.getSeed(), chunkCenter.getX(),
//				chunkCenter.getZ());
//		for (final GenerationStep.Feature feature : GenerationStep.Feature.values()) {
//			if (feature.equals(GenerationStep.Feature.LAKES)) {
//				continue;
//			}
//			try {
//				biome.generateFeatureStep(feature, accessor, this, region, populationSeed, this.chunkRandom,
//						chunkCenter);
//			} catch (Exception exception) {
//				CrashReport crashReport = CrashReport.create(exception, "Biosphere Biome Decoration");
//				crashReport.addElement("Generation").add("CenterX", chunkCenter.getX())
//						.add("CenterZ", chunkCenter.getZ()).add("Step", feature).add("Seed", populationSeed)
//						.add("Biome", Registry.BIOME.getId(biome));
//				throw new CrashException(crashReport);
//			}
//		}
//		if(runs > 9) {
		this.finishBiospheres(region);
//		}
	}

	public BlockPos[] getClosestSpheres(BlockPos centerPos) {
		BlockPos[] nesw = new BlockPos[4];
		for (int i = 0; i < 4; i++) {
			int xMod = centerPos.getX();
			int zMod = centerPos.getZ();
			if (i / 2 < 1) {
				xMod += (int) Math.round(Math.pow(-1, i) * this.sphereDistance);
			} else {
				zMod += (int) Math.round(Math.pow(-1, i) * this.sphereDistance);
			}
			nesw[i] = this.getNearestCenterSphere(new BlockPos(xMod, 0, zMod));
		}
		return nesw;
	}

	public void finishBiospheres(ChunkRegion region) {
		BlockPos chunkCenter = new BlockPos(region.getCenterChunkX() * 16, 0, region.getCenterChunkZ() * 16);
		BlockPos.Mutable current = new BlockPos.Mutable();
		BlockPos centerPos = this.getNearestCenterSphere(chunkCenter);
		for (final BlockPos pos : BlockPos.iterate(chunkCenter.getX() - 7, 0, chunkCenter.getZ() - 7,
				chunkCenter.getX() + 8, 0, chunkCenter.getZ() + 8)) {
			current.set(pos);
			double radialDistance = Math
					.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));
			double noise = this.noiseSampler.sample(pos.getX() / 8.0, pos.getZ() / 8.0, 1 / 16.0, 1 / 16.0) / 8;
			double sphereHeight = Math.sqrt(this.sphereRadius * this.sphereRadius
					- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
					- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
			if (radialDistance <= this.sphereRadius + 16) {
				for (int y = centerPos.getY() - (int) sphereHeight; y <= sphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					double noiseTemp = (noise + y / centerPos.getY());
					BlockState blockState = Blocks.AIR.getDefaultState();
					if (newRadialDistance <= this.sphereRadius - 1) {
						continue;
					}
					if (y * noiseTemp >= centerPos.getY()) {
						blockState = Blocks.GLASS.getDefaultState();
					} else {
						if (region.getBiome(chunkCenter).getCategory() == Biome.Category.NETHER) {
							blockState = this.defaultNetherBlock;
						} else {

						}blockState = this.defaultBlock;
					}
					region.setBlockState(current.set(pos.getX(), y, pos.getZ()), blockState, 0);
				}
				double largerSphereHeight = Math.sqrt((this.sphereRadius + 16) * (this.sphereRadius + 16)
						- (centerPos.getX() - pos.getX()) * (centerPos.getX() - pos.getX())
						- (pos.getZ() - centerPos.getZ()) * (pos.getZ() - centerPos.getZ()));
				for (int y = 0; y <= largerSphereHeight + centerPos.getY(); y++) {
					double newRadialDistance = Math
							.sqrt(centerPos.getSquaredDistance(pos.getX(), y, pos.getZ(), false));
					if (newRadialDistance >= this.sphereRadius) {
						region.setBlockState(current.set(pos.getX(), y, pos.getZ()), Blocks.AIR.getDefaultState(), 0);
					}
				}
			}
			this.makeBridges(pos, centerPos, this.getClosestSpheres(centerPos), region, current);
		}
	}

	public void makeBridges(BlockPos pos, BlockPos centerPos, BlockPos[] nesw, ChunkRegion region,
			BlockPos.Mutable current) {
		// generating bridges!
		double radialDistance = Math
				.sqrt(centerPos.getSquaredDistance(pos.getX(), centerPos.getY(), pos.getZ(), false));
//		if (radialDistance < this.sphereRadius) {
//			return;
//		}
		for (int i = 0; i < 4; i++) {
			if (radialDistance > this.sphereRadius - 2) {
				double slope = nesw[i].getY() - centerPos.getY();
//				BlockState blockState = Blocks.AIR.getDefaultState();
				double currentPos = 0;
				switch (i) {
				case (0):
					slope /= Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) - 2 * this.sphereRadius;
//					blockState = Blocks.BLUE_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getX() - pos.getX() + this.sphereRadius;
					if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
						if (pos.getX() > centerPos.getX()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, true);
							// x axis
						}
					}
					break;
				case (1):
					slope /= -Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) + 2 * this.sphereRadius;
//					blockState = Blocks.PURPLE_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getX() - pos.getX() - this.sphereRadius;
					if (pos.getZ() <= centerPos.getZ() + 2 && pos.getZ() >= centerPos.getZ() - 2) {
						if (pos.getX() < centerPos.getX()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, true);
							// x axis
						}
					}
					break;
				case (2):
					slope /= -Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) + 2 * this.sphereRadius;
//					blockState = Blocks.RED_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getZ() - pos.getZ() + this.sphereRadius;
					if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
						if (pos.getZ() > centerPos.getZ()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, false);
							// z axis
						}
					}
					break;
				case (3):
					slope /= Math.abs((double) (centerPos.getZ() - nesw[i].getZ())) - 2 * this.sphereRadius;
//					blockState = Blocks.YELLOW_STAINED_GLASS.getDefaultState();
					currentPos = centerPos.getZ() - pos.getZ() - this.sphereRadius;
					if (pos.getX() <= centerPos.getX() + 2 && pos.getX() >= centerPos.getX() - 2) {
						if (pos.getZ() < centerPos.getZ()) {
							this.fillBridgeSlice(
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()),
									new BlockPos(centerPos.getX(), slope * currentPos + centerPos.getY(), centerPos.getZ()),
									region, current, false);
							// z axis
						}
					}
					break;
				}
			}
		}
	}

	public void fillBridgeSlice(BlockPos pos, BlockPos centerPos, ChunkRegion region, BlockPos.Mutable current, boolean xa) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		int cx = centerPos.getX();
		int cz = centerPos.getZ();
		region.setBlockState(current.set(x, y - 1, z), this.defaultBridge, 0);
		region.setBlockState(current.set(x, y, z), Blocks.AIR.getDefaultState(), 0);
		if(xa) {
			region.setBlockState(current.set(x, y, cz+2), this.defaultEdgeX, 0);
			region.setBlockState(current.set(x, y, cz-2), this.defaultEdgeX, 0);
		} else {
			region.setBlockState(current.set(cx+2, y, z), this.defaultEdgeZ, 0);
			region.setBlockState(current.set(cx-2, y, z), this.defaultEdgeZ, 0);
		}
		region.setBlockState(current.set(x, y + 1, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 2, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 3, z), Blocks.AIR.getDefaultState(), 0);


	}

	public void fillBridgeEdge(BlockPos pos, ChunkRegion region, BlockPos.Mutable current, boolean xa) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		/*if (xa) {
			region.setBlockState(current.set(x, y, z - 3), this.defaultEdge, 0);
			region.setBlockState(current.set(x, y, z + 3), this.defaultEdge, 0);
		} else {
			region.setBlockState(current.set(x - 3, y, z), this.defaultEdge, 0);
			region.setBlockState(current.set(x + 3, y, z), this.defaultEdge, 0);
		}*/
	}

	@Override
	protected Codec<? extends ChunkGenerator> getCodec() {
		// TODO Auto-generated method stub
		return BiospheresChunkGenerator.CODEC;
	}

//	@Override
//	public void populateEntities(ChunkRegion region) {
//	}
}
