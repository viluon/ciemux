package org.squiddev.cobalt.function

import org.squiddev.cobalt.{LuaError, LuaState, LuaThread, UnwindThrowable}
import org.squiddev.cobalt.debug.DebugFrame

trait UnwindableCallable {
  @throws[LuaError]
  @throws[UnwindThrowable]
  def call(state: LuaState, thread: LuaThread, di: DebugFrame, cont: EvalCont): Unit
}
