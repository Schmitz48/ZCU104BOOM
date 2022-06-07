package chipyard.fpga.zcu104

import chisel3._

import freechips.rocketchip.diplomacy._
import freechips.rocketchip.config.{Parameters, Field}
import freechips.rocketchip.tilelink.{TLInwardNode, TLAsyncCrossingSink}

import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.fpgashells.shell.xilinx._
import sifive.fpgashells.clocks._
import sifive.fpgashells.devices.xilinx.xilinxzcu104mig.{XilinxZCU104MIGPads, XilinxZCU104MIGParams, XilinxZCU104MIG}

class SysClock2ZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 300, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    // TODO: I think the 125MHz clock should be used instead... (The 300MHz is maybe for the transceivers??)
    shell.xdc.addPackagePin(io.p, "AH18") // from schematic
    shell.xdc.addPackagePin(io.n, "AH17") // from schematic
    shell.xdc.addIOStandard(io.p, "DIFF_SSTL12")
    shell.xdc.addIOStandard(io.n, "DIFF_SSTL12")
  } }
}
class SysClock2ZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU104ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClock2ZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object ZCU104DDR2Size extends Field[BigInt](0x40000000L * 1) // 2GB
class DDR2ZCU104PlacedOverlay(val shell: ZCU104FPGATestHarness, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxZCU104MIGPads](name, designInput, shellInput)
{
  val size = p(ZCU104DDRSize)

  val migParams = XilinxZCU104MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxZCU104MIG(migParams))
  val ioNode = BundleBridgeSource(() => mig.module.io.cloneType)
  val topIONode = shell { ioNode.makeSink() }
  val ddrUI     = shell { ClockSourceNode(freqMHz = 125) }
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  // since this uses a separate clk/rst need to put an async crossing
  val asyncSink = LazyModule(new TLAsyncCrossingSink())
  val migClkRstNode = BundleBridgeSource(() => new Bundle {
    val clock = Output(Clock())
    val reset = Output(Bool())
  })
  val topMigClkRstIONode = shell { migClkRstNode.makeSink() }

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxZCU104MIGPads(size)

  InModuleBody {
    ioNode.bundle <> mig.module.io

    // setup async crossing
    asyncSink.module.clock := migClkRstNode.bundle.clock
    asyncSink.module.reset := migClkRstNode.bundle.reset
  }

  shell { InModuleBody {
    require (shell.sys_clock2.get.isDefined, "Use of DDRZCU104Overlay depends on SysClock2ZCU104Overlay")
    val (sys, _) = shell.sys_clock2.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)

    // connect the async fifo sync to sys_clock2
    topMigClkRstIONode.bundle.clock := sys.clock
    topMigClkRstIONode.bundle.reset := sys.reset

    val port = topIONode.bundle.port
    io <> port
    ui.clock := port.c0_ddr4_ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.c0_ddr4_ui_clk_sync_rst
    port.c0_sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.c0_ddr4_aresetn := !ar.reset

    // This was just copied from the SiFive example, but it's hard to follow.
    // The pins are emitted in the following order:
    // adr[0->13], we_n, cas_n, ras_n, bg, ba[0->1], reset_n, act_n, ck_c, ck_t, cke, cs_n, odt, dq[0->63], dqs_c[0->7], dqs_t[0->7], dm_dbi_n[0->7]



    // Pin mapped as described in the ZCU104 shematic
    val allddrpins = Seq(
    "AH16", "AG14", "AG15", "AF15", "AF16", "AJ14", "AH14", "AF17", "AK17", "AJ17", "AK14", "AK15", "AL18", "AK18", // adr[0->13]
    "AA16", "AA14", "AD15", "AC16", // we_n, cas_n, ras_n, bg
    "AL15", "AL16", // ba[0->1]
    "AB14", "AC17", "AG18", "AF18", "AD17", "AA15", "AE15", // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
    "AE24", "AE23", "AF22", "AF21", "AG20", "AG19", "AH21", "AG21", "AA20", "AA19", "AD19", "AC18", "AE20", "AD20", "AC19", "AB19", // dq[0->15]
    "AJ22", "AJ21", "AK20", "AJ20", "AJ19", "AJ19", "AL23", "AL22", "AN23", "AM23", "AP23", "AN22", "AP22", "AP21", "AN19", "AM19", // dq[16->31]
    "AC13", "AB13", "AF12", "AE12", "AF13", "AE13", "AE14", "AD14", "AG8", "AF8", "AG10", "AG11", "AH13", "AG13", "AJ11", "AH47", // dq[32->47]
    "AK9", "AJ9", "AK10", "AJ10", "AL12", "AK12", "AL10", "AL11", "AM8", "AM9", "AM10", "AM11", "AP11", "AN11", "AP9", "AP10", // dq[48->63]
    "AG23", "AB18", "AJ23", "AN21", "AD12", "AH9", "AL8", "AN8", // dqs_c[0->7]
    "AF23", "AA18", "AK22", "AM21", "AC12", "AG9", "AK8", "AN9", // dqs_t[0->7]
    "AH22", "AE18", "AL20", "AP19", "AF11", "AH12", "AK13", "AN12") // dm_dbi_n[0->7]
    
    //Pin mapped as described in ZCU104 datasheet
/*     val allddrpins = Seq(
      "AH8", "AG8", "AF8", "AG10", "AG11", "AH9", "AG9", "AH13", "AK10", "AJ10", "AL8", "AK8", "AL12", "AK12", // adr[0->13]
      "AC12", "AE12", "AF11", "AE14", // we_n, cas_n, ras_n, bg
      "AL10", "AL11", // ba[0->1]
      "AF12", "AD14", "AH11", "AH11", "AB13", "AD12", "AF10", // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
      "AG14", "AG15", "AF15", "AF16", "AF17", "AE17", "AG18", "AF18", "AD16", "AD17", "AB14", "AA14", "AB15", "AB16", "AC16", "AC17", // dq[0->15]
      "AJ15", "AJ16", "AK17", "AJ17", "AL18", "AK18", "AL15", "AL16", "AN16", "AN17", "AP15", "AP16", "AN18", "AM18", "AP13", "AN31", // dq[16->31]
      "AA20", "AA19", "AD19", "AC18", "AE20", "AD20", "AC19", "AB19", "AE24", "AE23", "AF22", "AF21", "AG20", "AG19", "AH21", "AG21", // dq[32->47]
      "AJ22", "AJ21", "AK20", "AJ20", "AK19", "AJ19", "AL23", "AL22", "AN23", "AM23", "AP23", "AN22", "AP22", "AP21", "AN19", "AM19", // dq[48->63]
      "AJ14", "AA15", "AK14", "AN14", "AB18", "AG23", "AK23", "AN21", // dqs_c[0->7]
      "AH14", "AA16", "AK15", "AM14", "AA18", "AF23", "AK22", "AM21", // dqs_t[0->7]
      "AH18", "AD15", "AM16", "AP18", "AE18", "AH22", "AL20", "AP19") // dm_dbi_n[0->7]
 */
    (IOPin.of(io) zip allddrpins) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}

class DDR2ZCU104ShellPlacer(shell: ZCU104FPGATestHarness, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[ZCU104FPGATestHarness] {
  def place(designInput: DDRDesignInput) = new DDR2ZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

