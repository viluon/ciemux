/*
 * The MIT License (MIT)
 *
 * Original Source: Copyright (c) 2009-2011 Luaj.org. All rights reserved.
 * Modifications: Copyright (c) 2015-2020 SquidDev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.squiddev.cobalt.function;

import org.squiddev.cobalt.*;

import static org.squiddev.cobalt.Constants.NIL;

/**
 * A {@link LibFunction} which accepts three arguments. This invokes a {@link LibFunction.ThreeArg} implementation,
 * filling in or dropping arguments as needed.
 */
final class ThreeArgFunction extends LibFunction {
	private final ThreeArg function;

	ThreeArgFunction(ThreeArg function) {
		this.function = function;
	}

	@Override
	protected LuaValue call(LuaState state) throws LuaError {
		return function.call(state, NIL, NIL, NIL);
	}

	@Override
	protected LuaValue call(LuaState state, LuaValue arg) throws LuaError {
		return function.call(state, arg, NIL, NIL);
	}

	@Override
	protected LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2) throws LuaError {
		return function.call(state, arg1, arg2, NIL);
	}

	@Override
	protected LuaValue call(LuaState state, LuaValue arg1, LuaValue arg2, LuaValue arg3) throws LuaError {
		return function.call(state, arg1, arg2, arg3);
	}

	@Override
	protected Varargs invoke(LuaState state, Varargs varargs) throws LuaError, UnwindThrowable {
		return function.call(state, varargs.first(), varargs.arg(2), varargs.arg(3));
	}
}
