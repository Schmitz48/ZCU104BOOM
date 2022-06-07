// See LICENSE for license details.
package sifive.fpgashells.shell.xilinx

import chisel3._
import chisel3.experimental.{attach, Analog, IO}
import freechips.rocketchip.config._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.SyncResetSynchronizerShiftReg
import sifive.fpgashells.clocks._
import sifive.fpgashells.shell._
import sifive.fpgashells.ip.xilinx._
import sifive.blocks.devices.chiplink._
import sifive.fpgashells.devices.xilinx.xilinxzcu104mig._
import sifive.fpgashells.devices.xilinx.xdma._
import sifive.fpgashells.ip.xilinx.xxv_ethernet._

// TODO: ZCU104 sysClock @ 125 MHz
class SysClockZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 125, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "F23") 
    shell.xdc.addPackagePin(io.n, "E23") 
    shell.xdc.addIOStandard(io.p, "LVDS")
    shell.xdc.addIOStandard(io.n, "LVDS") 
  } }
}
class SysClockZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU104ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new SysClockZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// ZCU104 DDR clock @ 300MHz
class DDRClockZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput)
{
  val node = shell { ClockSourceNode(freqMHz = 300, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "AH18") // 300MHz clock
    shell.xdc.addPackagePin(io.n, "AH17") //300MHz clock
    shell.xdc.addIOStandard(io.p, "DIFF_SSTL12")
    shell.xdc.addIOStandard(io.n, "DIFF_SSTL12") 
  } }
}
class DDRClockZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU104ShellBasicOverlays]
{
    def place(designInput: ClockInputDesignInput) = new DDRClockZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}


class RefClockZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ClockInputDesignInput, val shellInput: ClockInputShellInput)
  extends LVDSClockInputXilinxPlacedOverlay(name, designInput, shellInput) {
  val node = shell { ClockSourceNode(freqMHz = 125, jitterPS = 50)(ValName(name)) }

  shell { InModuleBody {
    shell.xdc.addPackagePin(io.p, "H11")
    shell.xdc.addPackagePin(io.n, "G11")
    shell.xdc.addIOStandard(io.p, "LVDS")
    shell.xdc.addIOStandard(io.n, "LVDS")
  } }
}
class RefClockZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ClockInputShellInput)(implicit val valName: ValName)
  extends ClockInputShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: ClockInputDesignInput) = new RefClockZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// NOTE: Use Female PMOD for SDIO
