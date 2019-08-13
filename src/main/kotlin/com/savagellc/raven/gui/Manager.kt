package com.savagellc.raven.gui

import com.savagellc.raven.Data
import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.discord.Api
import com.savagellc.raven.discord.ChannelType
import com.savagellc.raven.discord.OnlineStatus
import com.savagellc.raven.gui.controller.ChannelViewController
import com.savagellc.raven.gui.controller.MainViewController
import com.savagellc.raven.include.*
import com.savagellc.raven.utils.readFile
import com.savagellc.raven.utils.writeFile
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Tab
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import java.io.File

class OpenTab(
    val coreManager: CoreManager,
    val channel: Channel,
    val controller: ChannelViewController,
    val guiTab: Tab
) {

    var lastPos = 0.0
    var loading = false

    init {
        channel.guiReference = this
        Platform.runLater {
            setupGuiEvents()
            loadChat()
        }
    }

    private fun setupGuiEvents() {
        controller.sendMessageTextField.setOnKeyPressed {
            if (it.code == KeyCode.ENTER) {
                coreManager.sendMessage(controller.sendMessageTextField.text, this) {
                    Platform.runLater {
                        controller.sendMessageTextField.text = "";
                    }
                }
            }
        }
    }

    private fun registerScrollListener() {
        (controller.messagesList.childrenUnmodifiable[0] as VirtualFlow<*>).positionProperty()
            .addListener() { _, _, newVal ->
                if (loading) return@addListener
                loading = true
                if (lastPos > 0 && newVal.toDouble() == 0.0) {
                    lastPos = 0.0
                    println("loading older messages")
                    coreManager.loadOlderMessages(channel, this)
                } else {
                    Platform.runLater {
                        loading = false
                    }
                }
                lastPos = newVal.toDouble()
            }
    }

    private fun getScroll() = (controller.messagesList.childrenUnmodifiable[0] as VirtualFlow<*>).position
    private fun loadChat() {
        if (loading) return
        loading = true
        coreManager.initChat(channel) { succ ->
            if (!succ) {
                loading = false
                return@initChat
            }
            Platform.runLater {
                controller.messagesList.items.clear()
                controller.messagesList.items.addAll(channel.messages.map { it.getRendered(controller.messagesList) })
                controller.messagesList.scrollTo(channel.messages.size - 1)
                Platform.runLater {
                    registerScrollListener()
                    lastPos = getScroll()
                    loading = false
                }
            }
        }
    }

    fun appendMessage(msg: GuiMessage) {
        Platform.runLater {
            val rendered = msg.getRendered(controller.messagesList)
            controller.messagesList.items.add(rendered)

        }
    }

    fun prepend(messages: List<GuiMessage>) {
        val first = controller.messagesList.items.first()
        Platform.runLater {
            controller.messagesList.items.addAll(0, messages.reversed().map { it.getRendered(controller.messagesList) })
            Platform.runLater {
                controller.messagesList.scrollTo(first)
                loading = false
            }
        }
    }
}

class Manager(val stage: Stage) {
    lateinit var coreManager: CoreManager
    lateinit var controller: MainViewController
    var serverDisplayMode = false
    val treeView = TreeView<Any>()
    var moving = false
    val openChats = HashMap<String, OpenTab>()

