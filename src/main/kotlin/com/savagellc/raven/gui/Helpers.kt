package com.savagellc.raven.gui

import com.savagellc.raven.Data
import com.savagellc.raven.discord.ImageCache
import com.savagellc.raven.utils.OperatingSystemType
import com.savagellc.raven.utils.getOS
import com.savagellc.raven.utils.writeFile
import javafx.application.Platform
import javafx.concurrent.Task
import javafx.embed.swing.SwingFXUtils
import javafx.scene.Cursor
import javafx.scene.Node
import javafx.scene.image.ImageView
import java.awt.Desktop
import java.io.File
import java.net.URI

fun cursorOnHover(node: Node, target: Cursor = Cursor.HAND, default: Cursor = Cursor.DEFAULT) {
    node.setOnMouseEntered {
        node.scene.cursor = target
    }
    node.setOnMouseExited {
        node.scene.cursor = default
    }
}

fun browse(url: String) {
    Thread {
        Desktop.getDesktop().browse(URI(url))
    }.start()
}
fun sendNotification(title:String, message:String, imagePath:String) {
  Thread {
      when(getOS()) {
          OperatingSystemType.MAC_OS -> {
              val file = File(File(System.getProperty("user.home"), ".raven"), "RavenNativeUtils")
              if(!file.exists()) {
                  writeFile(Data.javaClass.getResourceAsStream("/native/darwin/RavenNativeUtils").readAllBytes(), file, false, true)
                  Runtime.getRuntime().exec("chmod +x ${file.absolutePath}")
              }
              val builder = ProcessBuilder(file.absolutePath)
              val process = builder.start()
              process.outputStream.write("Raven: $title\n$message\n$imagePath\n".toByteArray())
              process.outputStream.flush()
          }
          OperatingSystemType.LINUX -> {
              val file = File(File(System.getProperty("user.home"), ".raven"), "RavenNativeUtils")
              if(!file.exists()) {
                  writeFile(Data.javaClass.getResourceAsStream("/native/linux/RavenNativeUtils").readAllBytes(), file, false, true)
                  Runtime.getRuntime().exec("chmod +x ${file.absolutePath}")
              }
              val builder = ProcessBuilder(file.absolutePath)
              val process = builder.start()
              process.outputStream.write("Raven: $title\n$message\n$imagePath\n".toByteArray())
              process.outputStream.flush()
          }
          OperatingSystemType.WINDOWS -> {
              //Will come later
          }
      }
  }.start()
}
fun appendToImageViewAsync(view:ImageView, path:String, cb: () -> Unit) {
    val task = object : Task<Void>() {
        override fun call(): Void? {
            val image =
                SwingFXUtils.toFXImage(
                    ImageCache.getImage(
                        path
                    ),
                    null
                )
            Platform.runLater {
               view.image = image
            }
            return null
        }
    }
    task.setOnSucceeded {
        cb()
    }
    val loader = Thread(task)
    loader.isDaemon = true
    loader.start()
}
fun appendToImageViewAsync(view:ImageView, path:String) {
    val task = object : Task<Void>() {
        override fun call(): Void? {
            val image =
                SwingFXUtils.toFXImage(
                    ImageCache.getImage(
                        path
                    ),
                    null
                )

            Platform.runLater {
               view.image = image
            }
            return null
        }
    }

    val loader = Thread(task)
    loader.isDaemon = true
    loader.start()
}