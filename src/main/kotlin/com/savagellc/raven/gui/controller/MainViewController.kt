package com.savagellc.raven.gui.controller

import com.savagellc.raven.include.GuiDmChannel
import com.savagellc.raven.include.Server
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Tab
import javafx.scene.control.TextField
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox


class MainViewController {

    @FXML
    lateinit var nameLabel: Label

    @FXML
    lateinit var viewerBorderPane: BorderPane

    @FXML
    lateinit var dmChannelsList: ListView<GuiDmChannel>

    @FXML
    lateinit var serversList: ListView<Server>

    @FXML
    lateinit var serverTab:Tab

    @FXML
    lateinit var sendMessageTextField: TextField

    @FXML
    lateinit var messagesList: ListView<HBox>
}