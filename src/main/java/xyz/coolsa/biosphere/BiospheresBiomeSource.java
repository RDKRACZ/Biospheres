package xyz.coolsa.biosphere;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import com.mojang.datafixers.util.Pair;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.BuiltinBiomes;
//import net.minecraft.world.biome.Biomes;
//import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.layer.BiomeLayers;
import net.minecraft.world.biome.source.BiomeLayerSampler;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.MultiNoiseBiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;

public class BiospheresBiomeSource extends BiomeSource {

	public static final Codec<BiospheresBiomeSource> CODEC = RecordCodecBuilder
			.create((instance) -> instance.group(Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed))
					.apply(instance, instance.stable(BiospheresBiomeSource::new)));
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final ChunkRandom chunkRandom;
////	protected final long seed;
////	protected final int squareSize;
////	protected final int curveSize;
////	private final BiomeLayerSampler biomeSampler;
	protected static final List<Biome> BIOMES = ImmutableList.<Biome>of(BuiltinBiomes.PLAINS,
//			BuiltinBiomes.PLAINS
			BuiltinRegistries.BIOME.get(BiomeKeys.OCEAN), BuiltinRegistries.BIOME.get(BiomeKeys.DESERT),
			BuiltinRegistries.BIOME.get(BiomeKeys.MOUNTAINS), BuiltinRegistries.BIOME.get(BiomeKeys.FOREST),
			BuiltinRegistries.BIOME.get(BiomeKeys.TAIGA), BuiltinRegistries.BIOME.get(BiomeKeys.SWAMP),
			BuiltinRegistries.BIOME.get(BiomeKeys.RIVER), BuiltinRegistries.BIOME.get(BiomeKeys.FROZEN_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.FROZEN_RIVER),
			BuiltinRegistries.BIOME.get(BiomeKeys.MUSHROOM_FIELDS),
			BuiltinRegistries.BIOME.get(BiomeKeys.MUSHROOM_FIELD_SHORE),
			BuiltinRegistries.BIOME.get(BiomeKeys.BEACH), BuiltinRegistries.BIOME.get(BiomeKeys.DESERT_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.WOODED_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.TAIGA_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.MOUNTAIN_EDGE), BuiltinRegistries.BIOME.get(BiomeKeys.JUNGLE),
			BuiltinRegistries.BIOME.get(BiomeKeys.JUNGLE_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.JUNGLE_EDGE),
			BuiltinRegistries.BIOME.get(BiomeKeys.DEEP_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.STONE_SHORE),
			BuiltinRegistries.BIOME.get(BiomeKeys.BIRCH_FOREST),
			BuiltinRegistries.BIOME.get(BiomeKeys.BIRCH_FOREST_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.DARK_FOREST),
			BuiltinRegistries.BIOME.get(BiomeKeys.GIANT_TREE_TAIGA),
			BuiltinRegistries.BIOME.get(BiomeKeys.GIANT_TREE_TAIGA_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.WOODED_MOUNTAINS),
			BuiltinRegistries.BIOME.get(BiomeKeys.SAVANNA),
			BuiltinRegistries.BIOME.get(BiomeKeys.SAVANNA_PLATEAU),
			BuiltinRegistries.BIOME.get(BiomeKeys.BADLANDS),
			BuiltinRegistries.BIOME.get(BiomeKeys.WOODED_BADLANDS_PLATEAU),
			BuiltinRegistries.BIOME.get(BiomeKeys.BADLANDS_PLATEAU),
			BuiltinRegistries.BIOME.get(BiomeKeys.WARM_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.LUKEWARM_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.COLD_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.DEEP_WARM_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.DEEP_LUKEWARM_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.DEEP_COLD_OCEAN),
			BuiltinRegistries.BIOME.get(BiomeKeys.SUNFLOWER_PLAINS),
			BuiltinRegistries.BIOME.get(BiomeKeys.DESERT_LAKES),
			BuiltinRegistries.BIOME.get(BiomeKeys.GRAVELLY_MOUNTAINS),
			BuiltinRegistries.BIOME.get(BiomeKeys.FLOWER_FOREST),
			BuiltinRegistries.BIOME.get(BiomeKeys.TAIGA_MOUNTAINS),
			BuiltinRegistries.BIOME.get(BiomeKeys.SWAMP_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.ICE_SPIKES),
			BuiltinRegistries.BIOME.get(BiomeKeys.MODIFIED_JUNGLE),
			BuiltinRegistries.BIOME.get(BiomeKeys.MODIFIED_JUNGLE_EDGE),
			BuiltinRegistries.BIOME.get(BiomeKeys.TALL_BIRCH_FOREST),
			BuiltinRegistries.BIOME.get(BiomeKeys.TALL_BIRCH_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.DARK_FOREST_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.GIANT_SPRUCE_TAIGA),
			BuiltinRegistries.BIOME.get(BiomeKeys.GIANT_SPRUCE_TAIGA_HILLS),
			BuiltinRegistries.BIOME.get(BiomeKeys.MODIFIED_GRAVELLY_MOUNTAINS),
			BuiltinRegistries.BIOME.get(BiomeKeys.SHATTERED_SAVANNA),
			BuiltinRegistries.BIOME.get(BiomeKeys.SHATTERED_SAVANNA_PLATEAU),
			BuiltinRegistries.BIOME.get(BiomeKeys.ERODED_BADLANDS),
			BuiltinRegistries.BIOME.get(BiomeKeys.MODIFIED_WOODED_BADLANDS_PLATEAU),
			BuiltinRegistries.BIOME.get(BiomeKeys.MODIFIED_BADLANDS_PLATEAU)
			);