// See https://digilent.com/shop/pmod-microsd-microsd-card-slot/ for schematics etc.
// Used same PMOD pin layout as VCU118
// Placed on PMOD0
class SDIOZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: SPIDesignInput, val shellInput: SPIShellInput)
  extends SDIOXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {

    // I think the pinout should be the following (according to digilent pinout):
    // https://digilent.com/reference/pmod/pmodmicrosd/reference-manual?redirect=1
    // https://digilent.com/reference/_media/reference/pmod/pmodmicrosd/pmodmicrosd_sch.pdf
    //
    // Pins connected to FPGA package are 0 indexed, PMOD pins are 1 indexed
    // PMOD - SPI pin
    //   1  - CS   (dat(3))
    //   2  - DAT1 (dat(1))
    //   3  - MOSI 
    //   4  - DAT2 (dat(2))
    //   5  - MISO (dat(0))
    //   6  - CD (CS)
    //   7  - SCK
    //   8  - NC/EN (Enable for inrush current limiting, chip not connected for digilent microSD card PMOD)
    //   9  - GND
    //   10 - GND
    //   11 - VCC
    //   12 - VCC

    // As configured for VCU118 project, Doesn't make sense for digilents microSD PMOD...
    val packagePinsWithPackageIOs = Seq(("H7", IOPin(io.spi_clk)),    // PMOD0_3
                                        ("H8", IOPin(io.spi_cs)),     // PMOD0_1
                                        ("G7", IOPin(io.spi_dat(0))), // PMOD0_2
                                        ("G6", IOPin(io.spi_dat(1))), // PMOD0_4
                                        ("H6", IOPin(io.spi_dat(2))), // PMOD0_5
                                        ("G8", IOPin(io.spi_dat(3)))) // PMOD0_0

    //Corrected
    // val packagePinsWithPackageIOs = Seq(("H7", IOPin(io.spi_clk)),    // PMOD0_6
    //                                     ("J6", IOPin(io.spi_cs)),     // PMOD0_5
    //                                     ("G7", IOPin(io.spi_dat(0))), // PMOD0_4
    //                                     ("G6", IOPin(io.spi_dat(1))), // PMOD0_1
    //                                     ("H6", IOPin(io.spi_dat(2))), // PMOD0_3
    //                                     ("G8", IOPin(io.spi_dat(3)))) // PMOD0_0     

    // val packagePinsWithPackageIOs = Seq(("J6", IOPin(io.spi_clk)),    // PMOD0_6
    //                                     ("H6", IOPin(io.spi_cs)),     // PMOD0_5
    //                                     ("G6", IOPin(io.spi_dat(0))), // PMOD0_4
    //                                     ("H8", IOPin(io.spi_dat(1))), // PMOD0_1
    //                                     ("H7", IOPin(io.spi_dat(2))), // PMOD0_3
    //                                     ("G8", IOPin(io.spi_dat(3)))) // PMOD0_0                                   



    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS33")
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    } }
  } }
}
class SDIOZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: SPIShellInput)(implicit val valName: ValName)
  extends SPIShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: SPIDesignInput) = new SDIOZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class SPIFlashZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: SPIFlashDesignInput, val shellInput: SPIFlashShellInput)
  extends SPIFlashXilinxPlacedOverlay(name, designInput, shellInput)
{

  shell { InModuleBody {
    /*val packagePinsWithPackageIOs = Seq(("AF13", IOPin(io.qspi_sck)),
      ("AJ11", IOPin(io.qspi_cs)),
      ("AP11", IOPin(io.qspi_dq(0))),
      ("AN11", IOPin(io.qspi_dq(1))),
      ("AM11", IOPin(io.qspi_dq(2))),
      ("AL11", IOPin(io.qspi_dq(3))))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
      shell.xdc.addIOB(io)
    } }
    packagePinsWithPackageIOs drop 1 foreach { case (pin, io) => {
      shell.xdc.addPullup(io)
    } }
*/
  } }
}
class SPIFlashZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: SPIFlashShellInput)(implicit val valName: ValName)
  extends SPIFlashShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: SPIFlashDesignInput) = new SPIFlashZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class UARTZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: UARTDesignInput, val shellInput: UARTShellInput)
  extends UARTXilinxPlacedOverlay(name, designInput, shellInput, true)
{
  shell { InModuleBody {
    val packagePinsWithPackageIOs = Seq(("A19", IOPin(io.ctsn.get)),
                                        ("C18", IOPin(io.rtsn.get)),
                                        ("A20", IOPin(io.rxd)),
                                        ("C19", IOPin(io.txd)))

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      shell.xdc.addPackagePin(io, pin)
      shell.xdc.addIOStandard(io, "LVCMOS18")
      shell.xdc.addIOB(io)
    } }
  } }
}
class UARTZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: UARTShellInput)(implicit val valName: ValName)
  extends UARTShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: UARTDesignInput) = new UARTZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// NOTE: ZCU104 doesn't have QSFP
class QSFP1ZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: EthernetDesignInput, val shellInput: EthernetShellInput)
  extends EthernetUltraScalePlacedOverlay(name, designInput, shellInput, XXVEthernetParams(name = name, speed   = 10, dclkMHz = 125))
{
  // val dclkSource = shell { BundleBridgeSource(() => Clock()) }
  // val dclkSink = dclkSource.makeSink()
  // InModuleBody {
  //   dclk := dclkSink.bundle
  // }
  // shell { InModuleBody {
  //   dclkSource.bundle := shell.ref_clock.get.get.overlayOutput.node.out(0)._1.clock
  //   shell.xdc.addPackagePin(io.tx_p, "V7")
  //   shell.xdc.addPackagePin(io.tx_n, "V6")
  //   shell.xdc.addPackagePin(io.rx_p, "Y2")
  //   shell.xdc.addPackagePin(io.rx_n, "Y1")
  //   shell.xdc.addPackagePin(io.refclk_p, "W9")
  //   shell.xdc.addPackagePin(io.refclk_n, "W8")
  // } }
}
class QSFP1ZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: EthernetShellInput)(implicit val valName: ValName)
  extends EthernetShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: EthernetDesignInput) = new QSFP1ZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// NOTE: ZCU104 doesn't have QSFP
