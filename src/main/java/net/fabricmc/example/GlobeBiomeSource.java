package net.fabricmc.example;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.fabricmc.fabric.api.biomes.v1.OverworldBiomes;
import net.fabricmc.fabric.impl.biome.InternalBiomeData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.layer.BiomeLayers;
import net.minecraft.world.biome.source.BiomeLayerSampler;
import net.minecraft.world.biome.source.BiomeSource;

public class GlobeBiomeSource extends BiomeSource {

	protected final long seed;
	protected final int squareSize;
	protected final int curveSize;
	private final BiomeLayerSampler biomeSampler;
	// all overworld biomes should generate on the planet!
	private static final List<Biome> BIOMES = ImmutableList.<Biome>of(Biomes.OCEAN, Biomes.PLAINS, Biomes.DESERT,
			Biomes.MOUNTAINS, Biomes.FOREST, Biomes.TAIGA, Biomes.SWAMP, Biomes.RIVER, Biomes.FROZEN_OCEAN,
			Biomes.FROZEN_RIVER, Biomes.SNOWY_TUNDRA, Biomes.SNOWY_MOUNTAINS, Biomes.MUSHROOM_FIELDS,
			Biomes.MUSHROOM_FIELD_SHORE, Biomes.BEACH, Biomes.DESERT_HILLS, Biomes.WOODED_HILLS, Biomes.TAIGA_HILLS,
			Biomes.MOUNTAIN_EDGE, Biomes.JUNGLE, Biomes.JUNGLE_HILLS, Biomes.JUNGLE_EDGE, Biomes.DEEP_OCEAN,
			Biomes.STONE_SHORE, Biomes.SNOWY_BEACH, Biomes.BIRCH_FOREST, Biomes.BIRCH_FOREST_HILLS, Biomes.DARK_FOREST,
			Biomes.SNOWY_TAIGA, Biomes.SNOWY_TAIGA_HILLS, Biomes.GIANT_TREE_TAIGA, Biomes.GIANT_TREE_TAIGA_HILLS,
			Biomes.WOODED_MOUNTAINS, Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.BADLANDS,
			Biomes.WOODED_BADLANDS_PLATEAU, Biomes.BADLANDS_PLATEAU, Biomes.WARM_OCEAN, Biomes.LUKEWARM_OCEAN,
			Biomes.COLD_OCEAN, Biomes.DEEP_WARM_OCEAN, Biomes.DEEP_LUKEWARM_OCEAN, Biomes.DEEP_COLD_OCEAN,
			Biomes.DEEP_FROZEN_OCEAN, Biomes.SUNFLOWER_PLAINS, Biomes.DESERT_LAKES, Biomes.GRAVELLY_MOUNTAINS,
			Biomes.FLOWER_FOREST, Biomes.TAIGA_MOUNTAINS, Biomes.SWAMP_HILLS, Biomes.ICE_SPIKES, Biomes.MODIFIED_JUNGLE,
			Biomes.MODIFIED_JUNGLE_EDGE, Biomes.TALL_BIRCH_FOREST, Biomes.TALL_BIRCH_HILLS, Biomes.DARK_FOREST_HILLS,
			Biomes.SNOWY_TAIGA_MOUNTAINS, Biomes.GIANT_SPRUCE_TAIGA, Biomes.GIANT_SPRUCE_TAIGA_HILLS,
			Biomes.MODIFIED_GRAVELLY_MOUNTAINS, Biomes.SHATTERED_SAVANNA, Biomes.SHATTERED_SAVANNA_PLATEAU,
			Biomes.ERODED_BADLANDS, Biomes.MODIFIED_WOODED_BADLANDS_PLATEAU, Biomes.MODIFIED_BADLANDS_PLATEAU);
	public static final Codec<GlobeBiomeSource> CODEC = RecordCodecBuilder.create((instance) -> instance
			.group(Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed),
					Codec.INT.fieldOf("square_size").forGetter((generator) -> generator.squareSize),
					Codec.INT.fieldOf("curve_size").forGetter((generator) -> generator.curveSize))
			.apply(instance, instance.stable(GlobeBiomeSource::new)));

	public GlobeBiomeSource(long seed, int squareSize, int curveSize) {
		super(GlobeBiomeSource.BIOMES);
		
//		GlobeBiomeSource.BIOMES.addAll(InternalBiomeData.getOverworldModdedContinentalBiomePickers().);
		this.seed = seed;
		this.squareSize = squareSize;
		this.curveSize = curveSize;
		this.biomeSampler = BiomeLayers.build(seed, false, 2, 4);
	}

	@Override
	public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
//		if(biomeX < 0) biomeX++;
//		if(biomeZ < 0) biomeZ++;
		if(Math.abs(biomeX)-6 > ((squareSize+curveSize)/8) || Math.abs(biomeZ)-6 > ((squareSize+curveSize)/8)) 
////		System.out.println("AAAAAAAAAA");
			return Biomes.THE_VOID;
		return this.biomeSampler.sample(biomeX, biomeZ);
//		return Biomes.OCEAN;
	}

	@Override
	protected Codec<? extends BiomeSource> method_28442() {
		// TODO Auto-generated method stub
		return GlobeBiomeSource.CODEC;
	}

	@Override
	public BiomeSource withSeed(long seed) {
		// TODO Auto-generated method stub
		return new GlobeBiomeSource(seed, this.squareSize, this.curveSize);
	}
}
