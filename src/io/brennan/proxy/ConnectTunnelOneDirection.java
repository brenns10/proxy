package io.brennan.proxy;

import java.io.IOException;
import java.net.Socket;

/**
 * This is a thread that forwards one half of a TCP connection.
 * Created by stephen on 3/19/16.
 */
public class ConnectTunnelOneDirection extends Thread {

    private Socket from;
    private Socket to;
    private String name;
    private static final int BUFFER_SIZE = 4096;

    /**
     * Create a new thread to forward messages from one socket into another.  To successfully tunnel a connection, you
     * need to forward in both directions, so you need two instances of this class.
     * @param from Socket to forward messages from.
     * @param to Socket to forward messages into.
     * @param name A name to use when printing out diagnostic messages.
     */
    public ConnectTunnelOneDirection(Socket from, Socket to, String name) {
        this.from = from;
        this.to = to;
        this.name = name;
    }

    /**
     * Run the thread forever (or until the connection is closed).
     */
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
}