class QSFP2ZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: EthernetDesignInput, val shellInput: EthernetShellInput)
  extends EthernetUltraScalePlacedOverlay(name, designInput, shellInput, XXVEthernetParams(name = name, speed   = 10, dclkMHz = 125))
{
  // val dclkSource = shell { BundleBridgeSource(() => Clock()) }
  // val dclkSink = dclkSource.makeSink()
  // InModuleBody {
  //   dclk := dclkSink.bundle
  // }
  // shell { InModuleBody {
  //   dclkSource.bundle := shell.ref_clock.get.get.overlayOutput.node.out(0)._1.clock
  //   shell.xdc.addPackagePin(io.tx_p, "L5")
  //   shell.xdc.addPackagePin(io.tx_n, "L4")
  //   shell.xdc.addPackagePin(io.rx_p, "T2")
  //   shell.xdc.addPackagePin(io.rx_n, "T1")
  //   shell.xdc.addPackagePin(io.refclk_p, "R9")
  //   shell.xdc.addPackagePin(io.refclk_n, "R8")
  // } }
}
class QSFP2ZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: EthernetShellInput)(implicit val valName: ValName)
  extends EthernetShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: EthernetDesignInput) = new QSFP2ZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object LEDZCU104PinConstraints {
  val pins = Seq("D5", "D6", "A5", "B5")
}
class LEDZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: LEDDesignInput, val shellInput: LEDShellInput)
  extends LEDXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(LEDZCU104PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class LEDZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: LEDShellInput)(implicit val valName: ValName)
  extends LEDShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: LEDDesignInput) = new LEDZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object ButtonZCU104PinConstraints {
  val pins = Seq("B4", "C4", "B3", "C3")
}
class ButtonZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ButtonDesignInput, val shellInput: ButtonShellInput)
  extends ButtonXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(ButtonZCU104PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS18")
class ButtonZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ButtonShellInput)(implicit val valName: ValName)
  extends ButtonShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: ButtonDesignInput) = new ButtonZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

object SwitchZCU104PinConstraints {
  val pins = Seq("E4", "C4", "F5", "F4")
}
class SwitchZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: SwitchDesignInput, val shellInput: SwitchShellInput)
  extends SwitchXilinxPlacedOverlay(name, designInput, shellInput, packagePin = Some(SwitchZCU104PinConstraints.pins(shellInput.number)), ioStandard = "LVCMOS12")
class SwitchZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: SwitchShellInput)(implicit val valName: ValName)
  extends SwitchShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: SwitchDesignInput) = new SwitchZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

