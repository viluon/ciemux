package com.example.examplemod.data;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ExampleTurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * A {@link TurtleUpgradeDataProvider} that generates the JSON for our {@linkplain ExampleTurtleUpgrade example
 * upgrade}.
 *
 * @see ExampleModDataGenerators
 */
// @start region=body
public class TurtleDataProvider extends TurtleUpgradeDataProvider {
    public TurtleDataProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void addUpgrades(Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade) {
        simpleWithCustomItem(
            new ResourceLocation(ExampleMod.MOD_ID, "example_turtle_upgrade"),
            ExampleMod.EXAMPLE_TURTLE_UPGRADE,
            Items.COMPASS
        ).add(addUpgrade);
    }
}
// @end region=body
