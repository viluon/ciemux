package org.squiddev.cobalt.function

import org.squiddev.cobalt.Constants.{FALSE, NIL, NONE, TRUE}
import org.squiddev.cobalt.Lua.{GETARG_A, GETARG_Ax, GETARG_B, GETARG_Bx, GETARG_C, GETARG_sBx, GET_OPCODE, LFIELDS_PER_FLUSH, OP_ADD, OP_CALL, OP_CLOSURE, OP_CONCAT, OP_DIV, OP_EQ, OP_EXTRAARG, OP_FORLOOP, OP_FORPREP, OP_GETTABLE, OP_GETTABUP, OP_GETUPVAL, OP_JMP, OP_LE, OP_LEN, OP_LOADBOOL, OP_LOADK, OP_LOADKX, OP_LOADNIL, OP_LT, OP_MOD, OP_MOVE, OP_MUL, OP_NEWTABLE, OP_NOT, OP_POW, OP_RETURN, OP_SELF, OP_SETLIST, OP_SETTABLE, OP_SETTABUP, OP_SETUPVAL, OP_SUB, OP_TAILCALL, OP_TEST, OP_TESTSET, OP_TFORCALL, OP_TFORLOOP, OP_UNM, OP_VARARG}
import org.squiddev.cobalt.LuaDouble.valueOf
import org.squiddev.cobalt.debug.DebugFrame.{FLAG_FRESH, FLAG_TAIL}
import org.squiddev.cobalt.debug.{DebugFrame, DebugState}
import org.squiddev.cobalt.function.LuaInterpreter.{concat, createStack, doJump, getRK, luaO_fb2int, nativeCall, resume, setupCall, setupFrame, setupStack}
import org.squiddev.cobalt.lib.StringLib
import org.squiddev.cobalt.{LuaError, LuaState, LuaTable, OperationHelper, Print, Prototype, UnwindThrowable, ValueFactory, Varargs}

import java.io.FileOutputStream
import scala.annotation.tailrec
import scala.quoted.*
import scala.util.Using

given staging.Compiler = staging.Compiler.make(getClass.getClassLoader)

