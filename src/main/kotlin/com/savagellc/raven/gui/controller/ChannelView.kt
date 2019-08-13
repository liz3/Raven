package com.savagellc.raven.gui.controller

import javafx.fxml.FXML
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.layout.HBox

class ChannelViewController {
    @FXML
    lateinit var sendMessageTextField: TextField

    @FXML
    lateinit var messagesList: ListView<HBox>
}
