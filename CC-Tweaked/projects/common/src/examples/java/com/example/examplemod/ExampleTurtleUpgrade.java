package com.example.examplemod;

import dan200.computercraft.api.turtle.AbstractTurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleUpgradeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * An example turtle upgrade.
 */
// @start region=body
public class ExampleTurtleUpgrade extends AbstractTurtleUpgrade {
    public ExampleTurtleUpgrade(ResourceLocation id, ItemStack stack) {
        super(id, TurtleUpgradeType.PERIPHERAL, stack);
    }
}
// @end region=body