object LuaToScalaCompiler:
	private def unrolledPowerCode(x: Expr[Double], n: Int)(using Quotes): Expr[Double] =
		if n == 0 then '{ 1.0 }
		else if n == 1 then x
		else '{ $x * ${ unrolledPowerCode(x, n - 1) } }

	private val power3: Double => Double = staging.run {
		val stagedPower3: Expr[Double => Double] =
			'{ (x: Double) => ${ unrolledPowerCode('x, 3) } }
		stagedPower3
	}

	def dump(state: LuaState, function: LuaInterpretedFunction): String = {
		val file = java.nio.file.Files.createTempFile("bytecode:", "").toFile
		val bytecode = StringLib.dump(state, function, NIL).checkString()

		Using(FileOutputStream(file)) { stream =>
			stream.write(bytecode.getBytes)
		}

		file.getName
	}

	def printOpcode(i: Int): String = GET_OPCODE(i) match {
		case OP_MOVE =>
			// A B: R(A):= R(B)
			"MOVE"
		case OP_LOADK =>
			// A Bx: R(A):= Kst(Bx)
			"LOADK"
		case OP_LOADKX =>
			// A: R(A) := Kst(extra arg)
			"LOADKX"
		case OP_LOADBOOL =>
			// A B C: R(A):= (Bool)B: if (C) pc++
			"LOADBOOL"
		case OP_LOADNIL =>
			// A B     R(A), R(A+1), ..., R(A+B) := nil
			"LOADNIL"
		case OP_GETUPVAL =>
			// A B: R(A):= UpValue[B]
			"GETUPVAL"
		case OP_GETTABUP =>
			// A B C: R(A) := UpValue[B][RK(C)]
			"GETTABUP"
		case OP_GETTABLE =>
			// A B C: R(A):= R(B)[RK(C)]
			"GETTABLE"
		case OP_SETTABUP =>
			// A B C: UpValue[A][RK(B)] := RK(C)
			"SETTABUP"
		case OP_SETUPVAL =>
			// A B: UpValue[B]:= R(A)
			"SETUPVAL"
		case OP_SETTABLE =>
			// A B C: R(A)[RK(B)]:= RK(C)
			"SETTABLE"
		case OP_NEWTABLE =>
			// A B C: R(A):= {} (size = B,C)
			"NEWTABLE"
		case OP_SELF =>
			// A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
			"SELF"
		case OP_ADD =>
			// A B C: R(A):= RK(B) + RK(C)
			"ADD"
		case OP_SUB =>
			// A B C: R(A):= RK(B) - RK(C)
			"SUB"
		case OP_MUL =>
			// A B C: R(A):= RK(B) * RK(C)
			"MUL"
		case OP_DIV =>
			// A B C: R(A):= RK(B) / RK(C)
			"DIV"
		case OP_MOD =>
			// A B C: R(A):= RK(B) % RK(C)
			"MOD"
		case OP_POW =>
			// A B C: R(A):= RK(B) ^ RK(C)
			"POW"
		case OP_UNM =>
			// A B: R(A):= -R(B)
			"UNM"
		case OP_NOT =>
			// A B: R(A):= not R(B)
			"NOT"
		case OP_LEN =>
			// A B: R(A):= length of R(B)
			"LEN"
		case OP_CONCAT =>
			// A B C: R(A):= R(B).. ... ..R(C)
			"CONCAT"
		case OP_JMP =>
			// sBx: pc+=sBx
			"JMP"
		case OP_EQ =>
			// A B C: if ((RK(B) == RK(C)) ~= A) then pc++
			"EQ"
		case OP_LT =>
			// A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
			"LT"
		case OP_LE =>
			// A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
			"LE"
		case OP_TEST =>
			// A C: if not (R(A) <=> C) then pc++
			"TEST"
		case OP_TESTSET =>
			// A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
			"TESTSET"
		case OP_CALL =>
			// A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
			"CALL"
		case OP_TAILCALL =>
			// A B C: return R(A)(R(A+1), ... ,R(A+B-1))
			"TAILCALL"
		case OP_RETURN =>
			// A B: return R(A), ... ,R(A+B-2) (see note)
			"RETURN"
		case OP_FORLOOP =>
			// A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
			"FORLOOP"
		case OP_FORPREP =>
			// A sBx: R(A)-=R(A+2): pc+=sBx
			"FORPREP"
		case OP_TFORCALL =>
			"TFORCALL"
		case OP_TFORLOOP =>
			"TFORLOOP"
		case OP_SETLIST =>
			// A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
			"SETLIST"
		case OP_CLOSURE =>
			// A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
			"CLOSURE"
		case OP_VARARG =>
			// A B: R(A), R(A+1), ..., R(A+B-1) = vararg
			"VARARG"
		case _ =>
			assert(false, "Unknown opcode")
			throw new IllegalStateException("Unknown opcode")
	}

	def show(p: Prototype, pc: Int): String = {
		p.code
				.map(LuaToScalaCompiler.printOpcode)
				.zipWithIndex
				.map((line, i) => s"${p.lineAt(i)}: " + (if (i == pc) {
					line + s"    <============ pc = $pc"
				} else {
					line
				}))
				.mkString("\n")
	}

	private def continuation(proto: Prototype, pc: Int): UnwindableRunnable => UnwindableCallable = f => {
		val callable: UnwindableCallable = (thread, di, c) => {
			f.run(di)
			c.programCounter = di.pc + 1
		}
		proto.compiledInstructions(pc) = callable
		callable
	}

	private def rawCont(proto: Prototype, pc: Int): UnwindableCallable => UnwindableCallable = raw => {
		proto.compiledInstructions(pc) = raw
		raw
	}

	def partialEvalStep(state: LuaState, p: Prototype, pc: Int): UnwindableCallable = {
		// fetch all info from the function
		val code = p.code
		val k = p.constants

		// pull out the instruction
		val i = code(pc)
		val a = GETARG_A(i)

		// set up the base continuations
		val cont = continuation(p, pc)
		val raw = rawCont(p, pc)

		// process the instruction
		GET_OPCODE(i) match {
			case OP_MOVE => // A B: R(A):= R(B)
				val b = GETARG_B(i)
				cont(di => di.stack(a) = di.stack(b))

			case OP_LOADK => // A Bx: R(A):= Kst(Bx)
				val constant = k(GETARG_Bx(i))
				cont(di => di.stack(a) = constant)

			case OP_LOADKX =>
				// A: R(A) := Kst(extra arg)
				assert(GET_OPCODE(code(pc + 1)) == OP_EXTRAARG)
				val rb = GETARG_Ax(code(pc + 1))
				val constant = k(rb)
				raw((thread, di, cont) => {
					di.stack(a) = constant
					cont.programCounter += 2
				})


			case OP_LOADBOOL =>
				// A B C: R(A):= (Bool)B: if (C) pc++
				val constant = if (GETARG_B(i) != 0) TRUE else FALSE
				if (GETARG_C(i) != 0) {
					// skip next instruction (if C)
					raw((thread, di, c) => {
						di.stack(a) = constant
						c.programCounter += 2
					})
				} else {
					cont(di => di.stack(a) = constant)
				}


			case OP_LOADNIL =>
				// A B     R(A), R(A+1), ..., R(A+B) := nil
				val initB = GETARG_B(i)
				cont(di => {
					var a2 = a
					var b = initB
					while
						di.stack({
							a2 += 1;
							a2 - 1
						}) = NIL
						{
							b -= 1;
							b + 1
						} > 0
					do ()
				})


			case OP_GETUPVAL => // A B: R(A):= UpValue[B]
				val index = GETARG_B(i)
				cont(di => di.stack(a) = di.closure.asInstanceOf[LuaInterpretedFunction].upvalues(index).getValue)

			case OP_GETTABUP =>
				// A B C: R(A) := UpValue[B][RK(C)]
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => {
					val upvalue = di.closure.asInstanceOf[LuaInterpretedFunction].upvalues(b)
					di.stack(a) = OperationHelper.getTable(state, upvalue.getValue, getRK(di.stack, k, c), -b - 1)
				})


			case OP_GETTABLE =>
				// A B C: R(A):= R(B)[RK(C)]
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.getTable(state, di.stack(b), getRK(di.stack, k, c), b))


			case OP_SETTABUP =>
				// A B C: UpValue[A][RK(B)] := RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => {
					val upvalue = di.closure.asInstanceOf[LuaInterpretedFunction].upvalues(a)
					OperationHelper.setTable(state, upvalue.getValue, getRK(di.stack, k, b), getRK(di.stack, k, c), -b - 1)
				})


			case OP_SETUPVAL => // A B: UpValue[B]:= R(A)
				val index = GETARG_B(i)
				cont(di => {
					val upvalue = di.closure.asInstanceOf[LuaInterpretedFunction].upvalues(index)
					upvalue.setValue(di.stack(a))
				})

			case OP_SETTABLE =>
				// A B C: R(A)[RK(B)]:= RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => OperationHelper.setTable(state, di.stack(a), getRK(di.stack, k, b), getRK(di.stack, k, c), a))


			case OP_NEWTABLE => // A B C: R(A):= {} (size = B,C)
				val arraySize = luaO_fb2int(GETARG_B(i))
				val hashSize = luaO_fb2int(GETARG_C(i))
				cont(di => di.stack(a) = new LuaTable(arraySize, hashSize))

			case OP_SELF =>
				// A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => {
					val o = di.stack(b)
					di.stack(a + 1) = o
					di.stack(a) = OperationHelper.getTable(state, o, getRK(di.stack, k, c), b)
				})


			case OP_ADD =>
				// A B C: R(A):= RK(B) + RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.add(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_SUB =>
				// A B C: R(A):= RK(B) - RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.sub(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_MUL =>
				// A B C: R(A):= RK(B) * RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.mul(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_DIV =>
				// A B C: R(A):= RK(B) / RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.div(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_MOD =>
				// A B C: R(A):= RK(B) % RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.mod(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_POW =>
				// A B C: R(A):= RK(B) ^ RK(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				cont(di => di.stack(a) = OperationHelper.pow(state, getRK(di.stack, k, b), getRK(di.stack, k, c)))


			case OP_UNM =>
				// A B: R(A):= -R(B)
				val b = GETARG_B(i)
				cont(di => di.stack(a) = OperationHelper.neg(state, getRK(di.stack, k, b)))


			case OP_NOT => // A B: R(A):= not R(B)
				val b = GETARG_B(i)
				cont(di => di.stack(a) = if (di.stack(b).toBoolean) FALSE else TRUE)

			case OP_LEN =>
				// A B: R(A):= length of R(B)
				val b = GETARG_B(i)
				cont(di => di.stack(a) = OperationHelper.length(state, di.stack(b)))


			case OP_CONCAT =>
				// A B C: R(A):= R(B).. ... ..R(C)
				val b = GETARG_B(i)
				val c = GETARG_C(i)

				cont(di => {
					di.top = c + 1
					concat(state, di, di.stack, di.top, c - b + 1)
					di.stack(a) = di.stack(b)
					di.top = b
				})


			case OP_JMP => // sBx: pc+=sBx
				raw((thread, di, c) => c.programCounter += doJump(di, i, 1))

			case OP_EQ =>
				// A B C: if ((RK(B) == RK(C)) ~= A) then pc++
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				val aNonZero = a != 0
				val nextInstruction = code(pc + 1)

				raw((thread, di, cont) => {
					if (OperationHelper.eq(state, getRK(di.stack, k, b), getRK(di.stack, k, c)) == aNonZero) {
						// We assume the next instruction is a jump and read the branch from there.
						cont.programCounter += doJump(di, nextInstruction, 2)
					} else cont.programCounter += 2
				})


			case OP_LT =>
				// A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				val aNonZero = a != 0
				val nextInstruction = code(pc + 1)

				raw((thread, di, cont) => {
					if (OperationHelper.lt(state, getRK(di.stack, k, b), getRK(di.stack, k, c)) == aNonZero) {
						// We assume the next instruction is a jump and read the branch from there.
						cont.programCounter += doJump(di, nextInstruction, 2)
					} else cont.programCounter += 2
				})


			case OP_LE =>
				// A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				val aNonZero = a != 0
				val nextInstruction = code(pc + 1)

				raw((thread, di, cont) => {
					if (OperationHelper.le(state, getRK(di.stack, k, b), getRK(di.stack, k, c)) == aNonZero) {
						// We assume the next instruction is a jump and read the branch from there.
						cont.programCounter += doJump(di, nextInstruction, 2)
					} else cont.programCounter += 2
				})


			case OP_TEST =>
				// A C: if not (R(A) <=> C) then pc++
				val cond = GETARG_C(i) != 0
				val nextInstruction = code(pc + 1)

				raw((thread, di, cont) => {
					if (di.stack(a).toBoolean == cond) {
						// We assume the next instruction is a jump and read the branch from there.
						cont.programCounter += doJump(di, nextInstruction, 2)
					} else cont.programCounter += 2
				})

			case OP_TESTSET =>
				// A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
				/* note: doc appears to be reversed */
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				val cNonZero = c != 0
				val nextInstruction = code(pc + 1)

				raw((thread, di, cont) => {
					val value = di.stack(b)
					if (value.toBoolean == cNonZero) {
						di.stack(a) = value
						// We assume the next instruction is a jump and read the branch from there.
						cont.programCounter += doJump(di, nextInstruction, 2)
					} else cont.programCounter += 2
				})

			case OP_CALL =>
				// A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1))
				val b = GETARG_B(i)
				val c = GETARG_C(i)
				raw((thread, di, cont) => {
					val `val` = di.stack(a)
					if (`val`.isInstanceOf[LuaInterpretedFunction]) {
						val function = `val`.asInstanceOf[LuaInterpretedFunction]
						val newPrototype = function.p
						val newStack = createStack(newPrototype)
						val ds = thread.getDebugState
						val newFrame = ds.pushInfo
						val args = if (b > 0) setupStack(newPrototype, newStack, di.stack, a + 1, b - 1) // Exact args count
						else setupStack(newPrototype, newStack, ValueFactory.varargsOfCopy(di.stack, a + 1, di.top - di.extras.count - (a + 1), di.extras)) // From previous top

						setupFrame(ds, newFrame, function, args, newStack, 0)
						cont.debugFrame = newFrame
						cont.function = function
					} else {
						nativeCall(state, di, di.stack, `val`, i, a, b, c)
						cont.programCounter += 1
					}
				})


			case OP_TAILCALL =>
				// A B C: return R(A)(R(A+1), ... ,R(A+B-1))
				val b = GETARG_B(i)

				raw((thread, di, cont) => {
					val `val` = di.stack(a)
					var args: Varargs = null
					b match {
						case 1 => args = NONE
						case 2 => args = di.stack(a + 1)
						case _ => val v = di.extras
							args = if (b > 0) {
								ValueFactory.varargsOfCopy(di.stack, a + 1, b - 1)
							} else {
								ValueFactory.varargsOfCopy(di.stack, a + 1, di.top - v.count - (a + 1), v)
							} // exact arg count
						// from prev top
					}

					var functionVal: LuaFunction = null
					if (`val`.isInstanceOf[LuaFunction]) functionVal = `val`.asInstanceOf[LuaFunction]
					else {
						functionVal = Dispatch.getCallMetamethod(state, `val`, a)
						args = ValueFactory.varargsOf(`val`, args)
					}

					if (functionVal.isInstanceOf[LuaInterpretedFunction]) {
						val ds = thread.getDebugState
						val flags = di.flags
						di.cleanup()
						ds.popInfo()

						// FIXME: Return hook???!?

						// Replace the current frame with a new one.
						val function = functionVal.asInstanceOf[LuaInterpretedFunction]
						val di2 = if ((flags & FLAG_FRESH) != 0) ds.pushJavaInfo
						else ds.pushInfo
						setupCall(ds, di2, function, args, (flags & FLAG_FRESH) | FLAG_TAIL)
						cont.debugFrame = di2
						cont.function = function
					} else {
						val v = Dispatch.invoke(state, functionVal, args)
						di.top = a + v.count
						di.extras = v
						cont.programCounter += 1
					}
				})

			case OP_RETURN =>
				// A B: return R(A), ... ,R(A+B-2) (see note)
				val b = GETARG_B(i)

				raw((thread, di, cont) => {
					val flags = di.flags
					val top = di.top
					val v = di.extras
					di.cleanup()

					val ret = if (b > 0) {
						ValueFactory.varargsOfCopy(di.stack, a, b - 1)
					} else {
						ValueFactory.varargsOfCopy(di.stack, a, top - v.count - a, v)
					}

					if ((flags & FLAG_FRESH) != 0) {
						// If we're a fresh invocation then return to the parent.
						cont.varargs = ret
					} else {
						val debugState = thread.getDebugState
						debugState.onReturn(di, ret)
						val di2 = debugState.getStackUnsafe
						val function = di2.func.asInstanceOf[LuaInterpretedFunction]
						resume(state, di2, function, ret)
						cont.debugFrame = di2
						cont.function = function
					}
				})

			case OP_FORLOOP =>
				// A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
				val offset = GETARG_sBx(i) + 1

				raw((thread, di, cont) => {
					val limit = di.stack(a + 1).checkDouble
					val step = di.stack(a + 2).checkDouble
					val value = di.stack(a).checkDouble
					val idx = step + value

					val cond = if (0 < step) {
						idx <= limit
					} else {
						limit <= idx
					}

					if (cond) {
						val v = valueOf(idx)
						di.stack(a) = v
						di.stack(a + 3) = v
						cont.programCounter += offset
					} else {
						cont.programCounter += 1
					}
				})


			case OP_FORPREP =>
				// A sBx: R(A)-=R(A+2): pc+=sBx
				val offset = GETARG_sBx(i) + 1

				raw((thread, di, cont) => {
					val init = di.stack(a).checkNumber("'for' initial value must be a number")
					val limit = di.stack(a + 1).checkNumber("'for' limit must be a number")
					val step = di.stack(a + 2).checkNumber("'for' step must be a number")
					di.stack(a) = valueOf(init.toDouble - step.toDouble)
					di.stack(a + 1) = limit
					di.stack(a + 2) = step
					cont.programCounter += offset
				})


			case OP_TFORCALL =>
				assert(GET_OPCODE(code(pc + 1)) == OP_TFORLOOP)
				val cRange = GETARG_C(i) to 1 by -1

				cont(di => {
					val varargs = ValueFactory.varargsOf(di.stack(a + 1), di.stack(a + 2))
					val result = Dispatch.invoke(state, di.stack(a), varargs, a)
					for (c <- cRange) {
						di.stack(a + 2 + c) = result.arg(c)
					}
					// TODO: no fallthrough atm
					// fallthrough to OP_TFORLOOP, avoiding an extra interpreter loop.
				})

			case OP_TFORLOOP =>
				val offset = GETARG_sBx(i) + 1
				raw((thread, di, cont) => {
					val value = di.stack(a + 1)
					if (!value.isNil) {
						di.stack(a) = value
						cont.programCounter += offset
					} else {
						cont.programCounter += 1
					}
				})


			case OP_SETLIST =>
				// A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
				val b = GETARG_B(i)
				val (c, increment) = GETARG_C(i) match {
					case 0 => (GETARG_Ax(code(pc + 1)), 1)
					case c => (c, 0)
				}

				val offset = (c - 1) * LFIELDS_PER_FLUSH

				raw((thread, di, cont) => {
					cont.programCounter += 1 + increment
					val tbl = di.stack(a).checkTable

					if (b == 0) {
						val b = di.top - a - 1
						val m = b - di.extras.count
						tbl.presize(offset + b)

						var j = 1
						while (j <= m) {
							tbl.rawset(offset + j, di.stack(a + j))
							j += 1
						}

						while (j <= b) {
							tbl.rawset(offset + j, di.extras.arg(j - m))
							j += 1
						}
					} else {
						tbl.presize(offset + b)
						for (j <- 1 to b) {
							tbl.rawset(offset + j, di.stack(a + j))
						}
					}
				})


			case OP_CLOSURE =>
				// A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
				val bx = GETARG_Bx(i)
				val newp = p.children(bx)
				cont(di => {
					val newcl = new LuaInterpretedFunction(newp)
					var j = 0
					val nup = newp.upvalues
					while (j < nup) {
						val up = newp.getUpvalue(j)
						newcl.upvalues(j) = if (up.fromLocal) {
							di.getUpvalue(up.index)
						} else di.closure.asInstanceOf[LuaInterpretedFunction].upvalues(up.index)

						j += 1
					}
					di.stack(a) = newcl
				})


			case OP_VARARG =>
				// A B: R(A), R(A+1), ..., R(A+B-1) = vararg
				val b = GETARG_B(i)
				cont(di => {
					if (b == 0) {
						di.top = a + di.varargs.count
						di.extras = di.varargs
					} else for (j <- 1 until b) {
						di.stack(a + j - 1) = di.varargs.arg(j)
					}
				})

			case OP_EXTRAARG =>
				// trap, pc should never point here
				cont(di => throw new IllegalStateException("pc should never point at OP_EXTRAARG"))

			case _ =>
				val opcode = java.lang.StringBuilder()
				Print.printOpcode(opcode, p, pc, true)
				val message = s"Unknown opcode '$opcode' at $pc"
				assert(false, message)
				throw new IllegalStateException(message)
		}
	}

	@throws[LuaError]
	@throws[UnwindThrowable]
	@tailrec
	def execute(state: LuaState, di: DebugFrame, function: LuaInterpretedFunction): Varargs = {
		val ds = DebugState.get(state)
		//		newFrame //todo: labels are not supported
		while (true) {
			// Fetch all info from the function
			val p = function.p
			val upvalues = function.upvalues
			val code = p.code
			val k = p.constants
			// And from the debug info
			val stack = di.stack
			val varargs = di.varargs
			var pc = di.pc

			// process instructions
			while (true) {
				di.pc = pc
				if (state.isInterrupted) state.handleInterrupt()
				ds.onInstruction(di, pc)
				// pull out instruction
				val i = code({
					pc += 1;
					pc - 1
				})
				val a = GETARG_A(i)
				// process the instruction
				GET_OPCODE(i) match {
					case OP_MOVE => // A B: R(A):= R(B)
						stack(a) = stack(GETARG_B(i))

					case OP_LOADK => // A Bx: R(A):= Kst(Bx)
						stack(a) = k(GETARG_Bx(i))

					case OP_LOADKX =>
						// A: R(A) := Kst(extra arg)
						assert(GET_OPCODE(code(pc)) == OP_EXTRAARG)
						val rb = GETARG_Ax(code({
							pc += 1;
							pc - 1
						}))
						stack(a) = k(rb)


					case OP_LOADBOOL =>
						// A B C: R(A):= (Bool)B: if (C) pc++
						stack(a) = if (GETARG_B(i) != 0) TRUE
						else FALSE
						if (GETARG_C(i) != 0) pc += 1 // skip next instruction (if C)


					case OP_LOADNIL =>
						// A B     R(A), R(A+1), ..., R(A+B) := nil
						var a2 = a
						var b = GETARG_B(i)
						while
							stack({
								a2 += 1;
								a2 - 1
							}) = NIL
							{
								b -= 1;
								b + 1
							} > 0
						do ()


					case OP_GETUPVAL => // A B: R(A):= UpValue[B]
						stack(a) = upvalues(GETARG_B(i)).getValue

					case OP_GETTABUP =>
						// A B C: R(A) := UpValue[B][RK(C)]
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.getTable(state, upvalues(b).getValue, getRK(stack, k, c), -b - 1)


					case OP_GETTABLE =>
						// A B C: R(A):= R(B)[RK(C)]
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.getTable(state, stack(b), getRK(stack, k, c), b)


					case OP_SETTABUP =>
						// A B C: UpValue[A][RK(B)] := RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						OperationHelper.setTable(state, upvalues(a).getValue, getRK(stack, k, b), getRK(stack, k, c), -b - 1)


					case OP_SETUPVAL => // A B: UpValue[B]:= R(A)
						upvalues(GETARG_B(i)).setValue(stack(a))

					case OP_SETTABLE =>
						// A B C: R(A)[RK(B)]:= RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						OperationHelper.setTable(state, stack(a), getRK(stack, k, b), getRK(stack, k, c), a)


					case OP_NEWTABLE => // A B C: R(A):= {} (size = B,C)
						stack(a) = new LuaTable(luaO_fb2int(GETARG_B(i)), luaO_fb2int(GETARG_C(i)))

					case OP_SELF =>
						// A B C: R(A+1):= R(B): R(A):= R(B)[RK(C)]
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						val o = stack(b)
						stack(a + 1) = o
						stack(a) = OperationHelper.getTable(state, o, getRK(stack, k, c), b)


					case OP_ADD =>
						// A B C: R(A):= RK(B) + RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.add(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_SUB =>
						// A B C: R(A):= RK(B) - RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.sub(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_MUL =>
						// A B C: R(A):= RK(B) * RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.mul(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_DIV =>
						// A B C: R(A):= RK(B) / RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.div(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_MOD =>
						// A B C: R(A):= RK(B) % RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.mod(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_POW =>
						// A B C: R(A):= RK(B) ^ RK(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						stack(a) = OperationHelper.pow(state, getRK(stack, k, b), getRK(stack, k, c))


					case OP_UNM =>
						// A B: R(A):= -R(B)
						val b = GETARG_B(i)
						stack(a) = OperationHelper.neg(state, getRK(stack, k, b))


					case OP_NOT => // A B: R(A):= not R(B)
						stack(a) = if (stack(GETARG_B(i)).toBoolean) FALSE else TRUE

					case OP_LEN =>
						// A B: R(A):= length of R(B)
						val b = GETARG_B(i)
						stack(a) = OperationHelper.length(state, stack(b))


					case OP_CONCAT =>
						// A B C: R(A):= R(B).. ... ..R(C)
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						di.top = c + 1
						concat(state, di, stack, di.top, c - b + 1)
						stack(a) = stack(b)
						di.top = b


					case OP_JMP => // sBx: pc+=sBx
						pc += doJump(di, i, 0)

					case OP_EQ =>
						// A B C: if ((RK(B) == RK(C)) ~= A) then pc++
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						if (OperationHelper.eq(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							// We assume the next instruction is a jump and read the branch from there.
							pc += doJump(di, code(pc), 1)
						} else pc += 1


					case OP_LT =>
						// A B C: if ((RK(B) <  RK(C)) ~= A) then pc++
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						if (OperationHelper.lt(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							pc += doJump(di, code(pc), 1)
						} else pc += 1


					case OP_LE =>
						// A B C: if ((RK(B) <= RK(C)) ~= A) then pc++
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						if (OperationHelper.le(state, getRK(stack, k, b), getRK(stack, k, c)) == (a != 0)) {
							pc += doJump(di, code(pc), 1)
						} else pc += 1


					case OP_TEST =>
						// A C: if not (R(A) <=> C) then pc++
						if (stack(a).toBoolean == (GETARG_C(i) != 0)) {
							pc += doJump(di, code(pc), 1)
						} else pc += 1


					case OP_TESTSET =>
						// A B C: if (R(B) <=> C) then R(A):= R(B) else pc++
						/* note: doc appears to be reversed */
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						val `val` = stack(b)
						if (`val`.toBoolean == (c != 0)) {
							stack(a) = `val`
							pc += doJump(di, code(pc), 1)
						} else pc += 1


					case OP_CALL =>
						// A B C: R(A), ... ,R(A+C-2):= R(A)(R(A+1), ... ,R(A+B-1)) */
						val b = GETARG_B(i)
						val c = GETARG_C(i)
						val `val` = stack(a)
						if (`val`.isInstanceOf[LuaInterpretedFunction]) {
							val function = `val`.asInstanceOf[LuaInterpretedFunction]
							val newPrototype = function.p
							val newStack = createStack(newPrototype)
							val newFrame = ds.pushInfo
							val args = if (b > 0) setupStack(newPrototype, newStack, stack, a + 1, b - 1) // Exact args count
							else setupStack(newPrototype, newStack, ValueFactory.varargsOfCopy(stack, a + 1, di.top - di.extras.count - (a + 1), di.extras)) // From previous top

							setupFrame(ds, newFrame, function, args, newStack, 0)
							return LuaToScalaCompiler.execute(state, newFrame, function)
						} else nativeCall(state, di, stack, `val`, i, a, b, c)


					case OP_TAILCALL =>
						// A B C: return R(A)(R(A+1), ... ,R(A+B-1))
						val b = GETARG_B(i)

						val `val` = stack(a)
						var args: Varargs = null
						b match {
							case 1 => args = NONE
							case 2 => args = stack(a + 1)
							case _ => val v = di.extras
								args = if (b > 0) {
									ValueFactory.varargsOfCopy(stack, a + 1, b - 1)
								} else {
									ValueFactory.varargsOfCopy(stack, a + 1, di.top - v.count - (a + 1), v)
								} // exact arg count
							// from prev top
						}

						var functionVal: LuaFunction = null
						if (`val`.isInstanceOf[LuaFunction]) functionVal = `val`.asInstanceOf[LuaFunction]
						else {
							functionVal = Dispatch.getCallMetamethod(state, `val`, a)
							args = ValueFactory.varargsOf(`val`, args)
						}

						if (functionVal.isInstanceOf[LuaInterpretedFunction]) {
							val flags = di.flags
							di.cleanup()
							ds.popInfo()

							// FIXME: Return hook???!?

							// Replace the current frame with a new one.
							val function = functionVal.asInstanceOf[LuaInterpretedFunction]
							val di2 = if ((flags & FLAG_FRESH) != 0) ds.pushJavaInfo
							else ds.pushInfo
							setupCall(ds, di2, function, args, (flags & FLAG_FRESH) | FLAG_TAIL)
							return LuaToScalaCompiler.execute(state, di2, function)
						} else {
							val v = Dispatch.invoke(state, functionVal, args)
							di.top = a + v.count
							di.extras = v
						}

					case OP_RETURN =>
						// A B: return R(A), ... ,R(A+B-2) (see note)
						val b = GETARG_B(i)

						val flags = di.flags
						val top = di.top
						val v = di.extras
						di.cleanup()

						val ret = if (b > 0) {
							ValueFactory.varargsOfCopy(stack, a, b - 1)
						} else {
							ValueFactory.varargsOfCopy(stack, a, top - v.count - a, v)
						}

						if ((flags & FLAG_FRESH) != 0) {
							// If we're a fresh invocation then return to the parent.
							return ret
						} else {
							ds.onReturn(di, ret)
							val di2 = ds.getStackUnsafe
							val function = di2.func.asInstanceOf[LuaInterpretedFunction]
							resume(state, di2, function, ret)
							return LuaToScalaCompiler.execute(state, di2, function)
						}

					case OP_FORLOOP =>
						// A sBx: R(A)+=R(A+2): if R(A) <?= R(A+1) then { pc+=sBx: R(A+3)=R(A) }
						val limit = stack(a + 1).checkDouble
						val step = stack(a + 2).checkDouble
						val value = stack(a).checkDouble
						val idx = step + value
						if (if (0 < step) {
							idx <= limit
						} else limit <= idx) {
							val v = valueOf(idx)
							stack(a) = v
							stack(a + 3) = v
							pc += GETARG_sBx(i)
						}


					case OP_FORPREP =>
						// A sBx: R(A)-=R(A+2): pc+=sBx
						val init = stack(a).checkNumber("'for' initial value must be a number")
						val limit = stack(a + 1).checkNumber("'for' limit must be a number")
						val step = stack(a + 2).checkNumber("'for' step must be a number")
						stack(a) = valueOf(init.toDouble - step.toDouble)
						stack(a + 1) = limit
						stack(a + 2) = step
						pc += GETARG_sBx(i)


					case OP_TFORCALL =>
						val varargs = ValueFactory.varargsOf(stack(a + 1), stack(a + 2))
						val result = Dispatch.invoke(state, stack(a), varargs, a)
						for (c <- GETARG_C(i) to 1 by -1) {
							stack(a + 2 + c) = result.arg(c)
						}
						assert(GET_OPCODE(code(pc)) == OP_TFORLOOP)
						// TODO: no fallthrough atm
						// fallthrough to OP_TFORLOOP, avoiding an extra interpreter loop.
						di.pc = pc;
						return LuaToScalaCompiler.execute(state, di, function)

					case OP_TFORLOOP =>
						val value = stack(a + 1)
						if (!value.isNil) {
							stack(a) = value
							pc += GETARG_sBx(i)
						}


					case OP_SETLIST =>
						// A B C: R(A)[(C-1)*FPF+i]:= R(A+i), 1 <= i <= B
						var b = GETARG_B(i)
						var c = GETARG_C(i)
						if (c == 0) c = GETARG_Ax(code({
							pc += 1;
							pc - 1
						}))
						val offset = (c - 1) * LFIELDS_PER_FLUSH
						val tbl = stack(a).checkTable
						if (b == 0) {
							b = di.top - a - 1
							val m = b - di.extras.count
							tbl.presize(offset + b)
							var j = 1

							while (j <= m) {
								tbl.rawset(offset + j, stack(a + j))
								j += 1
							}

							while (j <= b) {
								tbl.rawset(offset + j, di.extras.arg(j - m))
								j += 1
							}
						}
						else {
							tbl.presize(offset + b)
							for (j <- 1 to b) {
								tbl.rawset(offset + j, stack(a + j))
							}
						}


					case OP_CLOSURE =>
						// A Bx: R(A):= closure(KPROTO[Bx], R(A), ... ,R(A+n))
						val newp = p.children(GETARG_Bx(i))
						val newcl = new LuaInterpretedFunction(newp)
						var j = 0
						val nup = newp.upvalues
						while (j < nup) {
							val up = newp.getUpvalue(j)
							newcl.upvalues(j) = if (up.fromLocal) {
								di.getUpvalue(up.index)
							} else upvalues(up.index)

							j += 1
						}
						stack(a) = newcl


					case OP_VARARG =>
						// A B: R(A), R(A+1), ..., R(A+B-1) = vararg
						val b = GETARG_B(i)
						if (b == 0) {
							di.top = a + varargs.count
							di.extras = varargs
						} else for (j <- 1 until b) {
							stack(a + j - 1) = varargs.arg(j)
						}


					case _ =>
						assert(false, "Unknown opcode")
						throw new IllegalStateException("Unknown opcode")

				}
			}
		}

		???
	}