//NOTE: Do we need chiplink on the FMC connector?
class ChipLinkZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: ChipLinkDesignInput, val shellInput: ChipLinkShellInput)
  extends ChipLinkXilinxPlacedOverlay(name, designInput, shellInput, rxPhase= -120, txPhase= -90, rxMargin=0.6, txMargin=0.5)
{
  val ereset_n = shell { InModuleBody {
    val ereset_n = IO(Analog(1.W))
    ereset_n.suggestName("ereset_n")

    // Commented out for ZCU104
    // val pin = IOPin(ereset_n, 0)
    // shell.xdc.addPackagePin(pin, "BC8")
    // shell.xdc.addIOStandard(pin, "LVCMOS18")
    // shell.xdc.addTermination(pin, "NONE")
    // shell.xdc.addPullup(pin)

    val iobuf = Module(new IOBUF)
    iobuf.suggestName("chiplink_ereset_iobuf")
    attach(ereset_n, iobuf.io.IO)
    iobuf.io.T := true.B // !oe
    iobuf.io.I := false.B

    iobuf.io.O
  } }
  
  // NOTE: Commented out for ZCU104
  // shell { InModuleBody {
  //   val dir1 = Seq("BC9", "AV8", "AV9", /* clk, rst, send */
  //                  "AY9",  "BA9",  "BF10", "BF9",  "BC11", "BD11", "BD12", "BE12",
  //                  "BF12", "BF11", "BE14", "BF14", "BD13", "BE13", "BC15", "BD15",
  //                  "BE15", "BF15", "BA14", "BB14", "BB13", "BB12", "BA16", "BA15",
  //                  "BC14", "BC13", "AY8",  "AY7",  "AW8",  "AW7",  "BB16", "BC16")
  //   val dir2 = Seq("AV14", "AK13", "AK14", /* clk, rst, send */
  //                  "AR14", "AT14", "AP12", "AR12", "AW12", "AY12", "AW11", "AY10",
  //                  "AU11", "AV11", "AW13", "AY13", "AN16", "AP16", "AP13", "AR13",
  //                  "AT12", "AU12", "AK15", "AL15", "AL14", "AM14", "AV10", "AW10",
  //                  "AN15", "AP15", "AK12", "AL12", "AM13", "AM12", "AJ13", "AJ12")
  //   (IOPin.of(io.b2c) zip dir1) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  //   (IOPin.of(io.c2b) zip dir2) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }
  // } }
}
class ChipLinkZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: ChipLinkShellInput)(implicit val valName: ValName)
  extends ChipLinkShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: ChipLinkDesignInput) = new ChipLinkZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// TODO: JTAG is untested
// NOTE: Commented out for ZCU104
// Placed on PMOD1
class JTAGDebugZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: JTAGDebugDesignInput, val shellInput: JTAGDebugShellInput)
  extends JTAGDebugXilinxPlacedOverlay(name, designInput, shellInput)
{
  shell { InModuleBody {
    //PMOD pin map:
    // TCK: 2
    // TMS: 5
    // TDI: 4
    // TDO: 0
    // RST: 1
    val pin_locations = Map(
      "PMOD_1"   -> Seq("K8",      "M10",      "L10",      "J9",      "K9"))
    val pins      = Seq(io.jtag_TCK, io.jtag_TMS, io.jtag_TDI, io.jtag_TDO, io.srst_n)

    shell.sdc.addClock("JTCK", IOPin(io.jtag_TCK), 10)
    shell.sdc.addGroup(clocks = Seq("JTCK"))
    shell.xdc.clockDedicatedRouteFalse(IOPin(io.jtag_TCK))

    val pin_voltage:String = "LVCMOS33"

    (pin_locations(shellInput.location.get) zip pins) foreach { case (pin_location, ioport) =>
      val io = IOPin(ioport)
      shell.xdc.addPackagePin(io, pin_location)
      shell.xdc.addIOStandard(io, pin_voltage)
      shell.xdc.addPullup(io)
      shell.xdc.addIOB(io)
    }
  } }
}
class JTAGDebugZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: JTAGDebugShellInput)(implicit val valName: ValName)
  extends JTAGDebugShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: JTAGDebugDesignInput) = new JTAGDebugZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

// NOTE: Commented out for ZCU104
class cJTAGDebugZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: cJTAGDebugDesignInput, val shellInput: cJTAGDebugShellInput)
  extends cJTAGDebugXilinxPlacedOverlay(name, designInput, shellInput)
{
  // shell { InModuleBody {
  //   shell.sdc.addClock("JTCKC", IOPin(io.cjtag_TCKC), 10)
  //   shell.sdc.addGroup(clocks = Seq("JTCKC"))
  //   shell.xdc.clockDedicatedRouteFalse(IOPin(io.cjtag_TCKC))
  //   val packagePinsWithPackageIOs = Seq(("AW11", IOPin(io.cjtag_TCKC)),
  //                                       ("AP13", IOPin(io.cjtag_TMSC)),
  //                                       ("AY10", IOPin(io.srst_n)))

  //   packagePinsWithPackageIOs foreach { case (pin, io) => {
  //     shell.xdc.addPackagePin(io, pin)
  //     shell.xdc.addIOStandard(io, "LVCMOS18")
  //   } }
  //     shell.xdc.addPullup(IOPin(io.cjtag_TCKC))
  //     shell.xdc.addPullup(IOPin(io.srst_n))
  // } }
}
class cJTAGDebugZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: cJTAGDebugShellInput)(implicit val valName: ValName)
  extends cJTAGDebugShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: cJTAGDebugDesignInput) = new cJTAGDebugZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

class JTAGDebugBScanZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: JTAGDebugBScanDesignInput, val shellInput: JTAGDebugBScanShellInput)
  extends JTAGDebugBScanXilinxPlacedOverlay(name, designInput, shellInput)
class JTAGDebugBScanZCU104ShellPlacer(val shell: ZCU104ShellBasicOverlays, val shellInput: JTAGDebugBScanShellInput)(implicit val valName: ValName)
  extends JTAGDebugBScanShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: JTAGDebugBScanDesignInput) = new JTAGDebugBScanZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}

case object ZCU104DDRSize extends Field[BigInt](0x40000000L * 1) // 2GB
class DDRZCU104PlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: DDRDesignInput, val shellInput: DDRShellInput)
  extends DDRPlacedOverlay[XilinxZCU104MIGPads](name, designInput, shellInput)
{
  val size = p(ZCU104DDRSize)

  val migParams = XilinxZCU104MIGParams(address = AddressSet.misaligned(di.baseAddress, size))
  val mig = LazyModule(new XilinxZCU104MIG(migParams))
  val ioNode = BundleBridgeSource(() => mig.module.io.cloneType)
  val topIONode = shell { ioNode.makeSink() }
  val ddrUI     = shell { ClockSourceNode(freqMHz = 200) }
  val areset    = shell { ClockSinkNode(Seq(ClockSinkParameters())) }
  areset := designInput.wrangler := ddrUI

  def overlayOutput = DDROverlayOutput(ddr = mig.node)
  def ioFactory = new XilinxZCU104MIGPads(size)

  InModuleBody { ioNode.bundle <> mig.module.io }

  shell { InModuleBody {
    require (shell.ddr_clock.get.isDefined, "Use of DDRZCU104Overlay depends on DDRClockZCU104Overlay")
    val (sys, _) = shell.ddr_clock.get.get.overlayOutput.node.out(0)
    val (ui, _) = ddrUI.out(0)
    val (ar, _) = areset.in(0)
    val port = topIONode.bundle.port
    io <> port
    ui.clock := port.c0_ddr4_ui_clk
    ui.reset := /*!port.mmcm_locked ||*/ port.c0_ddr4_ui_clk_sync_rst
    port.c0_sys_clk_i := sys.clock.asUInt
    port.sys_rst := sys.reset // pllReset
    port.c0_ddr4_aresetn := !ar.reset

  // Copied from CustomOverlays.scala
  // This should however, be the correct DDR pin layout
  //
  // This was just copied from the SiFive example, but it's hard to follow.
  // The pins are emitted in the following order:
  // adr[0->13], we_n, cas_n, ras_n, bg, ba[0->1], reset_n, act_n, ck_c, ck_t, cke, cs_n, odt, dq[0->63], dqs_c[0->7], dqs_t[0->7], dm_dbi_n[0->7]
  //
  // Pin mapped as described in the ZCU104 shematic
  val allddrpins = Seq(
  "AH16", "AG14", "AG15", "AF15", "AF16", "AJ14", "AH14", "AF17", "AK17", "AJ17", "AK14", "AK15", "AL18", "AK18", // adr[0->13]
  "AA16", "AA14", "AD15", "AC16", "AB16", // we_n, cas_n, ras_n, bg0, bg1
  "AL15", "AL16", // ba[0->1]
  "AB14", "AC17", "AG18", "AF18", "AD17", "AA15", "AE15", // reset_n, act_n, ck_c, ck_t, cke, cs_n, odt
  "AE24", "AE23", "AF22", "AF21", "AG20", "AG19", "AH21", "AG21", "AA20", "AA19", "AD19", "AC18", "AE20", "AD20", "AC19", "AB19", // dq[0->15]
  "AJ22", "AJ21", "AK20", "AJ20", "AK19", "AJ19", "AL23", "AL22", "AN23", "AM23", "AP23", "AN22", "AP22", "AP21", "AN19", "AM19", // dq[16->31]
  "AC13", "AB13", "AF12", "AE12", "AF13", "AE13", "AE14", "AD14", "AG8", "AF8", "AG10", "AG11", "AH13", "AG13", "AJ11", "AH11", // dq[32->47]
  "AK9", "AJ9", "AK10", "AJ10", "AL12", "AK12", "AL10", "AL11", "AM8", "AM9", "AM10", "AM11", "AP11", "AN11", "AP9", "AP10", // dq[48->63]
  "AG23", "AB18", "AK23", "AN21", "AD12", "AH9", "AL8", "AN8", // dqs_c[0->7]
  "AF23", "AA18", "AK22", "AM21", "AC12", "AG9", "AK8", "AN9", // dqs_t[0->7]
  "AH22", "AE18", "AL20", "AP19", "AF11", "AH12", "AK13", "AN12") // dm_dbi_n[0->7]


// PIN mapped as described in user guide for ZCU104
/*   val allddrpins = Seq(
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
    "AH18", "AD15", "AM16", "AP18", "AE18", "AH22", "AL20", "AP19") // dm_dbi_n[0->7] */

  (IOPin.of(io) zip allddrpins) foreach { case (io, pin) => shell.xdc.addPackagePin(io, pin) }



  } }

  shell.sdc.addGroup(pins = Seq(mig.island.module.blackbox.io.c0_ddr4_ui_clk))
}
class DDRZCU104ShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: DDRShellInput)(implicit val valName: ValName)
  extends DDRShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: DDRDesignInput) = new DDRZCU104PlacedOverlay(shell, valName.name, designInput, shellInput)
}
/* ZCU104 does not support PCIe without adding soft IPs in PL
class PCIeZCU104FMCPlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: PCIeDesignInput, val shellInput: PCIeShellInput)
  extends PCIeUltraScalePlacedOverlay(name, designInput, shellInput, XDMAParams(
    name     = "fmc_xdma",
    location = "X0Y3",
    bars     = designInput.bars,
    control  = designInput.ecam,
    bases    = designInput.bases,
    lanes    = 4))
{
  shell { InModuleBody {
    // Work-around incorrectly pre-assigned pins
    IOPin.of(io).foreach { shell.xdc.addPackagePin(_, "") }

    // We need some way to connect both of these to reach x8
    val ref126 = Seq("V38",  "V39")  /* [pn] GBT0 Bank 126 */
    val ref121 = Seq("AK38", "AK39") /* [pn] GBT0 Bank 121 */
    val ref = ref126

    // Bank 126 (DP5, DP6, DP4, DP7), Bank 121 (DP3, DP2, DP1, DP0)
    val rxp = Seq("U45", "R45", "W45", "N45", "AJ45", "AL45", "AN45", "AR45") /* [0-7] */
    val rxn = Seq("U46", "R46", "W46", "N46", "AJ46", "AL46", "AN46", "AR46") /* [0-7] */
    val txp = Seq("P42", "M42", "T42", "K42", "AL40", "AM42", "AP42", "AT42") /* [0-7] */
    val txn = Seq("P43", "M43", "T43", "K43", "AL41", "AM43", "AP43", "AT43") /* [0-7] */

    def bind(io: Seq[IOPin], pad: Seq[String]) {
      (io zip pad) foreach { case (io, pad) => shell.xdc.addPackagePin(io, pad) }
    }

    bind(IOPin.of(io.refclk), ref)
    // We do these individually so that zip falls off the end of the lanes:
    bind(IOPin.of(io.lanes.pci_exp_txp), txp)
    bind(IOPin.of(io.lanes.pci_exp_txn), txn)
    bind(IOPin.of(io.lanes.pci_exp_rxp), rxp)
    bind(IOPin.of(io.lanes.pci_exp_rxn), rxn)
  } }
}
class PCIeZCU104FMCShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: PCIeShellInput)(implicit val valName: ValName)
  extends PCIeShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: PCIeDesignInput) = new PCIeZCU104FMCPlacedOverlay(shell, valName.name, designInput, shellInput)
}

