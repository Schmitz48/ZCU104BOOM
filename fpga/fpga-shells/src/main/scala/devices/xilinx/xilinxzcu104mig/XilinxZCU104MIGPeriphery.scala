// See LICENSE for license details.
package sifive.fpgashells.devices.xilinx.xilinxzcu104mig

import Chisel._
import freechips.rocketchip.config._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.diplomacy.{LazyModule, LazyModuleImp, AddressRange}

case object MemoryXilinxDDRKey extends Field[XilinxZCU104MIGParams]

trait HasMemoryXilinxZCU104MIG { this: BaseSubsystem =>
  val module: HasMemoryXilinxZCU104MIGModuleImp

  val xilinxzcu104mig = LazyModule(new XilinxZCU104MIG(p(MemoryXilinxDDRKey)))

  xilinxzcu104mig.node := mbus.toDRAMController(Some("xilinxzcu104mig"))()
}

trait HasMemoryXilinxZCU104MIGBundle {
  val xilinxzcu104mig: XilinxZCU104MIGIO
  def connectXilinxZCU104MIGToPads(pads: XilinxZCU104MIGPads) {
    pads <> xilinxzcu104mig
  }
}

trait HasMemoryXilinxZCU104MIGModuleImp extends LazyModuleImp
    with HasMemoryXilinxZCU104MIGBundle {
  val outer: HasMemoryXilinxZCU104MIG
  val ranges = AddressRange.fromSets(p(MemoryXilinxDDRKey).address)
  require (ranges.size == 1, "DDR range must be contiguous")
  val depth = ranges.head.size
  val xilinxzcu104mig = IO(new XilinxZCU104MIGIO(depth))

  xilinxzcu104mig <> outer.xilinxzcu104mig.module.io.port
}
