package com.savagellc.raven.gui

import com.savagellc.raven.Data
import com.savagellc.raven.core.CoreManager
import com.savagellc.raven.discord.Api
import com.savagellc.raven.discord.ChannelType
import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.discord.OnlineStatus
import com.savagellc.raven.gui.controller.ChannelViewController
import com.savagellc.raven.gui.controller.MainViewController
import com.savagellc.raven.gui.dialogs.LoginDialog
import com.savagellc.raven.include.*
import com.savagellc.raven.utils.readFile
import com.savagellc.raven.utils.writeFile
import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCodeCombination
import javafx.scene.input.KeyCombination
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

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
        controller.messagesList.isFocusTraversable = false
        controller.sendMessageTextField.setOnKeyPressed {
            if (it.code == KeyCode.ENTER) {
                coreManager.sendMessage(controller.sendMessageTextField.text, this) {
                    Platform.runLater {
                        controller.sendMessageTextField.text = ""
                    }
                }
            }
        }
    }

    private fun registerScrollListener() {
        (controller.messagesList.childrenUnmodifiable[0] as VirtualFlow<*>).positionProperty()
            .addListener() { _, _, _ ->
                val lastPos = this.lastPos
                this.lastPos = getScroll()
                if (loading) return@addListener
                loading = true
                if (lastPos > .1 && getScroll() < .1) {
                    coreManager.loadOlderMessages(channel, this)
                } else {
                    Platform.runLater {
                        loading = false
                    }
                }

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
        if (messages.isEmpty()) return
        Platform.runLater {
            messages.reversed().last().renderSeparator = true
            controller.messagesList.items.addAll(0, messages.reversed().map { it.getRendered(controller.messagesList) })
            Platform.runLater {
                controller.messagesList.scrollTo(messages.size)
            }

            loading = false

        }
    }
}

class Manager(val stage: Stage) {
    val dir = File(System.getProperty("user.home"), ".raven")
    val file = File(dir, "tkn-f")
    lateinit var coreManager: CoreManager
    lateinit var controller: MainViewController
    var serverDisplayMode = false
    val treeView = TreeView<Any>()
    var moving = false
    val openChats = HashMap<String, OpenTab>()
    lateinit var infoManager: InfoViewManager
    lateinit var selectedChat: OpenTab

    private fun launchGui() {
        val cb = {
            initialLoad()

        }
        coreManager = CoreManager(this, cb)
        val loader = FXMLLoader()
        val parent = loader.load<BorderPane>(javaClass.getResourceAsStream("/fxml/MainView.fxml"))
        controller = loader.getController() as MainViewController
        val scene = Scene(parent)
        stage.minWidth = 400.0
        stage.minHeight = 300.0
        scene.stylesheets.add("/css/DarkStyle.css")
        stage.title = "Raven"
        stage.scene = scene
        stage.sizeToScene()
        stage.centerOnScreen()
        setupGuiEvents()
        stage.show()
        infoManager = InfoViewManager(this)
    }

    fun addDmChat(chat: PrivateChat) {
        Platform.runLater {
            controller.dmChannelsList.items.add(chat.guiObj)
        }
    }

