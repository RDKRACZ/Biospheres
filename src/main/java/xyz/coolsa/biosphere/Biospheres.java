package xyz.coolsa.biosphere;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.world.GeneratorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.VanillaLayeredBiomeSource;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import xyz.coolsa.biosphere.mixin.GeneratorTypeMixin;

public class Biospheres implements ModInitializer {
	public static BiosphereConfig config = new BiosphereConfig(192, 48, 5, 12);
	public static final GeneratorType BioSphere = new GeneratorType("biosphere") {
		@Override
		protected ChunkGenerator getChunkGenerator(Registry<Biome> biomeRegistry, Registry<ChunkGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
			return new BiospheresChunkGenerator(new BiospheresBiomeSource(seed), seed, config.sphereDistance, config.sphereRadius, config.lakeRadius, config.shoreRadius);
		}
	};
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("coolsa","biosphere"), BiospheresChunkGenerator.CODEC);
		Registry.register(Registry.BIOME_SOURCE, new Identifier("coolsa","biosphere_biomes"), BiospheresBiomeSource.CODEC);
		GeneratorTypeMixin.getValues().add(BioSphere);
		System.out.println("Loaded Biospheres Mod!");
	}
}
