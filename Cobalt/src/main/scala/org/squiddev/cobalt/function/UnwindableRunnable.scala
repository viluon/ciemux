package org.squiddev.cobalt.function

import org.squiddev.cobalt.LuaError
import org.squiddev.cobalt.UnwindThrowable
import org.squiddev.cobalt.debug.DebugFrame

trait UnwindableRunnable {
  @throws[LuaError]
  @throws[UnwindThrowable]
  def run(di: DebugFrame): Unit
}
