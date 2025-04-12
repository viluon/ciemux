package com.example.examplemod.peripheral;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jspecify.annotations.Nullable;

/**
 * A peripheral that tracks what computers it is attached to.
 *
 * @see AttachedComputerSet
 */
// @start region=body
public class ComputerTrackingPeripheral implements IPeripheral {
    private final AttachedComputerSet computers = new AttachedComputerSet();

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction
    public final void sayHello() {
        // Queue a "hello" event on each computer.
        computers.forEach(x -> x.queueEvent("hello", x.getAttachmentName()));
    }

    @Override
    public String getType() {
        return "my_peripheral";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return this == other;
    }
}
// @end region=body
