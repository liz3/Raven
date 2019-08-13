package com.savagellc.raven.gui

import com.savagellc.raven.discord.OpCode
import com.savagellc.raven.discord.Response
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.stage.Stage
import java.util.*

class EventLogger {
    val area = TextArea()
    var paused = false
    var dispsed = false
    val cached = Vector<String>()
    var messages = 0

    init {
        area.isEditable = false
        val stage = Stage()
        stage.title = "Raven Event Logger"
        val pane = BorderPane()
        pane.center = area
        pane.bottom = getLowerBox()
        val scene = Scene(pane, 800.0, 600.0)
        scene.stylesheets.add("/css/DarkStyle.css")
        stage.scene = scene
        stage.setOnCloseRequest {
            dispsed = true
            cached.clear()
        }

        stage.show()
    }

    private fun push(message: String) {
        if (dispsed) return
        if (paused) {
            cached.add(message)
        } else {
            Platform.runLater {
                messages++;
                area.appendText(message)
                if (messages > 4300) {
                    var count = 0;
                    val length = area.text.takeWhile {
                        if (it == '\n') count++
                        count < 100
                    }.length
                    area.text = area.text.substring(length)
                    messages -= 100
                }
            }
        }
    }

    fun pushApiUpdate(path: String, method: String, response: Response, requestBody: String?) {
        val message =
            "HTTP: [$method]$path${if (requestBody != null) "\n REQ> $requestBody" else ""}\n${response.code} RESP>${response.data}\n\n"
        push(message)
    }

    fun pushSockUp(op: OpCode, data: Any?) {
        val message = "SOCK PUSH: [$op:${op.num}] $data\n"
        push(message)
    }

    fun pushSockDown(op: Int, data: Any?) {
        val message = "SOCK RECEIVE: [$op] $data\n"
        push(message)
    }

    private fun getLowerBox(): HBox {
        val box = HBox()
        val pauseBtn = Button("Pause")
        pauseBtn.setOnAction {
            paused = !paused
            if (!paused) {
                Platform.runLater {
                    messages += cached.size
                    val joined = cached.joinToString("")
                    area.appendText("RESUME START >\n$joined\nRESUME END\n")
                    cached.clear()

                }
            }
        }
        box.children.add(pauseBtn)
        return box
    }
}