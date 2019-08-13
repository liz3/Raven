package com.savagellc.raven.gui

import javafx.scene.Cursor
import javafx.scene.Node

fun cursorOnHover(node: Node, target: Cursor = Cursor.HAND, default: Cursor = Cursor.DEFAULT) {
    node.setOnMouseEntered {
        node.scene.cursor = target
    }
    node.setOnMouseExited {
        node.scene.cursor = default
    }
}
