package com.savagellc.raven.gui

import com.savagellc.raven.PasswordDialog
import javafx.scene.control.TextInputDialog

object Prompts {
    fun textPrompt(title: String, header: String, default: String = ""): String {
        val input = TextInputDialog(default)
        val dialogPane = input.dialogPane
        dialogPane.stylesheets.add("/css/DarkStyle.css")
        input.title = title
        input.headerText = header
        input.graphic = null
        return try {
            input.showAndWait().get()
        } catch (e: Exception) {
            ""
        }
    }
    fun passPrompt(): String {

        val pd = PasswordDialog()
        val dialogPane = pd.dialogPane
        dialogPane.stylesheets.add("/css/DarkStyle.css")
        val result = pd.showAndWait()
        return try {
            result.get()
        } catch (e: Exception) {
            ""
        }
    }

}