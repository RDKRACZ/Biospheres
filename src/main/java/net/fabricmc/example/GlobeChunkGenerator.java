package net.fabricmc.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;
import java.util.stream.IntStream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.noise.NoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.Heightmap;

public class GlobeChunkGenerator extends ChunkGenerator {
	protected final long seed;
	protected final int squareSize;
	protected final int curveSize;
	protected final int oceanHeight;
	// globe side, 0 is top, 1 2 3 4 are n e s w, then 5 is bottom. (looking at top
	// face of globe)
	protected final int globeSide;
	protected final ChunkRandom random;
	protected final BlockState defaultBlock;
	protected final BlockState defaultFluid;
	protected final int worldHeight = 128;
	protected final double vertNoiseScale = 4;
	protected final double horzNoiseScale = 8;
	private final OctavePerlinNoiseSampler surfaceDepthNoise;
	private final BlockPos strongholdPos;

	public static final Codec<GlobeChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(BiomeSource.field_24713.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
					Codec.INT.fieldOf("square_size").forGetter((generator) -> generator.squareSize),
					Codec.INT.fieldOf("globe_side").forGetter((generator) -> generator.globeSide),
					Codec.INT.fieldOf("curve_size").forGetter((generator) -> generator.curveSize))
			.apply(instance, instance.stable(GlobeChunkGenerator::new)));

//	public static final TestGenerator INSTANCE = new TestGenerator();
	public GlobeChunkGenerator(BiomeSource biomeSource, long seed, int squareSize, int globeSide, int curveSize) {
		super(biomeSource, new StructuresConfig(false));
		this.squareSize = squareSize;
		this.curveSize = curveSize;
		this.oceanHeight = 64;
		this.seed = seed;
		this.globeSide = globeSide;
		this.random = new ChunkRandom(this.seed);
		this.defaultBlock = Blocks.STONE.getDefaultState();
		this.defaultFluid = Blocks.WATER.getDefaultState();
		System.out.println("AAAAAAAAAA");
		this.random.consume(2*this.globeSide);
		this.strongholdPos = new BlockPos(this.random.nextInt((this.globeSide/2))-this.globeSide/4,this.worldHeight/8,this.random.nextInt((this.globeSide/2))-this.globeSide/4);
		this.surfaceDepthNoise = new OctavePerlinNoiseSampler(this.random, IntStream.rangeClosed(-3, 0));
	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
//        ChunkPos chunkPos4 = chunk.getPos();
//        int integer5 = chunkPos4.x;
//        int integer6 = chunkPos4.z;
//        ChunkRandom chunkRandom7 = new ChunkRandom();
//        chunkRandom7.setTerrainSeed(integer5, integer6);
//        ChunkPos chunkPos8 = chunk.getPos();
//        int integer9 = chunkPos8.getStartX();
//        int integer10 = chunkPos8.getStartZ();
//        double double11 = 0.0625;
//        BlockPos.Mutable mutable13 = new BlockPos.Mutable();
//        for (int m = 0; m < 16; ++m) {
//            for (int n = 0; n < 16; ++n) {
//                int integer16 = integer9 + m;
//                int integer17 = integer10 + n;
//                int integer18 = chunk.sampleHeightmap(Type.WORLD_SURFACE_WG, m, n) + 1;
//                double double19 = this.surfaceDepthNoise.sample(integer16 * double11, integer17 * double11, double11, m * double11) * 15.0;
//                region.getBiome(mutable13.set(integer9 + m, integer18, integer10 + n)).buildSurface(chunkRandom7, chunk, integer16, integer17, integer18, double19, this.defaultBlock, this.defaultFluid, this.getSeaLevel(integer9 + m, integer10 + n), region.getSeed());
//            }
//        }
		ChunkPos chunkPos = chunk.getPos();
		ChunkRandom chunkRandom = new ChunkRandom();
		BlockPos.Mutable current = new BlockPos.Mutable();
		chunkRandom.setTerrainSeed(chunkPos.getStartX(), chunkPos.getStartZ());
		int xPos = chunk.getPos().getStartX();
		int zPos = chunk.getPos().getStartZ();
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 0, zPos + 15)) {
			int heightMap = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, pos.getX(), pos.getZ());
