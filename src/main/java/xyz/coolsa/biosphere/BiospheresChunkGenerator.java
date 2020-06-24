package xyz.coolsa.biosphere;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructuresConfig;

public class BiospheresChunkGenerator extends ChunkGenerator {
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final int oreSphereRadius;
	protected final BiomeSource biomeSource;
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
		// TODO Auto-generated constructor stub
	}

	@Override
	public void buildSurface(ChunkRegion region, Chunk chunk) {
		// TODO Auto-generated method stub

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
		ChunkRandom chunkRandom = new ChunkRandom();
		int centerX = (int) Math.round(xPos / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(zPos / (double) this.sphereDistance) * this.sphereDistance;
		chunkRandom.setTerrainSeed(centerX, centerZ);
		int centerY = chunkRandom.nextInt(256 - this.sphereRadius * 4) + this.sphereRadius * 2;

		for (final BlockPos pos : BlockPos.iterate(xPos, 0, zPos, xPos + 15, 256, zPos + 15)) {
			current.set(pos);
			double radialDistance = Math.sqrt(
					(pos.getX() - centerX) * (pos.getX() - centerX) + (pos.getZ() - centerZ) * (pos.getZ() - centerZ)
							+ (pos.getY() - centerY) * (pos.getY() - centerY));
			if (radialDistance <= this.sphereRadius - 1) {
				if (pos.getY() < centerY) {
					chunk.setBlockState(current.set(pos), Blocks.STONE.getDefaultState(), false);
				}
			} else if (radialDistance <= this.sphereRadius) {
//				chunk.setBlockState(current.set(pos), Blocks.GLASS.getDefaultState(), false);
				if (pos.getY() < centerY) {
					chunk.setBlockState(current.set(pos), Blocks.OBSIDIAN.getDefaultState(), false);
				}
			}

		}
	}

//	private void genSphere(BlockPos center, BlockPos.Mutable current, Chunk chunk, long radius) {
//		
//	}

	@Override
	public ChunkGenerator withSeed(long arg0) {
		// TODO Auto-generated method stub
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
		
	}

}
