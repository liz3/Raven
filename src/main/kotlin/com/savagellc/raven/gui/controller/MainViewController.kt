package com.savagellc.raven.gui.controller

import com.savagellc.raven.discord.OnlineStatus
import com.savagellc.raven.include.GuiDmChannel
import com.savagellc.raven.include.Server
import javafx.fxml.FXML
import javafx.scene.control.*
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
    lateinit var serverTab: Tab

    @FXML
    lateinit var statusComboBox: ComboBox<OnlineStatus>

    @FXML
    lateinit var joinBtn: Button

    @FXML
    lateinit var openChatsTabView: TabPane

    @FXML
    lateinit var mainMenuBar: MenuBar

}