//            int heightMap = 100;// chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, pos.getX(), pos.getZ()) + 1;
			double depthNoise = this.surfaceDepthNoise.sample(pos.getX() * 0.0625, pos.getZ() * 0.0625, 0.0625,
					(pos.getX() - xPos) * 0.0625) * 15.0;
			region.getBiome(current.set(pos.getX(), heightMap, pos.getZ())).buildSurface(chunkRandom, chunk, pos.getX(),
					pos.getZ(), heightMap, depthNoise, this.defaultBlock, this.defaultFluid,
					this.getSeaLevel(pos.getX(), pos.getZ()), region.getSeed());

		}

		this.genBedrock(chunk);
	}

	private void genBedrock(Chunk chunk) {
		// lets work with some blocks
		BlockPos.Mutable current = new BlockPos.Mutable();
		// current chunk position.
		int xPos = chunk.getPos().getStartX();
		int zPos = chunk.getPos().getStartZ();
		// loop through all x-z blocks in this chunk.
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 0, zPos + 15)) {
			// we are currently at height 0.
			double height = 0;
			if (Math.abs(pos.getX()) <= (this.squareSize / 2) && Math.abs(pos.getZ()) <= (this.squareSize / 2)) {
				height = 1;// if we have no height yet, lets just assume we are at 1, because its there
							// now.
				chunk.setBlockState(current.set(pos.getX(), 0, pos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
			}
			if (Math.abs(pos.getX()) <= this.squareSize / 2 + curveSize / 4
					&& Math.abs(pos.getZ()) <= this.squareSize / 2 + curveSize / 4) {
				int xDist = Math.abs(pos.getX()) - (squareSize / 2 - curveSize / 2);
				int zDist = Math.abs(pos.getZ()) - (squareSize / 2 - curveSize / 2);
				if (xDist >= 0)
					height += xDist * xDist;
				if (zDist >= 0)
					height += zDist * zDist;
				height = (int) Math.sqrt(height);
				if (height > this.curveSize / 2) {
					height = (this.curveSize * 3 / 4 - height) * (oceanHeight * 12 / (5 * curveSize));
					for (int i = 0; i <= height; i++) {
						chunk.setBlockState(current.set(pos.getX(), i, pos.getZ()), Blocks.BEDROCK.getDefaultState(),
								false);
					}
				} else {
					height *= (oceanHeight * 6 / (5 * curveSize));
					for (int i = 0; i <= height; i++) {
						chunk.setBlockState(current.set(pos.getX(), i, pos.getZ()), Blocks.BEDROCK.getDefaultState(),
								false);
					}
				}
			}
			// loop to create noisy bottom layer, like regular game has.
			for (int spot = 3; spot > 0 && height > 0; spot--) {
				if (spot <= random.nextInt(5)) {
					chunk.setBlockState(current.set(pos.getX(), spot + height, pos.getZ()),
							Blocks.BEDROCK.getDefaultState(), false);
				}
			}
		}
	}

	@Override
	public BlockView getColumnSample(int x, int z) {
		return new VerticalBlockSample(new BlockState[0]);
	}

	@Override
	public int getHeight(int x, int z, Type heightmapType) {
		return 64;
	}

	@Override
	protected Codec<? extends ChunkGenerator> method_28506() {
		return GlobeChunkGenerator.CODEC;
	}

	@Override
	public void populateNoise(WorldAccess world, StructureAccessor accessor, Chunk chunk) {
		ChunkPos chunkPos = chunk.getPos();
		ChunkRandom chunkRandom = new ChunkRandom();
		BlockPos.Mutable current = new BlockPos.Mutable();
		chunkRandom.setTerrainSeed(chunkPos.getStartX(), chunkPos.getStartZ());
//		Heightmap oceanFloor = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
//		Heightmap surfaceHeight = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
		int xPos = chunk.getPos().getStartX();
		int zPos = chunk.getPos().getStartZ();
		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, worldHeight, zPos + 15)) {
			BlockState block = this.getBlockState(this.getNoise(pos), pos);
			chunk.setBlockState(current.set(pos), block, false);
//			if (!block.getBlock().is(Blocks.AIR) && pos.getX()>=0) {
//				oceanFloor.trackUpdate(pos.getX(), pos.getY(), pos.getZ(), block);
//				surfaceHeight.trackUpdate(pos.getX(), pos.getY(), pos.getZ(), block);
//			}
			// int heightMap = 0;
//			for (heightMap = 0; heightMap < (chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, pos.getX(),
//					pos.getZ()) + 1) * (this.getSeaLevel(pos.getX(), pos.getZ()))
//					/ (double) this.oceanHeight; heightMap++) {
//				chunk.setBlockState(current.set(pos.getX(), heightMap, pos.getZ()), Blocks.STONE.getDefaultState(),
//						false);
//			}
		}
	}

	public BlockState getBlockState(double noise, BlockPos pos) {
		return getBlockState(noise, pos.getX(), pos.getY(), pos.getZ());
	}

	public BlockState getBlockState(double noise, int x, int y, int z) {
//		if (noise > 1) {
//			return Blocks.GLASS.getDefaultState();
//		}
//		if (noise > 0.5) {
//			return Blocks.BLACK_STAINED_GLASS.getDefaultState();
//		}
		if (noise > 0.0) {
			return this.defaultBlock;
		} else if (y < this.getSeaLevel(x, z))
			return this.defaultFluid;
		return Blocks.AIR.getDefaultState();
	}

	public double getNoise(int x, int y, int z) {
		double noise = 1 - ((double) y / worldHeight) * 2;
		double biomeDepth = 0;
		double biomeScale = 0;
		double runs = 0;
//		double thisDepth = this.biomeSource.getBiomeForNoiseGen(x >> 2, y >> 2, z >> 2).getDepth();
		for (int i = x - 3; i <= x + 3; i++) {
			for (int j = z - 3; j <= z + 3; j++) {
				double noiseScale = Math.sqrt(((i - x) * (i - x)) + ((j - z) * (j - z)));
				if(noiseScale >= 2.5) continue;
				Biome currentBiome = this.biomeSource.getBiomeForNoiseGen(i >> 2, y >> 2, j >> 2);
				double depth = currentBiome.getDepth();
				noiseScale *= (depth + 2.0);
//				if (depth < thisDepth)
//					noiseScale *= 4.0;
				biomeDepth += depth * noiseScale;
				biomeScale += currentBiome.getScale() * noiseScale;
				runs += noiseScale;
			}
		}
		biomeScale /= runs;
		biomeDepth /= runs;
		noise = getNoiseColumn(x, y, z, biomeScale, biomeDepth);
//		System.out.println(this.biomeSource.getBiomeForNoiseGen(x/16, y/16, z/16));
//		biomeScale /= runs*0.9;
//		biomeDepth -= 0.125;
//		biomeScale += 0.1;
//		noise += (biomeDepth);
//		noise *=biomeScale;
		return noise;
	}

	public double getNoiseColumn(int x, int y, int z, double biomeScale, double biomeDepth) {
		double noise = 0;
//		if(biomeDepth < Biomes.OCEAN.getDepth()) {
//			biomeDepth /=16;
//		}
		noise += biomeDepth;
		noise += (biomeScale) * surfaceDepthNoise.sample(x / this.horzNoiseScale, y / this.vertNoiseScale,
				z / this.horzNoiseScale, 0, 0, false) * 8;
		noise += (((double) (this.oceanHeight) / this.worldHeight) - (((double) y) / this.worldHeight)) * 5;
//		noise *= (1+(1/((y*y)/this.oceanHeight+1)));
//		double noise = 1;
//		noise = noise*noise*noise;
//		return biomeScale*noise;
//		return (noise+biomeDepth)*(biomeScale);
		noise -=  ((((double) this.oceanHeight) / ((double) this.getSeaLevel(x, z)))-1) ;
		return noise;
	}

	public double getNoiseBiome(double noise, int x, int y, int z) {
		return 0;
	}

	public double getNoise(BlockPos pos) {
		return getNoise(pos.getX(), pos.getY(), pos.getZ());
	}

	@Override
	public ChunkGenerator withSeed(long seed) {
		return new GlobeChunkGenerator(biomeSource, seed, squareSize, globeSide, curveSize);
	}

	public int getSeaLevel(BlockPos pos) {
		return this.getSeaLevel(pos.getX(), pos.getZ());
	}

	public int getSeaLevel(int x, int z) {
		double height = 0;
		if (Math.abs(x) <= (this.squareSize / 2) - curveSize / 2
				&& Math.abs(z) <= (this.squareSize / 2) - curveSize / 2) {
			return oceanHeight;// we have not yet hit the world curve.
		} else if (Math.abs(x) <= this.squareSize / 2 + curveSize && Math.abs(z) <= this.squareSize / 2 + curveSize) {
			int xDist = Math.abs(x) - (squareSize / 2 - curveSize / 2);
			int zDist = Math.abs(z) - (squareSize / 2 - curveSize / 2);
			if (xDist >= 0)
				height += xDist * xDist;
			if (zDist >= 0)
				height += zDist * zDist;
			height = Math.sqrt(height);
			// linearize the curve. between 0 and 1.
			height = (height / (curveSize * 3 / 2));// * oceanHeight;
			height = Math.sqrt(1 - height * height) * oceanHeight;// cuuuuurrve.
		}
//		return 64;
		return (int) Math.floor(height);
	}

	public int getCarveSeaLevel(int x, int z) {
		// start at normal level, then sharply drop off.
		double height = 0;
		if (Math.abs(x) <= (this.squareSize / 2) - curveSize / 2
				&& Math.abs(z) <= (this.squareSize / 2) - curveSize / 2) {
			height = oceanHeight;
		} else if (Math.abs(x) <= this.squareSize / 2 + curveSize / 2
				&& Math.abs(z) <= this.squareSize / 2 + curveSize / 2) {
			int xDist = Math.abs(x) - (squareSize / 2 - curveSize);
			int zDist = Math.abs(z) - (squareSize / 2 - curveSize);
			if (xDist >= 0)
				height += xDist * xDist;
			if (zDist >= 0)
				height += zDist * zDist;
			height = Math.sqrt(height);
			height = (height / (curveSize * 3 / 2)) * oceanHeight;
		}
		return (int) height;
	}

