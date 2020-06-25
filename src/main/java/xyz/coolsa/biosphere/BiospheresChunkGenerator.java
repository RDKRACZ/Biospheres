package xyz.coolsa.biosphere;

import java.util.Random;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.decorator.CountDepthDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.DefaultBiomeFeatures;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;

public class BiospheresChunkGenerator extends ChunkGenerator {
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final int oreSphereRadius;
	protected final int lakeRadius;
	protected final BiomeSource biomeSource;
	protected final ChunkRandom chunkRandom;
	protected final OctavePerlinNoiseSampler noiseSampler;
	protected final BlockState defaultBlock;
	protected final BlockState defaultFluid;
	protected final BlockState defaultBridge;
	protected final BlockState defaultEdge;
	public static final Codec<BiospheresChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.field_24713.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed))
			.apply(instance, instance.stable(BiospheresChunkGenerator::new)));

	public BiospheresChunkGenerator(BiomeSource biomeSource, long seed) {
		super(biomeSource, new StructuresConfig(false));
		this.biomeSource = biomeSource;
		this.seed = seed;
		this.sphereDistance = 128;
		this.sphereRadius = 32;
		this.oreSphereRadius = 8;
		this.lakeRadius = 8;
		this.defaultBlock = Blocks.STONE.getDefaultState();
		this.defaultFluid = Blocks.WATER.getDefaultState();
		this.defaultBridge = Blocks.OAK_PLANKS.getDefaultState();
		this.defaultEdge = Blocks.OAK_FENCE.getDefaultState();
		this.chunkRandom = new ChunkRandom(this.seed);
		this.chunkRandom.consume(1000);
		this.noiseSampler = new OctavePerlinNoiseSampler(this.chunkRandom, IntStream.rangeClosed(-3, 0));
		// TODO Auto-generated constructor stub
	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		BlockPos centerPos = this.getNearestCenterSphere(chunk.getPos().getCenterBlockPos());
		BlockPos.Mutable current = new BlockPos.Mutable();
		for (BlockPos pos : BlockPos.iterate(chunk.getPos().getStartX(), 0, chunk.getPos().getStartZ(),
				chunk.getPos().getEndX(), 0, chunk.getPos().getEndZ())) {
			region.getBiome(current.set(pos)).buildSurface(this.chunkRandom, chunk, pos.getX(), pos.getZ(),
					centerPos.getY() * 4, 0.0625, this.defaultBlock, this.defaultFluid, -10, this.seed);
		}

	}

	@Override
	public BlockView getColumnSample(int x, int z) {
		// TODO Auto-generated method stub
		return new VerticalBlockSample(new BlockState[0]);
	}

	@Override
	public int getHeight(int x, int z, Type heightmapType) {
		// TODO Auto-generated method stub
		return 64;
	}

	@Override
	protected Codec<? extends ChunkGenerator> method_28506() {
		// TODO Auto-generated method stub
		return BiospheresChunkGenerator.CODEC;
	}

	@Override
	public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		BlockPos.Mutable current = new BlockPos.Mutable();
		int xPos = chunkPos.getStartX();
		int zPos = chunkPos.getStartZ();
		BlockPos centerPos = this.getNearestCenterSphere(chunkPos.getCenterBlockPos());
		BlockState fluidBlock = this.getLakeBlock(centerPos);
		Heightmap oceanHeight = chunk.getHeightmap(Type.OCEAN_FLOOR_WG);
		Heightmap worldSurface = chunk.getHeightmap(Type.WORLD_SURFACE_WG);
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 256, zPos + 15)) {
			current.set(pos);
			double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos));
			BlockState blockState = Blocks.AIR.getDefaultState();
			if (radialDistance <= this.sphereRadius) {
				double noise = this.noiseSampler.sample(pos.getX() / 16.0, pos.getZ() / 16.0, 1 / 16.0, 1 / 16.0) / 8
						+ (pos.getY() / centerPos.getY());
				if (pos.getY() * noise < centerPos.getY()) {
					blockState = this.defaultBlock;
				}
				if (blockState.equals(this.defaultBlock) && radialDistance <= this.lakeRadius) {
					if (pos.getY() * noise >= centerPos.getY()) {
						blockState = Blocks.AIR.getDefaultState();
					} else if (fluidBlock.equals(this.defaultFluid)) {
						blockState = fluidBlock;
					} else if (fluidBlock.equals(Blocks.LAVA.getDefaultState())) {
						blockState = fluidBlock;
					}
				}
			}
			chunk.setBlockState(current.set(pos), blockState, false);
			oceanHeight.trackUpdate(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, blockState);
			worldSurface.trackUpdate(pos.getX() & 0xF, pos.getY() & 0xF, pos.getZ() & 0xF, blockState);
		}
	}

	public BlockPos getNearestCenterSphere(BlockPos pos) {
		int xPos = pos.getX();
		int zPos = pos.getZ();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = (int) ((Math.pow((this.chunkRandom.nextFloat() % 1.0) - 0.5, 3) + 0.5)
				* (256 - this.sphereRadius * 4)) + this.sphereRadius * 2;
		return new BlockPos(centerX, centerY, centerZ);
	}

	public BlockState getLakeBlock(BlockPos center) {
		this.chunkRandom.setTerrainSeed(center.getX(), center.getZ());
		int rng = this.chunkRandom.nextInt(10);
		BlockState state = Blocks.AIR.getDefaultState();
		if (rng >= 5 && rng <= 9)
			state = this.defaultFluid;
		else if (rng == 10) {
			state = Blocks.LAVA.getDefaultState();
		}
		return state;
	}

	@Override
	public ChunkGenerator withSeed(long arg0) {
		return new BiospheresChunkGenerator(this.biomeSource, arg0);
	}

	@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
	}

	@Override
	public void setStructureStarts(StructureAccessor structureAccessor, Chunk chunk, StructureManager structureManager,
			long seed) {

	}

	@Override
	public void addStructureReferences(WorldAccess world, StructureAccessor accessor, Chunk chunk) {

	}

	@Override
	public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
		BlockPos chunkCenter = new BlockPos(region.getCenterChunkX() * 16, 0, region.getCenterChunkZ() * 16);
		Biome biome = this.biomeSource.getBiomeForNoiseGen(chunkCenter.getX() / 4 + 2, 2, chunkCenter.getZ() / 4 + 2);
		
        biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE, Blocks.IRON_ORE.getDefaultState(), 9)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(new RangeDecoratorConfig(20, 32, 0, 128))));
        biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE, Blocks.GOLD_ORE.getDefaultState(), 9)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(new RangeDecoratorConfig(2, 64, 0, 32))));
        biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE, Blocks.REDSTONE_ORE.getDefaultState(), 8)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(new RangeDecoratorConfig(8, 100, 0, 16))));
        biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE, Blocks.DIAMOND_ORE.getDefaultState(), 8)).createDecoratedFeature(Decorator.COUNT_RANGE.configure(new RangeDecoratorConfig(1, 100, 0, 16))));
        biome.addFeature(GenerationStep.Feature.UNDERGROUND_ORES, Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Target.NATURAL_STONE, Blocks.LAPIS_ORE.getDefaultState(), 7)).createDecoratedFeature(Decorator.COUNT_DEPTH_AVERAGE.configure(new CountDepthDecoratorConfig(1, 128, 32))));
 
		long populationSeed = this.chunkRandom.setPopulationSeed(region.getSeed(), chunkCenter.getX(),
				chunkCenter.getZ());
		for (final GenerationStep.Feature feature : GenerationStep.Feature.values()) {
			if (feature.equals(GenerationStep.Feature.LAKES)) {
				continue;
			}
			try {
				biome.generateFeatureStep(feature, accessor, this, region, populationSeed, this.chunkRandom,
						chunkCenter);
			} catch (Exception exception) {
				CrashReport crashReport = CrashReport.create(exception, "Biosphere Biome Decoration");
				crashReport.addElement("Generation").add("CenterX", chunkCenter.getX())
						.add("CenterZ", chunkCenter.getZ()).add("Step", feature).add("Seed", populationSeed)
						.add("Biome", Registry.BIOME.getId(biome));
				throw new CrashException(crashReport);
			}
		}
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
				chunkCenter.getX() + 8, 256, chunkCenter.getZ() + 8)) {
			current.set(pos);
			double radialDistance = Math.sqrt(pos.getSquaredDistance(centerPos));
			BlockState blockState = Blocks.AIR.getDefaultState();
			if (radialDistance <= this.sphereRadius - 1) {
				continue;
			} else if (radialDistance <= this.sphereRadius) {
				if (pos.getY() >= centerPos.getY()) {
					blockState = Blocks.GLASS.getDefaultState();
//				} else if (pos.getY() < centerPos.getY() - this.sphereRadius / 2) {
//					continue;
				} else {// if(!(region.getBlockState(pos).equals(Blocks.STONE.getDefaultState()))){
					blockState = Blocks.STONE.getDefaultState();
				}
			} else {
				if (pos.getY() == 255) {
					this.makeBridges(pos, centerPos, this.getClosestSpheres(centerPos), region, current);
					continue;
				}
			}
			region.setBlockState(current.set(pos), blockState, 0);
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
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()), region,
									current);
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
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()), region,
									current);
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
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()), region,
									current);
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
									new BlockPos(pos.getX(), slope * currentPos + centerPos.getY(), pos.getZ()), region,
									current);
						}
					}
					break;
				}
			}
		}
	}

	public void fillBridgeSlice(BlockPos pos, ChunkRegion region, BlockPos.Mutable current) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		region.setBlockState(current.set(x, y - 1, z), this.defaultBridge, 0);
		region.setBlockState(current.set(x, y, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 1, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 2, z), Blocks.AIR.getDefaultState(), 0);
		region.setBlockState(current.set(x, y + 3, z), Blocks.AIR.getDefaultState(), 0);
	}

//	@Override
//	public void populateEntities(ChunkRegion region) {
//	}
}
