package xyz.coolsa.biosphere;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableBiMap.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryLookupCodec;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Pair;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
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
//			RecordCodecBuilder.create((instance) -> instance
//			.group(
//					//look up the actual registry for all the biomes, currently not configurable or customizable...
//					RegistryLookupCodec.of(Registry.BIOME_KEY).forGetter((generator) -> generator.registry),
//					//and also we want the seed of this thing, so we can go ahead and generate based on that.
//					Codec.LONG.fieldOf("seed").forGetter((generator) -> generator.seed)
//			).apply(instance, instance.stable(BiospheresBiomeSource::new)));
//	public static final MapCodec<BiospheresChunkGenerator> CUSTOM_CODEC = RecordCodecBuilder
//	.mapCodec(instance -> ((RecordCodecBuilder.Instance) instance).group(
//			(App) Codec.LONG.fieldOf("seed").forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.seed),
//			(App) RecordCodecBuilder
//					.create(instance -> instance.group(
//							(App) Biome.MixedNoisePoint.CODEC.fieldOf("parameters")
//									.forGetter((Function) Pair::getFirst),
//							(App) Biome.REGISTRY_CODEC.fieldOf("biome").forGetter((Function) Pair::getSecond))
//							.apply((Applicative) instance, (BiFunction) Pair::of))
//					.listOf().fieldOf("biomes")
//					.forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.biomePoints),
//			(App) MultiNoiseBiomeSource.NoiseParameters.CODEC.fieldOf("temperature_noise")
//					.forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.temperatureNoiseParameters),
//			(App) MultiNoiseBiomeSource.NoiseParameters.CODEC.fieldOf("humidity_noise")
//					.forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.humidityNoiseParameters),
//			(App) MultiNoiseBiomeSource.NoiseParameters.CODEC.fieldOf("altitude_noise")
//					.forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.altitudeNoiseParameters),
//			(App) MultiNoiseBiomeSource.NoiseParameters.CODEC.fieldOf("weirdness_noise")
//					.forGetter(multiNoiseBiomeSource -> multiNoiseBiomeSource.weirdnessNoiseParameters))
//			.apply(instance, MultiNoiseBiomeSource::new));
//public static final Codec<BiospheresChunkGenerator> CODEC2 = Codec
//	.mapEither((MapCodec) MultiNoiseBiomeSource.Instance.CODEC, (MapCodec) MultiNoiseBiomeSource.CUSTOM_CODEC)
//	.xmap(either -> (MultiNoiseBiomeSource) either
//			.map((Function) MultiNoiseBiomeSource.Instance::getBiomeSource, (Function) Function.identity()),
//			multiNoiseBiomeSource -> multiNoiseBiomeSource.getInstance().map(Either::left)
//					.orElseGet(() -> Either.right(multiNoiseBiomeSource)))
//	.codec();
//public static final MapCodec<BiospheresChunkGenerator> CODEC3 = RecordCodecBuilder
//	.mapCodec(
//			instance -> ((RecordCodecBuilder.Instance) instance)
//					.group((App) Identifier.CODEC
//							.flatXmap(
//									identifier -> Optional
//											.ofNullable(
//													MultiNoiseBiomeSource.Preset.BY_IDENTIFIER.get(identifier))
//											.map(DataResult::success)
//											.orElseGet(() -> DataResult.error("Unknown preset: " + identifier)),
//									preset -> DataResult.success(preset.id))
//							.fieldOf("preset").stable()
//							.forGetter((Function) MultiNoiseBiomeSource.Instance::getPreset),
//							(App) RegistryLookupCodec.of(Registry.BIOME_KEY)
//									.forGetter((Function) MultiNoiseBiomeSource.Instance::getBiomeRegistry),
//							(App) Codec.LONG.fieldOf("seed").stable()
//									.forGetter((Function) MultiNoiseBiomeSource.Instance::getSeed))
//					.apply(instance, ((RecordCodecBuilder.Instance) instance)
//							.stable(MultiNoiseBiomeSource.Instance::new)));
	protected final long seed;
	protected final int sphereDistance;
	protected final int sphereRadius;
	protected final ChunkRandom chunkRandom;
	protected final Registry<Biome> registry;
	protected final List<Pair<Long, RegistryKey<Biome>>> BIOMES = ImmutableList.<Pair<Long, RegistryKey<Biome>>>of(
			new Pair<Long, RegistryKey<Biome>>(1l, BiomeKeys.PLAINS),
			new Pair<Long, RegistryKey<Biome>>(1l, BiomeKeys.FOREST),
			new Pair<Long, RegistryKey<Biome>>(1l, BiomeKeys.BADLANDS),
			new Pair<Long, RegistryKey<Biome>>(1l, BiomeKeys.TAIGA)
			);
	private final List<RegistryKey<Biome>> BIOME_TABLE; //for a alias method randomness thing... yea its bad.
	private final int BIOME_COUNT;

	public static final MapCodec<BiospheresBiomeSource> CUSTOM_CODEC = RecordCodecBuilder.mapCodec(builder -> builder
			.group(
					// look up the actual registry for all the biomes, currently not configurable or
					// customizable...
					RegistryLookupCodec.of(Registry.BIOME_KEY).stable().forGetter((generator) -> generator.registry),
					// and also we want the seed of this thing, so we can go ahead and generate
					// based on that.
					Codec.LONG.fieldOf("seed").stable().forGetter((generator) -> generator.seed),
					Codec.INT.optionalFieldOf("sphere_distance", 128).stable()
							.forGetter(getter -> getter.sphereDistance),
					Codec.INT.optionalFieldOf("sphere_radius", 32).stable().forGetter(getter -> getter.sphereRadius))
			.apply(builder, BiospheresBiomeSource::new));
	public static final Codec<BiospheresBiomeSource> CODEC = CUSTOM_CODEC.codec();
