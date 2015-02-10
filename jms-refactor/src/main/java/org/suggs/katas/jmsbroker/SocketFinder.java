package org.suggs.katas.jmsbroker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

public class SocketFinder {

    private static final Logger LOG = LoggerFactory.getLogger(SocketFinder.class);

    public static int findNextAvailablePortBetween(int lowerPort, int higherPort) {
        for (int port = lowerPort; port < higherPort; port++) {
            if (portIsFreeAt(port)) {
                LOG.debug("Found that port {} is free", port);
                return port;
            }
        }
        throw new IllegalStateException("Unable to find an available port between [" + lowerPort + "] and [" + higherPort + "]");
    }

    private static boolean portIsFreeAt(int port) {
        ServerSocket serverSocket = null;
        DatagramSocket dataSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            dataSocket = new DatagramSocket(port);
            dataSocket.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            return false;
        } finally {
            if (dataSocket != null) {
                dataSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                }
            }
        }
    }
}