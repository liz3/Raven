package com.savagellc.raven.opus;

import com.sun.jna.ptr.PointerByReference;
import net.tomp2p.opuswrapper.Opus;

import javax.sound.sampled.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final int SAMPLE_RATE = 48000;
    private static final int FRAME_SIZE = 480;

    public static void main(String[] args) throws LineUnavailableException {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, true);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("not supported");
        }
        TargetDataLine microphone = AudioSystem.getTargetDataLine(format);
        // Obtain and open the line.
        microphone.open(format);
        // Begin audio capture.
        microphone.start();

        SourceDataLine speaker = AudioSystem.getSourceDataLine(format);
        speaker.open(format);
        speaker.start();

        IntBuffer error = IntBuffer.allocate(4);
        PointerByReference opusDecoder = Opus.INSTANCE.opus_decoder_create(SAMPLE_RATE, 1, error);
        PointerByReference opusEncoder = Opus.INSTANCE.opus_encoder_create(SAMPLE_RATE, 1, Opus.OPUS_APPLICATION_RESTRICTED_LOWDELAY, error);

        while (true) {
            ShortBuffer dataFromMic = recordFromMicrophone(microphone);
            long start = System.nanoTime();
            List<ByteBuffer> packets = encode(opusEncoder, dataFromMic);
            // packets go over network
            ShortBuffer decodedFromNetwork = decode(opusDecoder, packets);
            long stop = System.nanoTime();
            System.out.println((stop - start) / 1000000D + "ms");
            playBack(speaker, decodedFromNetwork);
        }

//        Opus.INSTANCE.opus_decoder_destroy(opusDecoder);
//        Opus.INSTANCE.opus_encoder_destroy(opusEncoder);
    }

    private static ShortBuffer decode(PointerByReference opusDecoder, List<ByteBuffer> packets) {
        ShortBuffer shortBuffer = ShortBuffer.allocate(packets.size() * FRAME_SIZE);
        for (ByteBuffer dataBuffer : packets) {
            byte[] transferedBytes = new byte[dataBuffer.remaining()];
            dataBuffer.get(transferedBytes);
            int decoded = Opus.INSTANCE.opus_decode(opusDecoder, transferedBytes, transferedBytes.length, shortBuffer, FRAME_SIZE, 0);
            shortBuffer.position(shortBuffer.position() + decoded);
        }
        shortBuffer.flip();
        return shortBuffer;
    }

    private static List<ByteBuffer> encode(PointerByReference opusEncoder, ShortBuffer shortBuffer) {
        int read = 0;
        List<ByteBuffer> list = new ArrayList<>();
        while (shortBuffer.hasRemaining()) {
            ByteBuffer dataBuffer = ByteBuffer.allocate(8192);
            int toRead = Math.min(shortBuffer.remaining(), dataBuffer.remaining());
            read = Opus.INSTANCE.opus_encode(opusEncoder, shortBuffer, FRAME_SIZE, dataBuffer, toRead);
            dataBuffer.position(dataBuffer.position() + read);
            dataBuffer.flip();
            list.add(dataBuffer);
            shortBuffer.position(shortBuffer.position() + FRAME_SIZE);
        }

        // used for debugging
        shortBuffer.flip();
        return list;
    }

    private static void playBack(SourceDataLine speaker, ShortBuffer shortBuffer) throws LineUnavailableException {
        short[] shortAudioBuffer = new short[shortBuffer.remaining()];
        shortBuffer.get(shortAudioBuffer);
        byte[] audio = ShortToByte_Twiddle_Method(shortAudioBuffer);
        speaker.write(audio, 0, audio.length);
    }

    private static ShortBuffer recordFromMicrophone(TargetDataLine microphone) throws LineUnavailableException {
        // Assume that the TargetDataLine, line, has already been obtained and
        // opened.

        byte[] data = new byte[microphone.getBufferSize() / 10];
        // probably way too big
        // Here, stopped is a global boolean set by another thread.
        int numBytesRead;
        numBytesRead = microphone.read(data, 0, data.length);
        ShortBuffer shortBuffer = ShortBuffer.allocate(numBytesRead * 2);
        shortBuffer.put(ByteBuffer.wrap(data).asShortBuffer());
        shortBuffer.flip();
        return shortBuffer;
    }

    private static byte[] ShortToByte_Twiddle_Method(final short[] input) {
        final int len = input.length;
        final byte[] buffer = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            buffer[(i * 2) + 1] = (byte) (input[i]);
            buffer[(i * 2)] = (byte) (input[i] >> 8);
        }
        return buffer;
    }

}