package org.squiddev.cobalt.function

import org.squiddev.cobalt.LuaError
import org.squiddev.cobalt.LuaThread
import org.squiddev.cobalt.UnwindThrowable
import org.squiddev.cobalt.debug.DebugFrame

trait UnwindableCallable {
  @throws[LuaError]
  @throws[UnwindThrowable]
  def call(thread: LuaThread, di: DebugFrame, cont: EvalCont): Unit
}
