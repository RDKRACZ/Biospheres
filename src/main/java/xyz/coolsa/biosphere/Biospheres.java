package xyz.coolsa.biosphere;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.decorator.RangeDecoratorConfig;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.heightprovider.UniformHeightProvider;
import xyz.coolsa.biosphere.mixin.GeneratorTypeMixin;

public class Biospheres implements ModInitializer {
	public static BiosphereConfig config = new BiosphereConfig(48, 5, 12);
	public static final GeneratorType BioSphere = new GeneratorType("biosphere") {
		@Override
		protected ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
			return new BiospheresChunkGenerator(new BiospheresBiomeSource(biomeRegistry, seed), seed, config.sphereDistance, config.sphereRadius, config.lakeRadius, config.shoreRadius);
		}
	};

	//public static ConfiguredFeature<?, ?> ORE_IRON_BIOSPHERE = Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, Blocks.IRON_ORE.getDefaultState(), 9)).range(new RangeDecoratorConfig(UniformHeightProvider.create(YOffset.aboveBottom(0), YOffset.fixed(192)))).spreadHorizontally().repeat(35);
	//public static RegistryKey<ConfiguredFeature<?, ?>> oreIronBiosphere = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY, new Identifier("biosphere", "ore_iron_biosphere"));

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("coolsa","biosphere"), BiospheresChunkGenerator.CODEC);
		Registry.register(Registry.BIOME_SOURCE, new Identifier("coolsa","biosphere_biomes"), BiospheresBiomeSource.CODEC);

		//Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, oreIronBiosphere.getValue(), ORE_IRON_BIOSPHERE);

		GeneratorTypeMixin.getValues().add(BioSphere);
		System.out.println("Loaded Biospheres Mod!");
	}
}
