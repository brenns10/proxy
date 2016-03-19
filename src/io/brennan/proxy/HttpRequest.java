package io.brennan.proxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * This class represents an HttpMessage that is specifically a request.  It implements operations that deal with the
 * request line as well as a few functions that exist specifically for requests, like determining the hostname the
 * request is destined for.
 * Created by stephen on 3/18/16.
 */
public class HttpRequest extends HttpMessage {

    private String method;
    private String url;
    private String version;
    private String hostname;

    /**
     * Create a new HTTP request object, immediately reading and parsing all headers.
     * @param stream Input stream, presumably from a socket.
     * @throws IOException On error from input stream.
     */
    public HttpRequest(InputStream stream) throws IOException {
        super(stream);
    }

    /**
     * Parse the HTTP request line.  This sets the method, url, and version fields.  This method is actually called by
     * the parent class's constructor, and thus should not be called elsewhere.
     *
     * One important operation this method does is checking if the URL argument contains the hostname.  If so, it parses
     * that out so that only the path portion of the URL is returned by reassembleFirstLine().  This is necessary for
     * proxying, and it's also just good HTTP/1.1 form.
     * @throws IOException
     */
    @Override
    protected void parseFirstLine() throws IOException {
        byte[] linebytes = readUntilNewline(this.stream);
        String line = new String(linebytes);
        line = line.substring(0, line.length() - 2); // remove CRLF
        String[] elements = line.split(" ", 3);
        this.method = elements[0];
        this.url = elements[1];
        if (this.url.startsWith("http://")) {
            int urlStart = this.url.indexOf("/", 7);
            this.hostname = this.url.substring(7, urlStart);
            this.url = this.url.substring(urlStart);
        }
        this.version = elements[2];
    }

    /**
     * Return a string containing the reassembled request line.  If the original request contained a hostname, this will
     * not include it - only the path portion of that URL.
     * @return reassembled request line
     */
    @Override
    protected String reassembleFirstLine() {
        return this.getMethod() + " " + this.getUrl() + " " + this.getVersion();
    }

    /**
     * Return HTTP method used.
     * @return HTTP method.
     */
    public String getMethod() {
        return method;
    }

    /**
     * Return the URL of the request (no hostname)
     * @return URL path without hostname
     */
    public String getUrl() {
        return url;
    }

    /**
     * Return the HTTP version as a string: HTTP/x.x
     * @return HTTP version
     */
    public String getVersion() {
        return version;
    }

    private String getHostString() {
        if (this.hostname != null) {
            return this.hostname;
        } else {
            return this.headers.get("Host");
        }
    }

    /**
     * Return the hostname of the destination!  This will search for it in two ways:
     * - If the request included a hostname in the URL, use that.
     * - Otherwise, look for a HTTP Host header.
     * If neither are present, I'm pretty sure this will throw an NPE, soooooo....
     * @return hostname of request
     */
    public String getDestinationHost() {
        String hostString = getHostString();
        int colonLocation = hostString.indexOf(":");
        if (colonLocation == -1) {
            return hostString;
        } else {
            return hostString.substring(0, colonLocation);
        }
    }

    /**
     * Return the destination port.  This is normally 80, but a different host may have been specified in the hostname.
     * So, this function checks for a port number in the hostname, and if one is not found, returns port 80.
     * @return port number for the destination host
     */
    public int getDestinationPort() {
        String hostString = getHostString();
        int colonLocation = hostString.indexOf(":");
        if (colonLocation == -1) {
            return 80;
        } else {
            return Integer.parseInt(hostString.substring(colonLocation+1));
        }
    }
}
