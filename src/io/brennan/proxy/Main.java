package io.brennan.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.*;

/**
 * It's a proxy, bitch!
 * Created by stephen on 3/18/16.
 */
public class Main {
    public static void main(String[] args) {
        // Before anything else, let's set up logging for the Proxy!.
        ProxyThread.logger.setLevel(Level.ALL); // don't let the logger filter anything, leave it to the handlers
        ProxyThread.logger.setUseParentHandlers(false); // don't let the root logger handle our messages
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new Formatter() { // define a simple format: LEVEL MESSAGE
            @Override
            public String format(LogRecord logRecord) {
                return logRecord.getLevel() + " " + logRecord.getMessage() + "\n";
            }
        });
        ch.setLevel(Level.INFO); // the level of messages we will see on the console
        ProxyThread.logger.addHandler(ch); // put logging on console

        // Verify args.
        if (args.length != 1) {
            System.err.println("usage: io.brennan.proxy.Main port");
            return;
        }

        int port = Integer.parseInt(args[0]);
        ServerSocket serverSocket;

        // Create "server" socket for accepting connections.
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.err.println("failed to open server socket:");
            System.err.println(e.getMessage());
            return;
        }

        // Now loop accepting connections and spawning threads to handle them.
        try {
            for (;;) {
                Socket client = serverSocket.accept();
                ProxyThread thread = new ProxyThread(client);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("failed to accept client connection:");
            System.err.println(e.getMessage());
        } finally {
            try {
                // At the end of the day, always make sure the socket gets closed.
                serverSocket.close();
            } catch (Exception e) {}
        }

    }
}
