// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.computer;

import dan200.computercraft.core.util.StringUtil;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Built-in events that can be queued on a computer.
 */
public final class ComputerEvents {
    private ComputerEvents() {
    }

    public static void keyDown(Receiver receiver, int key, boolean repeat) {
        receiver.queueEvent("key", new Object[]{ key, repeat });
    }

    public static void keyUp(Receiver receiver, int key) {
        receiver.queueEvent("key_up", new Object[]{ key });
    }

    /**
     * Type a character on the computer.
     *
     * @param receiver The computer to queue the event on.
     * @param chr      The character to type.
     * @see StringUtil#isTypableChar(byte)
     */
    public static void charTyped(Receiver receiver, byte chr) {
        receiver.queueEvent("char", new Object[]{ new byte[]{ chr } });
    }

    /**
     * Paste a string.
     *
     * @param receiver The computer to queue the event on.
     * @param contents The string to paste.
     * @see StringUtil#getClipboardString(String)
     */
    public static void paste(Receiver receiver, ByteBuffer contents) {
        receiver.queueEvent("paste", new Object[]{ contents });
    }

    public static void mouseClick(Receiver receiver, int button, int x, int y) {
        receiver.queueEvent("mouse_click", new Object[]{ button, x, y });
    }

    public static void mouseUp(Receiver receiver, int button, int x, int y) {
        receiver.queueEvent("mouse_up", new Object[]{ button, x, y });
    }

    public static void mouseDrag(Receiver receiver, int button, int x, int y) {
        receiver.queueEvent("mouse_drag", new Object[]{ button, x, y });
    }

    public static void mouseScroll(Receiver receiver, int direction, int x, int y) {
        receiver.queueEvent("mouse_scroll", new Object[]{ direction, x, y });
    }

    /**
     * An object that can receive computer events.
     */
    @FunctionalInterface
    public interface Receiver {
        void queueEvent(String event, @Nullable Object @Nullable [] arguments);
    }
}
