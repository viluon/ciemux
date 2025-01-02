// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.peripheral;

import dan200.computercraft.api.lua.GenericSource;

/**
 * A {@link GenericSource} which provides methods for a peripheral.
 * <p>
 * Unlike a {@link GenericSource}, all methods <strong>should</strong> target the same type, for instance a
 * block entity subclass. This is not currently enforced.
 */
public interface GenericPeripheral extends GenericSource {
    /**
     * Get the type of the exposed peripheral.
     * <p>
     * Unlike normal {@link IPeripheral}s, {@link GenericPeripheral} do not have to have a type. By default, the
     * resulting peripheral uses the resource name of the wrapped block entity (for instance {@code minecraft:chest}).
     * <p>
     * However, in some cases it may be more appropriate to specify a more readable name, or provide
     * {@linkplain PeripheralType#getAdditionalTypes() additional types}. Overriding this method allows you to do so.
     * <p>
     * When multiple {@link GenericPeripheral}s provide a {@linkplain PeripheralType#getPrimaryType() primary peripheral
     * type} for a single block entity, the lexicographically smallest will be chosen. In order to avoid this conflict,
     * primary types should only be used when your peripheral targets a single block entity <strong>AND</strong> it's
     * likely that you're the only mod to do so.
     *
     * @return The type of this peripheral or {@link PeripheralType#untyped()}.
     * @see IPeripheral#getType()
     * @see IPeripheral#getAdditionalTypes()
     */
    default PeripheralType getType() {
        return PeripheralType.untyped();
    }
}