////	public static final Codec<BiosphereBiomeSource> CODEC = Codec.mapPair(Identifier.CODEC.flatXmap(
////			identifier -> Optional.<MultiNoiseBiomeSource.Preset>ofNullable(this.Preset.field_24724.get(identifier))
////					.map(DataResult::success).orElseGet(() -> DataResult.error("Unknown preset: " + identifier)),
//BuiltinRegistries.BIOME.get(BuiltinBiomesreset -> DataResult.success(preset.id)).fieldOf("preset"), Codec.LONG.fieldOf("seed")).stable();
//
	protected BiospheresBiomeSource(long seed) {
		super(BIOMES);
		this.seed = seed;
		this.sphereDistance = 128;
		this.sphereRadius = 32;
		this.chunkRandom = new ChunkRandom(seed);
		// TODO Auto-generated constructor stub
	}

	@Override
	public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
//		if(biomeX < 0) biomeX++;
//		if(biomeZ < 0) biomeZ++;
//		if (Math.abs(biomeX) - 6 > ((squareSize + curveSize) / 8)
//				|| Math.abs(biomeZ) - 6 > ((squareSize + curveSize) / 8))
////		System.out.println("AAAAAAAAAA");

//		BlockPos centerPos 
		if (this.getDistanceFromSphere(biomeX + 1, biomeZ + 1) < this.sphereRadius + 6) {
			return this.getBiomeForSphere(biomeX, biomeZ);
		}
		return BuiltinBiomes.THE_VOID;
//		return this.biomeSampler.sample(biomeX, biomeZ);
//		return Biomes.OCEAN;
	}

	public double getDistanceFromSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		BlockPos center = new BlockPos(centerX, 0, centerZ);
		return Math.sqrt(center.getSquaredDistance(biomeX * 4, 0, biomeZ * 4, true));
	}

	public Biome getBiomeForSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int randomChoice = this.chunkRandom.nextInt(BiospheresBiomeSource.BIOMES.size());
		return BiospheresBiomeSource.BIOMES.get(randomChoice);
	}

	@Override
	public BiomeSource withSeed(long seed) {
		return new BiospheresBiomeSource(seed);
		// TODO Auto-generated method stub
//		return new BiosphereBiomeSource(seed, this.squareSize, this.curveSize);
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		// TODO Auto-generated method stub
		return BiospheresBiomeSource.CODEC;
	}
}
