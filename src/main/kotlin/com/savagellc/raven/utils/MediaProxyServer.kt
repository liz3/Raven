package com.savagellc.raven.utils

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.IOException


class MediaProxyServer() {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val port
        get() = server.address.port


    init {
        server.createContext("/", DefaultHandler())
        server.createContext("/youtube", YouTubeIFrameHandler())
        server.start()
        println("MediaProxyServer is running on port $port.")
    }

    open class HTTPHandler : HttpHandler {
        override fun handle(exchange: HttpExchange) {
            respond(exchange, 501, "Not implemented.", "text/plain; charset=utf-8")
        }

        protected fun respond(httpRequest: HttpExchange, statusCode: Int, response: String, contentType: String) {
            val responseBytes = response.toByteArray()
            val headers = httpRequest.responseHeaders
            headers.add("Content-Type", contentType)
            headers.add("Server", "Raven-Media-Proxy")
            headers.add("X-Powered-By", "cats")
            httpRequest.sendResponseHeaders(statusCode, responseBytes.size.toLong())
            val out = httpRequest.responseBody
            out.write(responseBytes)
            out.close()
        }
    }

    class DefaultHandler : HTTPHandler() {
        override fun handle(exchange: HttpExchange) {
            respond(
                exchange, 200,
                "Hello wanderer,\n" +
                        "You have found the Raven Media Proxy. This http server is meant to deliver videos inside Raven.",
                "text/plain; charset=utf-8"
            )
        }
    }

    class YouTubeIFrameHandler : HTTPHandler() {

        override fun handle(exchange: HttpExchange) {
            val urlParts = exchange.requestURI.toString().split("/")
            val youtubeVideoID = urlParts.last()
            if (youtubeVideoID.isNotEmpty())
                respond(exchange, 200, generateHTML(youtubeVideoID), "text/html; charset=utf-8")
            else
                respond(exchange, 400, "Bad Request.", "text/text; charset=utf-8")
        }

        private fun generateHTML(youtubeVideoID: String): String {
            var html = "";
            html += "<!DOCTYPE html>\n"
            html += "<html>\n"
            html += "<head>\n"
            html += "   <title>Raven Media Proxy</title>\n"
            html += "   <style>\n"
            html += "       #ytplayer {\n"
            html += "           position: absolute;\n"
            html += "           width: 100vw;\n"
            html += "           height: 100vh;\n"
            html += "           top: 0;\n"
            html += "           left: 0;\n"
            html += "       }\n"
            html += "   </style>\n"
            html += "</head>\n"
            html += "<body>\n"
            html += "   <iframe id=\"ytplayer\" " +
                    "      src=\"https://www.youtube.com/embed/$youtubeVideoID?autoplay=1&amp;auto_play=1\" frameborder=\"0\">" +
                    "   </iframe>"
            html += "</body>\n"
            html += "</html>\n"
            return html
        }
    }
}