package com.savagellc.raven;

public class IntUtils {

    public static int getInt(byte[] sink, int offset)
    {
        return sink[offset + 3] & 0xFF
                | (sink[offset + 2] & 0xFF) << 8
                | (sink[offset + 1] & 0xFF) << 16
                | (sink[offset    ] & 0xFF) << 24;
    }

}
