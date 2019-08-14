package com.savagellc.raven.gui.listitem.content

import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.gui.browse
import com.savagellc.raven.gui.cursorOnHover
import com.savagellc.raven.gui.listitem.content.shared.Thumbnail
import com.savagellc.raven.gui.renders.getLabel
import com.savagellc.raven.gui.renders.maxImageWidth
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.scene.input.MouseButton
import javafx.scene.web.WebView
import org.json.JSONObject

class EmbeddedContentItem(val embed: JSONObject, val mediaProxyServerPort: Int) : MessageContentItem() {

    private var thumbnail: Thumbnail? = null
    private var renderer: WebView? = null

    init {
        var previewIndex = -1
        cursorOnHover(this)
        if (embed.has("url") && !embed.isNull("url")) {
            if (embed.has("title"))
                children.add(getLabel(embed.getString("title"), "-fx-font-size: 15;", true) {
                    browse(embed.getString("url"))
                })

        } else {
            if (embed.has("title"))
                children.add(getLabel(embed.getString("title"), "-fx-font-size: 15;", true))
        }
        if (embed.has("description"))
            children.add(getLabel(embed.getString("description")))

        if (embed.has("thumbnail")) {
            val task = object : Task<Void>() {
                override fun call(): Void? {
                    val url = embed.getJSONObject("thumbnail").getString("url")
                    val lW = embed.getJSONObject("thumbnail").getInt("width").toDouble()
                    thumbnail = Thumbnail(SwingFXUtils.toFXImage(ImageCache.getImage(url), null), width, lW)
                    if (embed.has("video") && !embed.isNull("video")) {
                        var switched = false
                        thumbnail!!.setOnMouseClicked { ev ->
                            if (renderer != null && ev.pickResult.intersectedNode == renderer) return@setOnMouseClicked
                            if (ev.button == MouseButton.PRIMARY) {
                                if (switched) {
                                    children[previewIndex] = thumbnail
                                    renderer?.engine?.load(null)
                                    switched = false
                                    return@setOnMouseClicked
                                } else {
                                    if (renderer != null) {
                                        renderer!!.engine.reload()
                                    } else {

                                        val youtubeVideoID =
                                            embed.getJSONObject("video").getString("url").split("/").last()

                                        renderer = WebView()
                                        renderer!!.prefWidth = thumbnail!!.fitWidth
                                        renderer!!.prefHeight = 430.0
                                        renderer!!.engine.load("http://localhost:$mediaProxyServerPort/youtube/$youtubeVideoID")
                                    }
                                    children[previewIndex] = renderer!!
                                    switched = true
                                }
                            }
                        }
                    } else {
                        if (embed.has("url") && !embed.isNull("url")) {
                            cursorOnHover(thumbnail!!)
                            thumbnail!!.setOnMouseClicked { ev ->
                                browse(embed.getString("url"))
                            }

                        }
                    }
                    Platform.runLater {
                        children.add(thumbnail!!)
                        requestRefresh()
                        previewIndex = children.indexOf(thumbnail!!)
                    }
                    return null
                }
            }
            val thread = Thread(task)
            thread.isDaemon = true
            thread.start()
        }
    }

    override fun onWidthChanged(newWidth: Double) {
        thumbnail?.updateChatWidth(newWidth)
        if (newWidth <= maxImageWidth - 100) {
            renderer?.prefWidth = newWidth - 100
        }
    }

}