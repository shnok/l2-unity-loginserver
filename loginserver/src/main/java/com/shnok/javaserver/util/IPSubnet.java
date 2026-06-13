package com.shnok.javaserver.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class IPSubnet {
    final byte[] _addr;
    final byte[] _mask;
    final boolean _isIPv4;

    public IPSubnet(String input) throws UnknownHostException, NumberFormatException, ArrayIndexOutOfBoundsException {
        _addr = InetAddress.getByName(input).getAddress();
        _mask = getMask(_addr.length * 8, _addr.length); // host, no need to check mask
        _isIPv4 = _addr.length == 4;
    }

    public IPSubnet(InetAddress addr, int mask) throws UnknownHostException {
        _addr = addr.getAddress();
        _isIPv4 = _addr.length == 4;
        _mask = getMask(mask, _addr.length);
        if (!applyMask(_addr)) {
            throw new UnknownHostException(addr + "/" + mask);
        }
    }

    public byte[] getAddress() {
        return _addr;
    }

    public boolean applyMask(byte[] addr) {
        // V4 vs V4 or V6 vs V6 checks
        if (_isIPv4 == (addr.length == 4)) {
            for (int i = 0; i < _addr.length; i++) {
                if ((addr[i] & _mask[i]) != _addr[i]) {
                    return false;
                }
            }
        } else {
            // check for embedded v4 in v6 addr (not done !)
            if (_isIPv4) {
                // my V4 vs V6
                for (int i = 0; i < _addr.length; i++) {
                    if ((addr[i + 12] & _mask[i]) != _addr[i]) {
                        return false;
                    }
                }
            } else {
                // my V6 vs V4
                for (int i = 0; i < _addr.length; i++) {
                    if ((addr[i] & _mask[i + 12]) != _addr[i + 12]) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String toString() {
        int size = 0;
        for (byte element : _mask) {
            size += Integer.bitCount((element & 0xFF));
        }

        try {
            return InetAddress.getByAddress(_addr) + "/" + size;
        } catch (UnknownHostException e) {
            return "Invalid";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IPSubnet) {
            return applyMask(((IPSubnet) o).getAddress());
        } else if (o instanceof InetAddress) {
            return applyMask(((InetAddress) o).getAddress());
        }

        return false;
    }

    private static byte[] getMask(int n, int maxLength) throws UnknownHostException {
        if ((n > (maxLength << 3)) || (n < 0)) {
            throw new UnknownHostException("Invalid netmask: " + n);
        }

        final byte[] result = new byte[maxLength];
        Arrays.fill(result, (byte) 0xFF);

        for (int i = (maxLength << 3) - 1; i >= n; i--) {
            result[i >> 3] = (byte) (result[i >> 3] << 1);
        }

        return result;
    }
}