////	protected final long seed;
////	protected final int squareSize 
////	protected final int curveSize;
////	private final BiomeLayerSampler biomeSampler;
	// for the biomes, should i just clone every single one, register them as
	// "biosphere:*biome*", and then generate with those?
	// so we can do ores and stuff? that seems to be one of the better ideas i would
	// say...

////	public static final Codec<BiosphereBiomeSource> CODEC = Codec.mapPair(Identifier.CODEC.flatXmap(
////			identifier -> Optional.<MultiNoiseBiomeSource.Preset>ofNullable(this.Preset.field_24724.get(identifier))
////					.map(DataResult::success).orElseGet(() -> DataResult.error("Unknown preset: " + identifier)),
//BuiltinRegistries.BIOME.get(BuiltinBiomesreset -> DataResult.success(preset.id)).fieldOf("preset"), Codec.LONG.fieldOf("seed")).stable();
//
	protected BiospheresBiomeSource(Registry<Biome> registry, long seed, int sphereDistance, int sphereRadius) {
		super(ImmutableList.of());
		this.seed = seed;
		this.sphereDistance = sphereDistance;
		this.sphereRadius = sphereRadius;
		this.chunkRandom = new ChunkRandom(seed);
		this.registry = registry;
		this.BIOME_TABLE = new ArrayList<RegistryKey<Biome>>();
		for(Pair<Long,RegistryKey<Biome>> p : BIOMES) {
			Long size = p.getFirst();
			for(Long i = 0l; i.compareTo(size)<0; i++ ) {
				this.BIOME_TABLE.add(p.getSecond()); //write the position of the biome in the biome list to a position.
			}
		}
		this.BIOME_COUNT = this.BIOME_TABLE.size();
		// TODO Auto-generated constructor stub
	}

	/**
	 * When we get a biomeX, biomeY, or biomeZ, it is actually 1/4x1/4x1/4 of a
	 * 16x16x16 block "chunk" of the world.
	 */
	@Override
	public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
//		if(biomeX < 0) biomeX++;
//		if(biomeZ < 0) biomeZ++;
//		if (Math.abs(biomeX) - 6 > ((squareSize + curveSize) / 8)
//				|| Math.abs(biomeZ) - 6 > ((squareSize + curveSize) / 8))
////		System.out.println("AAAAAAAAAA");

//		BlockPos centerPos 
		if (this.getDistanceFromSphere(biomeX + 1, biomeZ + 1) < this.sphereRadius + 6) {
			return registry.get(this.getBiomeForSphere(biomeX, biomeZ));
		}
//		return BiomeSelectors.vanilla().;
//		return this.biomeSampler.sample(biomeX, biomeZ);
//		return Biomes.OCEAN;
		return registry.get(BiomeKeys.THE_VOID);
	}

	public double getDistanceFromSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		BlockPos center = new BlockPos(centerX, 0, centerZ);
		return Math.sqrt(center.getSquaredDistance(biomeX * 4, 0, biomeZ * 4, true));
	}

	public RegistryKey<Biome> getBiomeForSphere(int biomeX, int biomeZ) {
		int centerX = (int) Math.round(biomeX * 4 / (double) this.sphereDistance) * this.sphereDistance;
		int centerZ = (int) Math.round(biomeZ * 4 / (double) this.sphereDistance) * this.sphereDistance;
		this.chunkRandom.setTerrainSeed(centerX, centerZ);
		int randomChoice = this.chunkRandom.nextInt(this.BIOME_COUNT); //from the size of our lookup table, select one biome (weighted)
		return this.BIOME_TABLE.get(randomChoice); //get from this class's lookup table the biome we need
	}

	@Override
	public BiomeSource withSeed(long seed) {
		return new BiospheresBiomeSource(this.registry, seed, this.sphereDistance, this.sphereRadius);
		// TODO Auto-generated method stub
//		return new BiosphereBiomeSource(seed, this.squareSize, this.curveSize);
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		// TODO Auto-generated method stub
		return BiospheresBiomeSource.CODEC;
	}
}
