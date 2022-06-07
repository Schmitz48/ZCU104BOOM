package chipyard.fpga.zcu104.bringup

import chisel3._

import freechips.rocketchip.subsystem._
import freechips.rocketchip.system._
import freechips.rocketchip.config.Parameters
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._

import chipyard.{DigitalTop, DigitalTopModule}

// ------------------------------------
// Bringup ZCU104 DigitalTop
// ------------------------------------

class BringupZCU104DigitalTop(implicit p: Parameters) extends DigitalTop
  with sifive.blocks.devices.i2c.HasPeripheryI2C
  with testchipip.HasPeripheryTSIHostWidget
{
  override lazy val module = new BringupZCU104DigitalTopModule(this)
}

class BringupZCU104DigitalTopModule[+L <: BringupZCU104DigitalTop](l: L) extends DigitalTopModule(l)
  with sifive.blocks.devices.i2c.HasPeripheryI2CModuleImp
