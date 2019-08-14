package com.savagellc.raven

import com.savagellc.raven.gui.JavaFxBootstrapper
import com.savagellc.raven.include.Options

object Data {
    var token = ""
    val options = Options()
    var focused = true
}

fun main(args: Array<String>) {
    JavaFxBootstrapper.bootstrap()
}
