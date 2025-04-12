// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.media.items;

import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class TreasureDiskItem extends Item {
    private static final String NBT_TITLE = "Title";
    private static final String NBT_COLOUR = "Colour";
    private static final String NBT_SUB_PATH = "SubPath";

    public TreasureDiskItem(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> list, TooltipFlag tooltipOptions) {
        var label = getTitle(stack);
        if (!label.isEmpty()) list.add(Component.literal(label));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return DiskDriveBlock.defaultUseItemOn(context);
    }

    public static ItemStack create(String subPath, int colourIndex) {
        var result = new ItemStack(ModRegistry.Items.TREASURE_DISK.get());
        var nbt = result.getOrCreateTag();
        nbt.putString(NBT_SUB_PATH, subPath);

        var slash = subPath.indexOf('/');
        if (slash >= 0) {
            var author = subPath.substring(0, slash);
            var title = subPath.substring(slash + 1);
            nbt.putString(NBT_TITLE, "\"" + title + "\" by " + author);
        } else {
            nbt.putString(NBT_TITLE, "untitled");
        }
        nbt.putInt(NBT_COLOUR, Colour.values()[colourIndex].getHex());

        return result;
    }

    static String getTitle(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_TITLE) ? nbt.getString(NBT_TITLE) : "'missingno' by how did you get this anyway?";
    }

    static String getSubPath(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_SUB_PATH) ? nbt.getString(NBT_SUB_PATH) : "dan200/alongtimeago";
    }

    public static int getColour(ItemStack stack) {
        var nbt = stack.getTag();
        return nbt != null && nbt.contains(NBT_COLOUR) ? nbt.getInt(NBT_COLOUR) : Colour.BLUE.getHex();
    }
}
