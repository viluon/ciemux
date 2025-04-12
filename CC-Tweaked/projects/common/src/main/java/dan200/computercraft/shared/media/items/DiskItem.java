// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class DiskItem extends Item implements IColouredItem {
    private static final String NBT_ID = "DiskId";

    public DiskItem(Properties settings) {
        super(settings);
    }

    public static ItemStack createFromIDAndColour(int id, @Nullable String label, int colour) {
        var stack = new ItemStack(ModRegistry.Items.DISK.get());
        setDiskID(stack, id);
        if (label != null) stack.setHoverName(Component.literal(label));
        IColouredItem.setColourBasic(stack, colour);
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag options) {
        if (options.isAdvanced()) {
            var id = getDiskID(stack);
            if (id >= 0) {
                list.add(Component.translatable("gui.computercraft.tooltip.disk_id", id)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return DiskDriveBlock.defaultUseItemOn(context);
    }

    public static int getDiskID(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_ID) ? nbt.getInt(NBT_ID) : -1;
    }

    public static void setDiskID(ItemStack stack, int id) {
        if (id >= 0) stack.getOrCreateTag().putInt(NBT_ID, id);
    }

    @Override
    public int getColour(ItemStack stack) {
        var colour = IColouredItem.getColourBasic(stack);
        return colour == -1 ? Colour.WHITE.getHex() : colour;
    }
}
