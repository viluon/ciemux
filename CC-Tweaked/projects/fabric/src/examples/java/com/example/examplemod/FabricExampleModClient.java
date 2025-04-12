package com.example.examplemod;

import dan200.computercraft.api.client.FabricComputerCraftAPIClient;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModeller;
import net.fabricmc.api.ClientModInitializer;

public class FabricExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // @start region=turtle_modellers
        FabricComputerCraftAPIClient.registerTurtleUpgradeModeller(ExampleMod.EXAMPLE_TURTLE_UPGRADE, TurtleUpgradeModeller.flatItem());
        // @end region=turtle_modellers
    }
}
