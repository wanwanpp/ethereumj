package org.ethereum.sharding.util;

import org.spongycastle.util.encoders.HexEncoder;

import java.io.ByteArrayOutputStream;

/**
 * @author Mikhail Kalinin
 * @since 28.11.2018
 */
public class Hex {
    private static final HexEncoder encoder = new HexEncoder();

    public static String toHexString(byte[] data) {
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            encoder.encode(data, 0, data.length, bOut);
            return new String(bOut.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encode Hex string: " + e.getMessage(), e);
        }
    }

    public static byte[] fromHexString(String data) {
        try {
            assert data.length() % 2 == 0;

            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            encoder.decode(data.startsWith("0x") ? data.substring(2) : data, bOut);
            return bOut.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode Hex string: " + e.getMessage(), e);
        }
    }
}
