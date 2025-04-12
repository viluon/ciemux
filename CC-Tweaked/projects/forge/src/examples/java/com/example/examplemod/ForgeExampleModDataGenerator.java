package com.example.examplemod;

import com.example.examplemod.data.ExampleModDataGenerators;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The data generator entrypoint for the Forge version of our example mod.
 *
 * @see ExampleModDataGenerators The main implementation
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeExampleModDataGenerator {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        ExampleModDataGenerators.run(event.getGenerator().getVanillaPack(true));
    }
}