//	@Override
//	public void populateBiomes(Chunk chunk) {
//
//	}
//	@Override
//	public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
//		
//	}
	@Override
	public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
		BiomeAccess biomeAccess = access.withSource(this.biomeSource);
		ChunkRandom chunkRandomCarver = new ChunkRandom();
		ChunkPos chunkPos = chunk.getPos();
		int xPos = chunkPos.x;
		int zPos = chunkPos.z;
		Biome biome = this.biomeSource.getBiomeForNoiseGen(chunkPos.x << 2, 0, chunkPos.z << 2);
		BitSet bitSet = ((ProtoChunk) chunk).method_28510(carver);
		for (int curX = xPos - 8; curX <= xPos + 8; ++curX) {
			for (int curZ = zPos - 8; curZ <= zPos + 8; ++curZ) {
				List<ConfiguredCarver<?>> carverList = biome.getCarversForStep(carver);
				ListIterator<ConfiguredCarver<?>> currentCarver = carverList.listIterator();
				while (currentCarver.hasNext()) {
					int salt = currentCarver.nextIndex();
					ConfiguredCarver<?> configuredCarver20 = currentCarver.next();
					chunkRandomCarver.setCarverSeed(seed + salt, curX, curZ);
					if (configuredCarver20.shouldCarve(chunkRandomCarver, curX, curZ)) {
						configuredCarver20.carve(chunk, biomeAccess::getBiome, chunkRandomCarver,
								this.getCarveSeaLevel(curX, curZ) - 8, curX, curZ, xPos, zPos, bitSet);
					}
				}
			}
		}
	}
	
	@Override
	public boolean method_28507(ChunkPos chunkPos) {
		ChunkPos chunk = new ChunkPos(0,0);
		if(chunkPos.equals(chunk)) return true;
		return false;
	}
	
	@Override
	public BlockPos locateStructure(ServerWorld world, StructureFeature<?> feature, BlockPos center, int radius, boolean skipExistingChunks) {
	        if (!this.biomeSource.hasStructureFeature(feature)) {
	            return null;
	        }
	        if (feature == StructureFeature.STRONGHOLD) {
	            return this.strongholdPos;
	        }
	        return null;
//	        return feature.locateStructure(world, world.getStructureAccessor(), center, radius, skipExistingChunks, world.method_8412(), this.config.method_28600(feature));
	}
}
