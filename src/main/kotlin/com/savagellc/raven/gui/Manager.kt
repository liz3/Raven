package com.savagellc.raven.gui

import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.gui.controller.MainViewController
import com.savagellc.raven.include.*
import com.sun.javafx.scene.control.skin.VirtualFlow
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.stage.Stage


class Manager(val stage: Stage) {
    val coreManager = CoreManager(this)
    lateinit var controller:MainViewController
    var lastPos = 0.0
    var serverDisplayMode = false
    val treeView = TreeView<Any>()
    var loading = false

    private fun launchGui() {
        val loader = FXMLLoader()
        val parent = loader.load<BorderPane>(javaClass.getResourceAsStream("/fxml/MainView.fxml"))
        controller = loader.getController() as MainViewController
        val scene = Scene(parent)
        scene.stylesheets.add("/css/DarkStyle.css")
        stage.title = "Raven"
        stage.scene = scene
        stage.sizeToScene()
        stage.centerOnScreen()
        setupGuiEvents()
        stage.show()
        initialLoad()
    }
    fun appendMessage(msg:GuiMessage) {
        Platform.runLater {
            controller.messagesList.items.add(msg.getRendered(controller.messagesList))
        }
    }
    fun getScroll() =  (controller.messagesList.childrenUnmodifiable[0] as VirtualFlow<*>).position
    fun loadChat(privateChat: Channel) {
        if(loading) return
        loading = true
        coreManager.initChat(privateChat) {
            Platform.runLater {
                controller.messagesList.items.clear()
                controller.messagesList.items.addAll(privateChat.messages.map { it.getRendered(controller.messagesList) })
                controller.messagesList.scrollTo(privateChat.messages.size - 1)
                Platform.runLater {
                    lastPos = getScroll()
                    loading = false
                }
            }
        }
    }
    fun prepend(messages: List<GuiMessage>) {
        val first = controller.messagesList.items.first()
        Platform.runLater {
                controller.messagesList.items.addAll(0, messages.reversed().map { it.getRendered(controller.messagesList) } )
            Platform.runLater {
                lastPos = getScroll()
                controller.messagesList.scrollTo(first)
            }
        }
    }
    fun setupGuiEvents() {
        controller.messagesList.setOnScrollFinished {
           if(lastPos > 0 && getScroll() == 0.0) {
               println("Loading older messages")
               coreManager.loadOlderMessages()
           }
            lastPos = getScroll()
        }
        controller.dmChannelsList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            loadChat(newValue.privateChat)
        }
        controller.sendMessageTextField.setOnKeyPressed {
            if(it.code == KeyCode.ENTER) {
                coreManager.sendMessage(controller.sendMessageTextField.text) {
                    Platform.runLater {
                        controller.sendMessageTextField.text = "";
                    }
                }
            }
        }
        treeView.setOnMouseClicked {
            if(treeView.selectionModel.selectedItem != null) {
                val item = treeView.selectionModel.selectedItem
                if(item == treeView.root) {
                    controller.serverTab.content = controller.serversList
                    return@setOnMouseClicked

                }
                if(item.value is GuiServerChannel) {
                   val channel = (item.value as GuiServerChannel).privateChat
                    if(channel.type == 0) {
                        loadChat(channel)
                    }
                }
            }
        }
        controller.serversList.setOnMouseClicked {
            if(it.clickCount == 2) {
               if(controller.serversList.selectionModel.selectedItem != null) {
                   val server = controller.serversList.selectionModel.selectedItem
                   coreManager.loadServerChannels(server) {
                      Platform.runLater {
                          val root = TreeItem<Any>("Root")
                          server.channels.filter {th -> th.type == 4 }.sortedBy { x -> x.obj.getInt("position") }.forEach {ch->
                              val item = TreeItem<Any>(ch.guiObj)
                              server.channels.filter { cCh -> cCh.obj.has("parent_id") && cCh.obj.get("parent_id") is String && cCh.obj.get("parent_id") == ch.id }.forEach {cCh ->
                                  item.children.add(TreeItem(cCh.guiObj))
                              }
                              root.children.add(item)
                          }
                          treeView.root = root

                          controller.serverTab.content = treeView
                          serverDisplayMode = true
                      }
                   }
               }
            }
        }
    }

    fun initialLoad() {
        Thread{
            coreManager.initLoad()
            Platform.runLater {
                coreManager.chats.forEach {
                    controller.dmChannelsList.items.add(it.guiObj)
                }
                controller.serversList.items.addAll(coreManager.servers)
            }
        }.start()
    }
    fun start() {
        launchGui()
    }
}

class JavaFxBootstrapper : Application() {
    override fun start(primaryStage: Stage) {
        Manager(primaryStage).start()
    }
    companion object {
        fun bootstrap() = launch(JavaFxBootstrapper::class.java)
    }
}