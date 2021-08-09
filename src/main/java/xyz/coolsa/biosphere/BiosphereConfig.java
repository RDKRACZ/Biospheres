package xyz.coolsa.biosphere;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.world.GeneratorType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BiosphereConfig {
    public int sphereDistance;
    public int sphereRadius;
    public int shoreRadius;
    public int lakeRadius;

    public BiosphereConfig(int sd, int spr, int shr, int lr) {
        this.sphereDistance = sd;
        this.sphereRadius = spr;
        this.shoreRadius = shr;
        this.lakeRadius = lr;
    }
}
