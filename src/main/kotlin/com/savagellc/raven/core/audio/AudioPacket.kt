package com.savagellc.raven.core.audio

import java.nio.ByteBuffer

class IncomingAudioPacket(val data: ByteBuffer, val ssrc: Int, val valid: Boolean) {

}