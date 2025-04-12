// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class FabricCommonHooks {
    public static boolean onBlockDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        return !(state.getBlock() instanceof CableBlock cable) || !cable.onCustomDestroyBlock(state, level, pos, player);
    }
}
