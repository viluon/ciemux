// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import dan200.computercraft.shared.computer.menu.ServerInputHandler;

import java.nio.ByteBuffer;

/**
 * Handles user-provided input, forwarding it to a computer. This describes the "shape" of both the client-and
 * server-side input handlers.
 *
 * @see ServerInputHandler
 * @see ServerComputer
 */
public interface InputHandler {
    void keyDown(int key, boolean repeat);

    void keyUp(int key);

    void charTyped(byte chr);

    void paste(ByteBuffer contents);

    void mouseClick(int button, int x, int y);

    void mouseUp(int button, int x, int y);

    void mouseDrag(int button, int x, int y);

    void mouseScroll(int direction, int x, int y);

    void terminate();

    void shutdown();

    void turnOn();

    void reboot();
}