class PCIeZCU104EdgePlacedOverlay(val shell: ZCU104ShellBasicOverlays, name: String, val designInput: PCIeDesignInput, val shellInput: PCIeShellInput)
  extends PCIeUltraScalePlacedOverlay(name, designInput, shellInput, XDMAParams(
    name     = "edge_xdma",
    location = "X1Y2",
    bars     = designInput.bars,
    control  = designInput.ecam,
    bases    = designInput.bases,
    lanes    = 8))
{
  shell { InModuleBody {
    // Work-around incorrectly pre-assigned pins
    IOPin.of(io).foreach { shell.xdc.addPackagePin(_, "") }

    // PCIe Edge connector U2
    //   Lanes 00-03 Bank 227
    //   Lanes 04-07 Bank 226
    //   Lanes 08-11 Bank 225
    //   Lanes 12-15 Bank 224

    // FMC+ J22
    val ref227 = Seq("AC9", "AC8")  /* [pn]  Bank 227 PCIE_CLK2_*/
    val ref = ref227

    // PCIe Edge connector U2 : Bank 227, 226
    val rxp = Seq("AA4", "AB2", "AC4", "AD2", "AE4", "AF2", "AG4", "AH2") // [0-7]
    val rxn = Seq("AA3", "AB1", "AC3", "AD1", "AE3", "AF1", "AG3", "AH1") // [0-7]
    val txp = Seq("Y7", "AB7", "AD7", "AF7", "AH7", "AK7", "AM7", "AN5") // [0-7]
    val txn = Seq("Y6", "AB6", "AD6", "AF6", "AH6", "AK6", "AM6", "AN4") // [0-7]

    def bind(io: Seq[IOPin], pad: Seq[String]) {
      (io zip pad) foreach { case (io, pad) => shell.xdc.addPackagePin(io, pad) }
    }

    bind(IOPin.of(io.refclk), ref)
    // We do these individually so that zip falls off the end of the lanes:
    bind(IOPin.of(io.lanes.pci_exp_txp), txp)
    bind(IOPin.of(io.lanes.pci_exp_txn), txn)
    bind(IOPin.of(io.lanes.pci_exp_rxp), rxp)
    bind(IOPin.of(io.lanes.pci_exp_rxn), rxn)
  } }
}
class PCIeZCU104EdgeShellPlacer(shell: ZCU104ShellBasicOverlays, val shellInput: PCIeShellInput)(implicit val valName: ValName)
  extends PCIeShellPlacer[ZCU104ShellBasicOverlays] {
  def place(designInput: PCIeDesignInput) = new PCIeZCU104EdgePlacedOverlay(shell, valName.name, designInput, shellInput)
} */

