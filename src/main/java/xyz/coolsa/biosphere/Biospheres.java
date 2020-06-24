package xyz.coolsa.biosphere;

import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class Biospheres implements ModInitializer {
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("coolsa","biosphere"), BiospheresChunkGenerator.CODEC);
		Registry.register(Registry.BIOME_SOURCE, new Identifier("coolsa","biosphere_biomes"), BiospheresBiomeSource.CODEC);
		System.out.println("Loaded Biospheres Mod!");
	}
}
