// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.gametest

import dan200.computercraft.gametest.api.*
import dan200.computercraft.shared.ModRegistry
import dan200.computercraft.shared.media.items.PrintoutItem
import dan200.computercraft.test.shared.ItemStackMatcher.isStack
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestGenerator
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.gametest.framework.TestFunction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals

class Printout_Test {
    /**
     * Test printouts render correctly
     */
    @GameTestGenerator
    fun Render_in_frame(): List<TestFunction> {
        val tests = mutableListOf<TestFunction>()

        fun addTest(label: String, time: Long = Times.NOON, tag: String = TestTags.CLIENT) {
            if (!TestTags.isEnabled(tag)) return

            val className = this::class.java.simpleName.lowercase()
            val testName = "$className.render_in_frame"

            tests.add(
                TestFunction(
                    "$testName.$label",
                    "$testName.$label",
                    testName,
                    Timeouts.DEFAULT,
                    0,
                    true,
                ) { renderPrintout(it, time) },
            )
        }

        addTest("noon", Times.NOON)
        addTest("midnight", Times.MIDNIGHT)

        addTest("sodium", tag = "sodium")

        addTest("iris_noon", Times.NOON, tag = "iris")
        addTest("iris_midnight", Times.MIDNIGHT, tag = "iris")

        return tests
    }

    private fun renderPrintout(helper: GameTestHelper, time: Long) = helper.sequence {
        thenExecute {
            helper.level.dayTime = time
            helper.positionAtArmorStand()
        }

        thenScreenshot()

        thenExecute { helper.level.dayTime = Times.NOON }
    }

    @GameTest(template = "default")
    fun Craft_pages(helper: GameTestHelper) = helper.immediate {
        // Assert that crafting with only one page fails
        helper.assertNotCraftable(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(Items.STRING))

        // Assert that crafting with no pages fails
        helper.assertNotCraftable(ItemStack(Items.PAPER), ItemStack(Items.PAPER), ItemStack(Items.STRING))

        // Assert that crafting with a book fails
        helper.assertNotCraftable(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(ModRegistry.Items.PRINTED_BOOK.get()), ItemStack(Items.STRING))

        assertThat(
            helper.craftItem(
                PrintoutItem.createSingleFromTitleAndText("First", createPagesOf(' '), createPagesOf('b')),
                PrintoutItem.createMultipleFromTitleAndText("Second", createPagesOf(' '), createPagesOf('b')),
                ItemStack(Items.STRING),
            ),
            isStack(PrintoutItem.createMultipleFromTitleAndText("First", createPagesOf(' ', 2), createPagesOf('b', 2))),
        )
    }

    @GameTest(template = "default")
    fun Craft_book(helper: GameTestHelper) = helper.immediate {
        // Assert that crafting with no pages fails
        helper.assertNotCraftable(ItemStack(Items.PAPER), ItemStack(Items.PAPER), ItemStack(Items.STRING), ItemStack(Items.LEATHER))

        // Assert that crafting with only one page works
        assertEquals(
            ModRegistry.Items.PRINTED_BOOK.get(),
            helper.craftItem(ItemStack(ModRegistry.Items.PRINTED_PAGE.get()), ItemStack(Items.STRING), ItemStack(Items.LEATHER)).item,
        )

        assertThat(
            helper.craftItem(
                PrintoutItem.createSingleFromTitleAndText("First", createPagesOf(' '), createPagesOf('b')),
                PrintoutItem.createMultipleFromTitleAndText("Second", createPagesOf(' '), createPagesOf('b')),
                ItemStack(Items.STRING),
                ItemStack(Items.LEATHER),
            ),
            isStack(PrintoutItem.createBookFromTitleAndText("First", createPagesOf(' ', 2), createPagesOf('b', 2))),
        )
    }

    private fun createPagesOf(c: Char, pages: Int = 1): Array<String> {
        val line = c.toString().repeat(PrintoutItem.LINE_MAX_LENGTH)
        return Array(PrintoutItem.LINES_PER_PAGE * pages) { line }
    }
}
