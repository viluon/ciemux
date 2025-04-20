package org.squiddev.cobalt.function;

import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaThread;
import org.squiddev.cobalt.UnwindThrowable;
import org.squiddev.cobalt.debug.DebugFrame;

public interface UnwindableCallable {
	void call(LuaThread thread, DebugFrame di, EvalCont cont) throws LuaError, UnwindThrowable;
}
