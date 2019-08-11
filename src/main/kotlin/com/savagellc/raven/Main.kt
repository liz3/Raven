package com.savagellc.raven

import com.savagellc.raven.gui.JavaFxBootstrapper

object Data {
    var token = ""
}

fun main(args:Array<String>) {
    Data.token = args[0]
    JavaFxBootstrapper.bootstrap()
}