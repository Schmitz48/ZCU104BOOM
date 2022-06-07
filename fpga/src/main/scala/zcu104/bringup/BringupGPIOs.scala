package chipyard.fpga.zcu104.bringup

import scala.collection.mutable.{LinkedHashMap}

object BringupGPIOs {
    // map of the pin name (akin to die pin name) to (fpga package pin, IOSTANDARD, add pullup resistor?)
    // Pin map can be found in "Zynq Ultrascale+ MPSoC ZCU104 Board User Guide"
    // LEDS:        Page 67-68
    val pinMapping = LinkedHashMap(
        // these connect to LEDs and switches on the ZCU104 (and use 3.3V)
        "led0"  -> ("D5", "LVCMOS33", false), // 0
        "led1"  -> ("D6", "LVCMOS33", false), // 1
        "led2"  -> ("A5", "LVCMOS33", false), // 2
        "led3"  -> ("B5", "LVCMOS33", false), // 3
        "sw0"   -> ("E4", "LVCMOS33", false), // 0
        "sw1"   -> ("D4", "LVCMOS33", false), // 1
        "sw2"   -> ("F5", "LVCMOS33", false), // 2
        "sw3"   -> ("F4", "LVCMOS33", false)  // 3
    )

    // return list of names (ordered)
    def names: Seq[String] = pinMapping.keys.toSeq

    // return number of GPIOs
    def width: Int = pinMapping.size
}
