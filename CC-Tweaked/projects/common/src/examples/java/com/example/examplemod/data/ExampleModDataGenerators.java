package com.example.examplemod.data;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;

/**
 * The entry point to example mod's data-generators.
 * <p>
 * This is called by our platform-specific entry-point (see {@code FabricExampleModDataGenerator} and
 * {@code ForgeExampleModDataGenerator}. That said, the exact setup isn't relevant (it will vary depending on
 * mod-loader), what's interesting is the contents of the {@link #run(DataGenerator.PackGenerator)} method!
 */
public final class ExampleModDataGenerators {
    public static void run(DataGenerator.PackGenerator pack) {
        pack.addProvider((DataProvider.Factory<?>) TurtleDataProvider::new);
    }
}
