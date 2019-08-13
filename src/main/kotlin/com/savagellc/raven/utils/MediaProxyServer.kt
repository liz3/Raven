package com.savagellc.raven.utils

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.io.IOException


class MediaProxyServer() {

    private val server: HttpServer = HttpServer.create(InetSocketAddress("127.0.0.1", 55555), 0)
    val port
        get() = server.address.port


    init {
        server.createContext("/youtube", YouTubeIFrameHandler())
        server.start()
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

    class YouTubeIFrameHandler : HTTPHandler() {
        override fun handle(exchange: HttpExchange) {
            val urlParts = exchange.requestURI.toString().split("/")
            val youtubeVideoID = urlParts.last()
            if (youtubeVideoID.isNotEmpty())
                // DISCLAIMER: THIS IS SPAGHETTI THAT WAS BEST BEFORE A FEW CENTURIES AGO. PLEASE DON'T JUDGE
                respond(
                    exchange, 200,
                    "<style>" +
                            "body {margin: 0;}" +
                            "</style>" +
                            "<iframe id=\"ytplayer\" src=\"https://www.youtube.com/embed/$youtubeVideoID?autoplay=1&amp;auto_play=1\" width=\"400\" height=\"225\" frameborder=\"0\"></iframe>" +
                            "<script>" +
                            "document.getElementById('ytplayer').width = window.outerWidth;" +
                            "document.getElementById('ytplayer').height = window.outerHeight;" +
                            "</script>",
                    "text/html; charset=utf-8"
                )
            else
                respond(
                    exchange, 400,
                    "Bad Request.",
                    "text/text; charset=utf-8"
                )
        }

    }
}