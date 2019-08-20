package com.savagellc.raven;

import java.nio.ByteBuffer;

public class IOUtils {

    public static short getShortBigEndian(byte[] arr, int offset)
    {
        return (short) ((arr[offset    ] & 0xff) << 8
                | arr[offset + 1] & 0xff);
    }
    public static boolean hasExtension(byte profile) {
        return (profile & 0x10) != 0;
    }
    public static byte getCc(byte profile) {
        return (byte) (profile & 0x0f);
    }

    public static short getShortLittleEndian(byte[] arr, int offset)
    {
        // Same as big endian but reversed order of bytes (java uses big endian)
        return (short) ((arr[offset    ] & 0xff)
                | (arr[offset + 1] & 0xff) << 8);
    }

    public static int getIntBigEndian(byte[] arr, int offset)
    {
        return arr[offset + 3] & 0xFF
                | (arr[offset + 2] & 0xFF) << 8
                | (arr[offset + 1] & 0xFF) << 16
                | (arr[offset    ] & 0xFF) << 24;
    }

    public static void setIntBigEndian(byte[] arr, int offset, int it)
    {
        arr[offset    ] = (byte) ((it >>> 24) & 0xFF);
        arr[offset + 1] = (byte) ((it >>> 16) & 0xFF);
        arr[offset + 2] = (byte) ((it >>> 8)  & 0xFF);
        arr[offset + 3] = (byte) ( it         & 0xFF);
    }

    public static ByteBuffer reallocate(ByteBuffer original, int length)
    {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.put(original);
        return buffer;
    }
}
