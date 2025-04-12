package com.example.examplemod;

import com.example.examplemod.peripheral.BrewingStandPeripheral;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 * The main entry point for the Forge version of our example mod.
 */
@Mod(ExampleMod.MOD_ID)
public class ForgeExampleMod {
    public ForgeExampleMod() {
        // Register our turtle upgrade. If writing a Forge-only mod, you'd normally use DeferredRegister instead.
        // However, this is an easy way to implement this in a multi-loader-compatible manner.

        // @start region=turtle_upgrades
        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterEvent event) -> {
            event.register(TurtleUpgradeSerialiser.registryId(), new ResourceLocation(ExampleMod.MOD_ID, "example_turtle_upgrade"), () -> ExampleMod.EXAMPLE_TURTLE_UPGRADE);
        });
        // @end region=turtle_upgrades

        modBus.addListener((FMLCommonSetupEvent event) -> ExampleMod.registerComputerCraft());

        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, ForgeExampleMod::attachPeripherals);
    }

    // @start region=peripherals
    // The main function to attach peripherals to block entities. This should be added to the Forge event bus.
    public static void attachPeripherals(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BrewingStandBlockEntity brewingStand) {
            PeripheralProvider.attach(event, brewingStand, BrewingStandPeripheral::new);
        }
    }

    // Boilerplate for adding a new capability provider

    public static final Capability<IPeripheral> CAPABILITY_PERIPHERAL = CapabilityManager.get(new CapabilityToken<>() {
    });
    private static final ResourceLocation PERIPHERAL = new ResourceLocation(ExampleMod.MOD_ID, "peripheral");

    // A {@link ICapabilityProvider} that lazily creates an {@link IPeripheral} when required.
    private static final class PeripheralProvider<O extends BlockEntity> implements ICapabilityProvider {
        private final O blockEntity;
        private final Function<O, IPeripheral> factory;
        private @Nullable LazyOptional<IPeripheral> peripheral;

        private PeripheralProvider(O blockEntity, Function<O, IPeripheral> factory) {
            this.blockEntity = blockEntity;
            this.factory = factory;
        }

        private static <O extends BlockEntity> void attach(AttachCapabilitiesEvent<BlockEntity> event, O blockEntity, Function<O, IPeripheral> factory) {
            var provider = new PeripheralProvider<>(blockEntity, factory);
            event.addCapability(PERIPHERAL, provider);
            event.addListener(provider::invalidate);
        }

        private void invalidate() {
            if (peripheral != null) peripheral.invalidate();
            peripheral = null;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction direction) {
            if (capability != CAPABILITY_PERIPHERAL) return LazyOptional.empty();
            if (blockEntity.isRemoved()) return LazyOptional.empty();

            var peripheral = this.peripheral;
            return (peripheral == null ? (this.peripheral = LazyOptional.of(() -> factory.apply(blockEntity))) : peripheral).cast();
        }
    }
    // @end region=peripherals
}
