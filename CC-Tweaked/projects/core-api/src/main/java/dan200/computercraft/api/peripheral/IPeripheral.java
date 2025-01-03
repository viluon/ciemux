// Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaTask;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * A peripheral is an external device that a computer can interact with.
 * <p>
 * Peripherals can be supplied by both a block (or block entity), or from
 * {@linkplain dan200.computercraft.api.turtle.ITurtleUpgrade#createPeripheral(dan200.computercraft.api.turtle.ITurtleAccess, dan200.computercraft.api.turtle.TurtleSide) turtle}
 * or {@linkplain dan200.computercraft.api.pocket.IPocketUpgrade#createPeripheral(dan200.computercraft.api.pocket.IPocketAccess) pocket}
 * upgrades.
 * <p>
 * See the {@linkplain dan200.computercraft.api.peripheral package documentation} for more information on registering peripherals.
 * <p>
 * Peripherals should provide a series of methods to the user, typically by annotating Java methods with
 * {@link LuaFunction}. Alternatively, {@link IDynamicPeripheral} may be used to provide a dynamic set of methods.
 * Remember that peripheral methods are called on the <em>computer</em> thread, and so it is not safe to interact with
 * the Minecraft world by default. One should use {@link LuaFunction#mainThread()} or
 * {@link ILuaContext#executeMainThreadTask(LuaTask)} to run code on the main server thread.
 */
public interface IPeripheral {
    /**
     * Should return a string that uniquely identifies this type of peripheral.
     * This can be queried from lua by calling {@code peripheral.getType()}
     *
     * @return A string identifying the type of peripheral.
     * @see PeripheralType#getPrimaryType()
     */
    String getType();

    /**
     * Return additional types/traits associated with this object.
     *
     * @return A collection of additional object traits.
     * @see PeripheralType#getAdditionalTypes()
     */
    default Set<String> getAdditionalTypes() {
        return Set.of();
    }

    /**
     * Is called when a computer is attaching to the peripheral.
     * <p>
     * This will occur when a peripheral is placed next to an active computer, when a computer is turned on next to a
     * peripheral, when a turtle travels into a square next to a peripheral, or when a wired modem adjacent to this
     * peripheral is does any of the above.
     * <p>
     * Between calls to attach and {@link #detach}, the attached computer can make method calls on the peripheral using
     * {@code peripheral.call()}. This method can be used to keep track of which computers are attached to the
     * peripheral, or to take action when attachment occurs.
     * <p>
     * Be aware that may be called from both the server thread and ComputerCraft Lua thread, and so must be thread-safe
     * and reentrant. If you need to store a list of attached computers, it is recommended you use a
     * {@link AttachedComputerSet}.
     *
     * @param computer The interface to the computer that is being attached. Remember that multiple computers can be
     *                 attached to a peripheral at once.
     * @see #detach
     */
    default void attach(IComputerAccess computer) {
    }

    /**
     * Called when a computer is detaching from the peripheral.
     * <p>
     * This will occur when a computer shuts down, when the peripheral is removed while attached to computers, when a
     * turtle moves away from a block attached to a peripheral, or when a wired modem adjacent to this peripheral is
     * detached.
     * <p>
     * This method can be used to keep track of which computers are attached to the peripheral, or to take action when
     * detachment occurs.
     * <p>
     * Be aware that this may be called from both the server and ComputerCraft Lua thread, and must be thread-safe
     * and reentrant. If you need to store a list of attached computers, it is recommended you use a
     * {@link AttachedComputerSet}.
     *
     * @param computer The interface to the computer that is being detached. Remember that multiple computers can be
     *                 attached to a peripheral at once.
     * @see #attach
     */
    default void detach(IComputerAccess computer) {
    }

    /**
     * Get the object that this peripheral provides methods for. This will generally be the block entity
     * or block, but may be an inventory, entity, etc...
     *
     * @return The object this peripheral targets
     */
    @Nullable
    default Object getTarget() {
        return null;
    }

    /**
     * Determine whether this peripheral is equivalent to another one.
     * <p>
     * The minimal example should at least check whether they are the same object. However, you may wish to check if
     * they point to the same block or block entity.
     *
     * @param other The peripheral to compare against. This may be {@code null}.
     * @return Whether these peripherals are equivalent.
     */
    boolean equals(@Nullable IPeripheral other);
}
