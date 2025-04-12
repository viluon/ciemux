// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.api.lua.Coerced
import dan200.computercraft.api.lua.LuaException
import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutItem
import dan200.computercraft.shared.peripheral.printer.PrinterBlock
import dan200.computercraft.shared.peripheral.printer.PrinterPeripheral
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.RedStoneWireBlock
import org.junit.jupiter.api.Assertions.*
import java.util.*

class Printer_Test {
    /**
     * Check comparators can read the contents of the printer
     */
    @GameTest
    fun Comparator(helper: GameTestHelper) = helper.sequence {
        val printerPos = BlockPos(2, 2, 2)
        val dustPos = BlockPos(2, 2, 4)

        // Adding items should provide power
        thenExecute {
            val printer = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            printer.setItem(0, ItemStack(Items.BLACK_DYE))
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 1) }

        // And removing them should reset power.
        thenExecute {
            val printer = helper.getBlockEntity(printerPos, ModRegistry.BlockEntities.PRINTER.get())
            printer.clearContent()
            printer.setChanged()
        }
        thenIdle(2)
        thenExecute { helper.assertBlockHas(dustPos, RedStoneWireBlock.POWER, 0) }
    }

    /**
     * Changing the inventory contents updates the block state
     */
    @GameTest(template = "printer_test.empty")
    fun Contents_updates_state(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())

            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in the top row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "One item in the top row")

            printer.setItem(7, ItemStack(Items.PAPER))
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, true, message = "One item in each row")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in each row")

            printer.setItem(1, ItemStack.EMPTY)
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "One item in the bottom")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "One item in the bottom row")

            printer.setItem(7, ItemStack.EMPTY)
            printer.setChanged()
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "Empty")
        }
    }

    /**
     * Printing a page
     */
    @GameTest(template = "printer_test.empty")
    fun Print_page(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())
            val peripheral = printer.peripheral() as PrinterPeripheral

            // Try to print with no pages
            assertFalse(peripheral.newPage(), "newPage fails with no items")

            // Try to print with just ink
            printer.setItem(0, ItemStack(Items.BLUE_DYE))
            printer.setChanged()
            assertFalse(peripheral.newPage(), "newPage fails with no paper")

            printer.clearContent()

            // Try to print with just paper
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            assertFalse(peripheral.newPage(), "newPage fails with no ink")

            printer.clearContent()

            // Try to print with both items
            printer.setItem(0, ItemStack(Items.BLUE_DYE))
            printer.setItem(1, ItemStack(Items.PAPER))
            printer.setChanged()
            assertTrue(peripheral.newPage(), "newPage succeeds")

            // newPage() should consume both items and update the block state
            helper.assertContainerEmpty(pos)
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, false, message = "Empty")

            assertFalse(peripheral.newPage(), "Cannot start a page when already printing")

            peripheral.setPageTitle(Optional.of("New Page"))
            peripheral.write(Coerced("Hello, world!"))
            peripheral.setCursorPos(5, 2)
            peripheral.write(Coerced("Second line"))

            // Try to finish the page
            assertTrue(peripheral.endPage(), "endPage prints item")

            // endPage() should
            helper.assertBlockHas(pos, PrinterBlock.TOP, false, message = "Empty")
            helper.assertBlockHas(pos, PrinterBlock.BOTTOM, true, message = "Has pages")

            // And check the inventory matches
            val lines = createPageOf(' ')
            lines[0] = "Hello, world!            "
            lines[1] = "    Second line          "
            helper.assertContainerExactly(
                pos,
                listOf(
                    // Ink
                    ItemStack.EMPTY,
                    // Paper
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                    // Pages
                    PrintoutItem.createSingleFromTitleAndText("New Page", lines, createPageOf('b')),
                    ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ),
            )

            val error = assertThrows(LuaException::class.java) { peripheral.endPage() }
            assertEquals("Page not started", error.message)
        }
    }

    /**
     * Can't print when full.
     */
    @GameTest
    fun No_print_when_full(helper: GameTestHelper) = helper.sequence {
        val pos = BlockPos(2, 2, 2)

        thenExecute {
            val printer = helper.getBlockEntity(pos, ModRegistry.BlockEntities.PRINTER.get())
            val peripheral = printer.peripheral() as PrinterPeripheral
            assertTrue(peripheral.newPage())
            assertFalse(peripheral.endPage(), "Cannot print when full")
        }
    }

    /**
     * When the block is broken, we drop the contents and an optionally named stack.
     */
    @GameTest
    fun Drops_contents(helper: GameTestHelper) = helper.sequence {
        thenExecute {
            helper.level.destroyBlock(helper.absolutePos(BlockPos(2, 2, 2)), true)
            helper.assertExactlyItems(
                ItemStack(ModRegistry.Items.PRINTER.get()).setHoverName(Component.literal("My Printer")),
                ItemStack(Items.PAPER),
                ItemStack(Items.BLACK_DYE),
                message = "Breaking a printer should drop the contents",
            )
        }
    }

    /**
     * Asserts items can be inserted into a printer.
     */
    @GameTest(required = false) // Allow on Forge for now, see #1951.
    fun Can_insert_items(helper: GameTestHelper) = helper.sequence {
        thenWaitUntil {
            helper.assertContainerExactly(BlockPos(1, 2, 2), listOf(ItemStack.EMPTY, ItemStack(Items.PAPER)))
            helper.assertContainerExactly(BlockPos(3, 2, 2), listOf(ItemStack(Items.BLACK_DYE)))
        }
    }

    /**
     * Asserts items can be removed from a printer.
     */
    @GameTest
    fun Can_extract_items(helper: GameTestHelper) = helper.sequence {
        thenWaitUntil { helper.assertContainerEmpty(BlockPos(2, 3, 2)) }
    }

    private fun createPageOf(c: Char): Array<String> {
        val line = c.toString().repeat(PrintoutItem.LINE_MAX_LENGTH)
        return Array(PrintoutItem.LINES_PER_PAGE) { line }
    }
}
