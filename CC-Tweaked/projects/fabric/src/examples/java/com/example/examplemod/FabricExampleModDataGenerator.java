package com.example.examplemod;

import com.example.examplemod.data.TurtleDataProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.data.DataProvider;

/**
 * The data generator entrypoint for our Fabric example mod.
 */
public class FabricExampleModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        pack.addProvider((DataProvider.Factory<?>) TurtleDataProvider::new);
    }
}