    private fun loadChat(channel: Channel) {
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
            tab.content = rPane
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

    private fun quit() {
        coreManager.api.webSocket.websocket.sendClose()
        Platform.exit()
    }

    private fun logout() {
        Thread {
            openChats.clear()
            coreManager.api.webSocket.websocket.sendClose()
            coreManager.api.webSocket.disposed = true
            coreManager.api.webSocket.clearEventListeners()
            coreManager.api.webSocket.heartbeatThread.interrupt()
            coreManager.api.sendLogout()
            ImageCache.disposeCache()
            Data.token = ""
            this.file.delete()
            Platform.runLater {
                stage.close()
                Manager(Stage()).start()
            }
        }.start()
    }

    private fun setupGuiEvents() {
        stage.setOnCloseRequest {
            quit()
        }
        stage.focusedProperty().addListener { observable, oldValue, newValue ->
            Data.focused = newValue
        }
        setupMainMenu()
        controller.statusComboBox.items.addAll(OnlineStatus.values())
        controller.statusComboBox.selectionModel.select(OnlineStatus.ONLINE)
        controller.statusComboBox.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
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
            val find = openChats.values.find { it.guiTab == newValue } ?: return@addListener
            selectedChat = find
            infoManager.loadChannel(find)
            if (find.channel.messages.size > 0) {
                val lastId = find.channel.messages.lastElement().id
                if (find.channel.lastAck != find.channel.messages.lastElement().id)
                    Thread {
                        coreManager.api.sendMessageAckByChannelSwitch(find.channel.id, lastId)
                    }.start()
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
            Platform.runLater {
                coreManager.chats.forEach {
                    controller.dmChannelsList.items.add(it.guiObj)
                }
                controller.serversList.items.addAll(coreManager.servers)
            }
        }.start()
    }

    private fun setupMainMenu() {
        val menu = controller.mainMenuBar
        val file = Menu("File")
        file.items.addAll(createMenuOption("Logout") {
            logout()
        }, createMenuOption("Toogle side pane") {
            infoManager.toggle()
        }, createMenuOption("Attach Debugger") {
            val d = EventLogger()
            coreManager.api.attachDebugger(d)
        }, createMenuOption("Quit", KeyCodeCombination(KeyCode.Q, KeyCombination.CONTROL_DOWN)) {
            quit()
        })
        val options = Menu("Settings")

        options.items.add(createMenuOption("Toggle message delete prevent") {
            Data.options.preventMessageDelete = !Data.options.preventMessageDelete
        })
        options.items.add(createMenuOption("Toggle channel delete prevent") {
            Data.options.preventChannelDelete = !Data.options.preventChannelDelete
        })
        options.items.add(createMenuOption("Toggle message update prevent") {
            Data.options.preventMessageUpdate = !Data.options.preventMessageUpdate
        })

        menu.menus.addAll(file, options)
        menu.isUseSystemMenuBar = true
    }

    private fun createMenuOption(name: String, keyCombination: KeyCombination? = null, func: () -> Unit): MenuItem {
        val item = MenuItem(name)
        item.setOnAction {
            func()
        }
        if (keyCombination != null)
            item.accelerator = keyCombination
        return item
    }

    fun start() {
        if (!dir.exists()) dir.mkdir()
        if (file.exists()) {
            Data.token = readFile(file).second
            launchGui()
        } else {
            val credentials = LoginDialog().showAndWait().get()
            if (credentials.first.isEmpty() || credentials.second.isEmpty()) exitProcess(0)
            val localApi = Api("", true)
            val lgResp = localApi.login(credentials.first, credentials.second)
            if (!lgResp.hasData) {
                start()
                return
            }
            val data = JSONObject(lgResp.data)
            if (data.has("token")) {
                if (!data.isNull("token")) {
                    val token = data.getString("token")
                    writeFile(token.toByteArray(), file, false, true)
                    Data.token = token
                    launchGui()
                } else {
                    if (data.has("mfa") && data.getBoolean("mfa")) {
                        val mFaToken = data.getString("ticket")
                        val totp = Prompts.textPrompt("2FA", "Enter 2FA code")
                        val mFaResp = localApi.sendTotp(totp, mFaToken)
                        if (mFaResp.hasData) {
                            val token = JSONObject(mFaResp.data).getString("token")
                            writeFile(token.toByteArray(), file, false, true)
                            Data.token = token
                            launchGui()
                        } else {
                            start()
                        }
                    } else {
                        start()
                    }
                }
            }
        }
    }
}

class JavaFxBootstrapper : Application() {
    override fun start(primaryStage: Stage) {
        Manager(primaryStage).start()
    }

    override fun stop() {
        exitProcess(0)
    }

    companion object {
        fun bootstrap() = launch(JavaFxBootstrapper::class.java)
    }
}
