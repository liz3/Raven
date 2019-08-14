package com.savagellc.raven.gui.dialogs

import javafx.application.Platform
import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.VBox


class LoginDialog : Dialog<Pair<String, String>?>() {

    private val emailField = TextField()
    private val passwordField = PasswordField()

    init {

        title = "Login into Discord"
        headerText = "Enter your account credentials below."

        dialogPane.stylesheets.add("/css/DarkStyle.css")

        dialogPane.buttonTypes.addAll(ButtonType("Login", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL)

        dialogPane.minWidth = 400.0
        dialogPane.minHeight = 200.0

        isResizable = true

        emailField.promptText = "Email"
        passwordField.promptText = "Password"

        val vBox = VBox()
        vBox.children.addAll(emailField, passwordField)
        vBox.padding = Insets(20.0)

        vBox.spacing = 5.0

        dialogPane.content = vBox

        Platform.runLater {
            emailField.requestFocus()
        }

        setResultConverter {
            if (it.buttonData == ButtonBar.ButtonData.OK_DONE) {
                return@setResultConverter Pair(emailField.text, passwordField.text)
            }
            return@setResultConverter Pair("", "")
        }

    }


}