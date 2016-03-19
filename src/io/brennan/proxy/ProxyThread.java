package io.brennan.proxy;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Represents a thread that serves a single client connection.
 * Created by stephen on 3/18/16.
 */
public class ProxyThread extends Thread {
    static final Logger logger = Logger.getLogger(ProxyThread.class.getCanonicalName());

    private Socket client;

    public ProxyThread(Socket client) {
        this.client = client;
    }

    public void run() {
        logger.fine("Starting thread " + this.getId());
        try {
            // This flag tracks whether the client would like us to close the connection afterwards.  We assume they do,
            // unless they use the Connection: keep-alive header.
            boolean clientWantsClose = true;

            // We set TCP NoDelay on the connection because we "know better" than the kernel when we want our messages
            // to be sent.  We use BufferedOutputStreams and call flush() when we're finished forwarding a message.
            // This way, we know that our messages get sent when we mean them to.
            this.client.setTcpNoDelay(true);

            for (;;) {
                // Read the request part.
                HttpRequest request = new HttpRequest(this.client.getInputStream());
                HttpHeaders headers = request.getHeaders();

                // Log information about the request we received.
                logger.fine(request.reassembleFirstLine() + " [BodyType " + request.bodyType() + "]" + getId());
                logger.finest("Client Request:\n" + request.reassembleHeaders());

                // Check whether the client would like us to keep the connection alive.
                if (headers.contains("Connection") && headers.get("Connection").equals("keep-alive") ||
                        headers.contains("Proxy-Connection") && headers.get("Proxy-Connection").equals("keep-alive")) {
                    clientWantsClose = false;
                }

                // Modify some headers.
                headers.remove("Proxy-Connection");
                headers.set("Connection", "close");
                headers.set("Host", request.getDestinationHost());

                // Open a socket to the server, and forward the message.
                Socket server = new Socket(request.getDestinationHost(), request.getDestinationPort());
                request.forwardMessage(new BufferedOutputStream(server.getOutputStream()));
                HttpResponse response = new HttpResponse(server.getInputStream());

                // Log information about the response we received.
                logger.fine(response.reassembleFirstLine() + " [BodyType " + request.bodyType() + "]" + getId());
                logger.finest("Server response:\n" + response.reassembleHeaders());

                // Edit some headers.
                headers = response.getHeaders();
                if (!clientWantsClose) {
                    headers.set("Connection", "keep-alive");
                }

                // Now that we have information about the request and response, we put those together to form one mega
                // log entry that will be at a high log level, so we can see the output when we run.
                logger.info("Thread " + getId() + ": " + request.getDestinationHost() + ": " +
                        request.reassembleFirstLine() + " [BodyType " + request.bodyType() + "] " +
                        response.reassembleFirstLine() + " [BodyType " + response.bodyType() + "]");

                // Forward the response to the client :D
                response.forwardMessage(new BufferedOutputStream(this.client.getOutputStream()));

                // Finally, close our connections.
                server.close();
                if (clientWantsClose) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Thread " + getId() + " exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                this.client.close();
            } catch (Exception e) {
                System.out.println("Trouble closing client socket!");
            } finally {
                logger.fine("Finishing thread " + this.getId());
            }
        }
    }

}