abstract class ZCU104ShellBasicOverlays()(implicit p: Parameters) extends UltraScaleShell{
  // PLL reset causes
  val pllReset = InModuleBody { Wire(Bool()) }

  val ddr_clock = Overlay(ClockInputOverlayKey, new DDRClockZCU104ShellPlacer(this, ClockInputShellInput()))
  val sys_clock = Overlay(ClockInputOverlayKey, new SysClockZCU104ShellPlacer(this, ClockInputShellInput()))
  //val ref_clock = Overlay(ClockInputOverlayKey, new RefClockZCU104ShellPlacer(this, ClockInputShellInput())) // NOTE: ZCU104 doesn't need refclk, only used for QSPF
  val led       = Seq.tabulate(8)(i => Overlay(LEDOverlayKey, new LEDZCU104ShellPlacer(this, LEDShellInput(color = "red", number = i))(valName = ValName(s"led_$i"))))
  val switch    = Seq.tabulate(4)(i => Overlay(SwitchOverlayKey, new SwitchZCU104ShellPlacer(this, SwitchShellInput(number = i))(valName = ValName(s"switch_$i"))))
  val button    = Seq.tabulate(5)(i => Overlay(ButtonOverlayKey, new ButtonZCU104ShellPlacer(this, ButtonShellInput(number = i))(valName = ValName(s"button_$i"))))
  val ddr       = Overlay(DDROverlayKey, new DDRZCU104ShellPlacer(this, DDRShellInput()))

  // NOTE: ZCU104 commented because we don't need it
  // val qsfp1     = Overlay(EthernetOverlayKey, new QSFP1ZCU104ShellPlacer(this, EthernetShellInput()))
  // val qsfp2     = Overlay(EthernetOverlayKey, new QSFP2ZCU104ShellPlacer(this, EthernetShellInput()))
  val chiplink  = Overlay(ChipLinkOverlayKey, new ChipLinkZCU104ShellPlacer(this, ChipLinkShellInput()))
  //val spi_flash = Overlay(SPIFlashOverlayKey, new SPIFlashZCU104ShellPlacer(this, SPIFlashShellInput()))
  //SPI Flash not functional
}

