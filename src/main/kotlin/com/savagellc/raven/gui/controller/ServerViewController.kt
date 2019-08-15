package com.savagellc.raven.gui.controller

import com.savagellc.raven.include.GuildMember
import javafx.scene.layout.VBox
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.image.ImageView


class ServerViewController {

    @FXML
    lateinit var closeBtn: Button

    @FXML
    lateinit var profile: ImageView

    @FXML
    lateinit var userNameLabel: Label

    @FXML
    lateinit var infoContainer: VBox

    @FXML
    lateinit var serverUsersList: ListView<GuildMember>
}