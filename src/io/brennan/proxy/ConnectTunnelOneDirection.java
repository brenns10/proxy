package io.brennan.proxy;

import java.io.IOException;
import java.net.Socket;

/**
 * Implements CONNECT tunneling for HTTPS and other TCP connections.  This class spawns two threads, which simply read
 * and write until a connection closes (and then they terminate).
 * Created by stephen on 3/19/16.
 */
public class ConnectTunnelOneDirection extends Thread {

    private Socket from;
    private Socket to;
    private String name;
    private static final int BUFFER_SIZE = 4096;

    public ConnectTunnelOneDirection(Socket from, Socket to, String name) {
        this.from = from;
        this.to = to;
        this.name = name;
    }

    public void run() {
        System.out.println("ConnectTunnelOneDirection " + name + " starting");
        byte[] buffer = new byte[BUFFER_SIZE];
        int read;
        while (true) {
            try {
                read = from.getInputStream().read(buffer);
            } catch (IOException e) {
                System.out.println("Exception reading in " + name + ".");
                break;
            }
            if (read == -1) {
                System.out.println("Read returned -1 in  " + name + ".");
                break;
            }
            try {
                to.getOutputStream().write(buffer, 0, read);
            } catch (IOException e) {
                 System.out.println("Exception writing in  " + name + ".");
                 break;
            }
        }
        try {
            from.close();
        } catch (IOException e) {}
        try {
            to.close();
        } catch (IOException e) {}
        System.out.println("ConnectTunnelOneDirection " + name + " closing.");
    }

    private class OneDirection extends Thread {

        public OneDirection(Socket from, Socket to) {

        }

        @Override
        public void run() {
        }
    }
}
