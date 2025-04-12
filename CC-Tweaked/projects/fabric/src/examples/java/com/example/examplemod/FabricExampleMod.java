package com.example.examplemod;

import com.example.examplemod.peripheral.BrewingStandPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The main entry point for our example mod.
 */
public class FabricExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // @start region=turtle_upgrades
        @SuppressWarnings("unchecked")
        var turtleUpgradeSerialisers = (Registry<TurtleUpgradeSerialiser<?>>) BuiltInRegistries.REGISTRY.get(TurtleUpgradeSerialiser.registryId().location());
        Registry.register(turtleUpgradeSerialisers, new ResourceLocation(ExampleMod.MOD_ID, "example_turtle_upgrade"), ExampleMod.EXAMPLE_TURTLE_UPGRADE);
        // @end region=turtle_upgrades

        ExampleMod.registerComputerCraft();

        // @start region=peripherals
        PeripheralLookup.get().registerForBlockEntity((f, s) -> new BrewingStandPeripheral(f), BlockEntityType.BREWING_STAND);
        // @end region=peripherals
    }
}
