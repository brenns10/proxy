package io.brennan.proxy;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class represents an HttpMessage that is specifically a response.  It implements methods for dealing with the
 * status line in HTTP responses.
 * Created by stephen on 3/18/16.
 */
public class HttpResponse extends HttpMessage {

    private String version;
    private String status;
    private String description;

    /**
     * Create a new HttpResponse, immediately reading and parsing everything up to the end of the headers.
     * @param is Input stream from the socket.
     * @throws IOException On error reading from socket.
     */
    public HttpResponse(InputStream is) throws IOException {
        super(is);
    }

    /**
     * Read and parse the status line of the response.  This is called by constructor.
     * @throws IOException on error reading.
     */
    @Override
    protected void parseFirstLine() throws IOException {
        byte[] linebytes = readUntilNewline(this.stream);
        String line = new String(linebytes);
        line = line.substring(0, line.length() - 2); // remove CRLF
        String[] elements = line.split(" ", 3);
        this.version = elements[0];
        this.status = elements[1];
        this.description = elements[2];
    }

    /**
     * Return the original status line (just reverses the parsing done above)
     * @return original status line (without CRLF).
     */
    @Override
    protected String reassembleFirstLine() {
        return version + " " + status + " " + description;
    }

    /**
     * Return HTTP version as a string: HTTP/x.x
     * @return HTTP version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Return HTTP status code as a string (should be parseable with Integer.parseInt()).
     * @return HTTP status code
     */
    public String getStatus() {
        return status;
    }

    /**
     * Return HTTP status code reason.
     * @return status code reason
     */
    public String getDescription() {
        return description;
    }
}