case object ZCU104ShellPMOD extends Field[String]("JTAG")
case object ZCU104ShellPMOD2 extends Field[String]("JTAG")

class WithZCU104ShellPMOD(device: String) extends Config((site, here, up) => {
  case ZCU104ShellPMOD => device
})

// Change JTAG pinouts to ZCU104 J53
// Due to the level shifter is from 1.2V to 3.3V, the frequency of JTAG should be slow down to 1Mhz
class WithZCU104ShellPMOD2(device: String) extends Config((site, here, up) => {
  case ZCU104ShellPMOD2 => device
})

class WithZCU104ShellPMODJTAG extends WithZCU104ShellPMOD("JTAG")
class WithZCU104ShellPMODSDIO extends WithZCU104ShellPMOD("SDIO")

// Reassign JTAG pinouts location to PMOD J53
class WithZCU104ShellPMOD2JTAG extends WithZCU104ShellPMOD2("PMODJ53_JTAG")

class ZCU104Shell()(implicit p: Parameters) extends ZCU104ShellBasicOverlays
{
  val pmod_is_sdio  = p(ZCU104ShellPMOD) == "SDIO"
  //val pmod_j53_is_jtag = p(ZCU104ShellPMOD2) == "PMODJ53_JTAG"
  //val jtag_location = Some(if (pmod_is_sdio) (if (pmod_j53_is_jtag) "PMOD_J53" else "FMC_J2") else "PMOD_J52")

  // Order matters; ddr depends on sys_clock
  val uart      = Overlay(UARTOverlayKey, new UARTZCU104ShellPlacer(this, UARTShellInput()))
  val sdio      = if (pmod_is_sdio) Some(Overlay(SPIOverlayKey, new SDIOZCU104ShellPlacer(this, SPIShellInput()))) else None

  //NOTE: ZCU104 commented because we don't need it
  val jtag      = Overlay(JTAGDebugOverlayKey, new JTAGDebugZCU104ShellPlacer(this, JTAGDebugShellInput()))
  // val cjtag     = Overlay(cJTAGDebugOverlayKey, new cJTAGDebugZCU104ShellPlacer(this, cJTAGDebugShellInput()))
  val jtagBScan = Overlay(JTAGDebugBScanOverlayKey, new JTAGDebugBScanZCU104ShellPlacer(this, JTAGDebugBScanShellInput()))
  // val fmc       = Overlay(PCIeOverlayKey, new PCIeZCU104FMCShellPlacer(this, PCIeShellInput()))
  // val edge      = Overlay(PCIeOverlayKey, new PCIeZCU104EdgeShellPlacer(this, PCIeShellInput()))

  val topDesign = LazyModule(p(DesignKey)(designParameters))

  // Place the sys_clock at the Shell if the user didn't ask for it
  designParameters(ClockInputOverlayKey).foreach { unused =>
    val source = unused.place(ClockInputDesignInput()).overlayOutput.node
    val sink = ClockSinkNode(Seq(ClockSinkParameters()))
    sink := source
  }

  override lazy val module = new LazyRawModuleImp(this) {
    val reset = IO(Input(Bool()))
    xdc.addPackagePin(reset, "M11")
    xdc.addIOStandard(reset, "LVCMOS33")

    val reset_ibuf = Module(new IBUF)
    reset_ibuf.io.I := reset

    val sysclk: Clock = sys_clock.get() match {
      case Some(x: SysClockZCU104PlacedOverlay) => x.clock
    }

    val powerOnReset: Bool = PowerOnResetFPGAOnly(sysclk)
    sdc.addAsyncPath(Seq(powerOnReset))

    val ereset: Bool = chiplink.get() match {
      case Some(x: ChipLinkZCU104PlacedOverlay) => !x.ereset_n
      case _ => false.B
    }

    pllReset := (reset_ibuf.io.O || powerOnReset || ereset)
  }
}
