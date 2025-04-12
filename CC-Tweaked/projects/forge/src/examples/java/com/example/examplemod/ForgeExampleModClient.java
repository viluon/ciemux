package com.example.examplemod;

import dan200.computercraft.api.client.turtle.RegisterTurtleModellersEvent;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The client-side entry point for the Forge version of our example mod.
 */
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeExampleModClient {
    // @start region=turtle_modellers
    @SubscribeEvent
    public static void onRegisterTurtleModellers(RegisterTurtleModellersEvent event) {
        event.register(ExampleMod.EXAMPLE_TURTLE_UPGRADE, TurtleUpgradeModeller.flatItem());
    }
    // @end region=turtle_modellers
}
