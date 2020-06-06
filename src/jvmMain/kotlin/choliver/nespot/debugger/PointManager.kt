package choliver.nespot.debugger

import choliver.nespot.common.Address
import choliver.nespot.debugger.PointManager.Point.Breakpoint
import choliver.nespot.debugger.PointManager.Point.Watchpoint

class PointManager {
  sealed class Point(open val num: Int) {
    data class Breakpoint(override val num: Int, val pc: Address) : Point(num)
    data class Watchpoint(override val num: Int, val addr: Address) : Point(num)
  }

  private var nextPointNum = 1
  private val points = mutableMapOf<Int, Point>()
  private val _breakpoints = mutableMapOf<Address, Breakpoint>()
  private val _watchpoints = mutableMapOf<Address, Watchpoint>()

  val breakpoints get() = _breakpoints.toMap()
  val watchpoints get() = _watchpoints.toMap()

  fun addBreakpoint(pc: Address): Breakpoint {
    val bp = Breakpoint(nextPointNum++, pc)
    points[bp.num] = bp
    _breakpoints[bp.pc] = bp
    return bp
  }

  fun addWatchpoint(addr: Address): Watchpoint {
    val wp = Watchpoint(nextPointNum++, addr)
    points[wp.num] = wp
    _watchpoints[wp.addr] = wp
    return wp
  }

  fun removeAll() {
    points.clear()
    _breakpoints.clear()
    _watchpoints.clear()
  }

  fun remove(idx: Int): Point? {
    val removed = points.remove(idx)
    when (removed) {
      is Breakpoint -> _breakpoints.remove(removed.pc)
      is Watchpoint -> _watchpoints.remove(removed.addr)
      null -> {}
    }
    return removed
  }
}