    private fun launchGui() {
        coreManager = CoreManager(this)
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

    fun addDmChat(chat: PrivateChat) {
        Platform.runLater {
            controller.dmChannelsList.items.add(chat.guiObj)
        }
    }

    fun loadChat(channel: Channel) {
        if (openChats.containsKey(channel.id)) {
            Platform.runLater {
                controller.openChatsTabView.selectionModel.select(openChats[channel.id]!!.guiTab)
            }
            return
        }
        val loader = FXMLLoader()
        val pane = loader.load<BorderPane>(javaClass.getResourceAsStream("/fxml/ChatView.fxml"))
        val controller = loader.getController<ChannelViewController>()
        Platform.runLater {
            val tab =
                Tab(if (channel is PrivateChat) channel.guiObj.toString() else (channel as ServerChannel).guiObj.toString())
            val obj = OpenTab(coreManager, channel, controller, tab)
            tab.setOnClosed {
                openChats.remove(channel.id)
            }
            openChats[channel.id] = obj
            val rPane = AnchorPane()
            rPane.children.add(pane)
            AnchorPane.setTopAnchor(pane, 0.0)
            AnchorPane.setLeftAnchor(pane, 0.0)
            AnchorPane.setRightAnchor(pane, 0.0)
            AnchorPane.setBottomAnchor(pane, 0.0)
            tab.content = rPane;
            this.controller.openChatsTabView.tabs.add(tab)
            this.controller.openChatsTabView.selectionModel.select(tab)
        }
    }

    fun renderServerChannels(server: Server): TreeItem<Any> {
        val root = TreeItem<Any>("<- Back to server list")
        root.isExpanded = true
        server.channels.filter { th -> th.type == ChannelType.GUILD_TEXT.num && (th.obj.isNull("parent_id")) }
            .sortedBy { x -> x.obj.getInt("position") }.forEach { cCh ->
                root.children.add(TreeItem(cCh.guiObj))
            }
        server.channels.filter { th -> th.type == ChannelType.GUILD_CATEGORY.num }
            .sortedBy { x -> x.obj.getInt("position") }.forEach { ch ->
                val item = TreeItem<Any>(ch.guiObj)
                item.isExpanded = true
                server.channels.filter { cCh ->
                    cCh.obj.has("parent_id") && cCh.obj.get("parent_id") is String && cCh.obj.get(
                        "parent_id"
                    ) == ch.id
                }.filter { cCh -> cCh.type == ChannelType.GUILD_TEXT.num }.forEach { cCh ->
                    item.children.add(TreeItem(cCh.guiObj))
                }
                root.children.add(item)
            }

        return root
    }

    fun setupGuiEvents() {
        controller.statusComboBox.items.addAll(OnlineStatus.values())
        controller.statusComboBox.selectionModel.select(OnlineStatus.ONLINE)
        controller.statusComboBox.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            println(newValue)
            Thread {
                coreManager.api.updateOnlineStatus(newValue)
            }.start()
        }
        treeView.setOnMouseClicked {
            if (treeView.selectionModel.selectedItem != null) {
                val item = treeView.selectionModel.selectedItem
                if (item == treeView.root) {
                    controller.serverTab.content = controller.serversList
                    return@setOnMouseClicked

                }
                if (item.value is GuiServerChannel) {
                    val channel = (item.value as GuiServerChannel).privateChat
                    if (channel.type == 0) {
                        loadChat(channel)
                    }
                }
            }
        }
        controller.openChatsTabView.selectionModel.selectedItemProperty().addListener { _, old, newValue ->
            if(newValue != null && old != null) {
                val find = openChats.values.find { it.guiTab == newValue }
                if(find != null && find.channel.messages.size > 0) {
                        val lastId = find.channel.messages.lastElement().id
                        if(find.channel.lastAck != find.channel.messages.lastElement().id)
                            Thread {
                                coreManager.api.sendMessageAckByChannelSwitch(find.channel.id, lastId)
                            }.start()
                    }
            }
        }
        controller.serversList.setOnMouseClicked {
            if (it.clickCount == 2) {
                if (controller.serversList.selectionModel.selectedItem != null) {
                    val server = controller.serversList.selectionModel.selectedItem
                    coreManager.loadServerChannels(server) {
                        Platform.runLater {
                            treeView.root = renderServerChannels(server)
                            coreManager.activeServer = server
                            controller.serverTab.content = treeView
                            serverDisplayMode = true
                        }
                    }
                }
            } else {
                if (controller.serversList.selectionModel.selectedItem != null) {
                    val server = controller.serversList.selectionModel.selectedItem
                    ServerMenu.openMenu(
                        server,
                        it.screenX,
                        it.screenY,
                        coreManager,
                        controller.serversList,
                        it.button == MouseButton.SECONDARY
                    )
                }
            }
        }
        controller.dmChannelsList.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
            if (moving) return@addListener
            loadChat(newValue.privateChat)
        }
        controller.joinBtn.setOnAction {
            val text = Prompts.textPrompt("Enter Link", "Enter Discord Invite link")
            val id = if (text.contains("discord.gg")) text.split("discord.gg/")[1] else text
            Thread {
                coreManager.api.acceptInvite(id)
            }.start()
        }
    }

    private fun initialLoad() {
        Thread {
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
        val dir = File(System.getProperty("user.home"), ".raven")
        if (!dir.exists()) dir.mkdir()
        val file = File(dir, "tkn-f")
        if (file.exists()) {
            Data.token = readFile(file).second
            launchGui()
        } else {
            val userName = Prompts.textPrompt("Email", "Enter Email Address")
            val password = Prompts.passPrompt()
            val data = Api("", true).login(userName, password)
            if (data.has("token")) {
                val token = data.getString("token")
                writeFile(token.toByteArray(), file, false, true)
                Data.token = token
                launchGui()
            }
        }
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