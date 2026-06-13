package com.shnok.javaserver.dto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public abstract class ReceivablePacket extends Packet {
    private int iterator;

    public ReceivablePacket(byte[] data) {
        super(data);
        readB();
    }

    protected byte readB() {
        return packetData[iterator++];
    }

    protected byte[] readB(int size) {
        byte[] readBytes = Arrays.copyOfRange(packetData, iterator, iterator + size);
        iterator += size;
        return readBytes;
    }

//    protected int readI() {
//        byte[] array = new byte[4];
//        System.arraycopy(packetData, iterator, array, 0, 4);
//        iterator += 4;
//        return ByteBuffer.wrap(array).getInt();
//    }

    protected int readI() {
        return (packetData[iterator++] & 0xff) | ((packetData[iterator++] & 0xff) << 8) | ((packetData[iterator++] & 0xff) << 16) | ((packetData[iterator++] & 0xff) << 24);
    }

    public int readH()
    {
        return (packetData[iterator++] & 0xff) | ((packetData[iterator++] & 0xff) << 8);
    }
//    protected float readF() {
//        byte[] array = new byte[4];
//        System.arraycopy(packetData, iterator, array, 0, 4);
//        iterator += 4;
//        return ByteBuffer.wrap(array).getFloat();
//    }

//    protected String readS() {
//        byte strLen = readB();
//        byte[] data = new byte[strLen];
//        System.arraycopy(packetData, iterator, data, 0, strLen);
//        iterator += strLen;
//
//        return new String(data, 0, strLen, StandardCharsets.UTF_8);
//    }

    public String readS() {
        final int start = iterator;

        // Find the null terminator in UTF-16LE encoding.
        int end = start;
        while (end < packetData.length - 1 && (packetData[end] != 0 || packetData[end + 1] != 0))
            end += 2;

        // Move the offset past the string and the null terminator.
        iterator += (end - start) + 2;

        // Create a string from the bytes between start and end.
        String s = new String(packetData, start, end - start, StandardCharsets.UTF_16LE);

        byte[] data = new byte[end - start];
        System.arraycopy(packetData, start, data, 0, end - start);
        return s;
    }
}
