package io.brennan.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static io.brennan.proxy.BodyType.Identity;

/**
 * This class represents an HTTP message.  It is intended to support both HTTP Requests and HTTP Responses.  Subclasses
 * must implement parsing and reassembly of the first line of the message, but otherwise this class takes care of the
 * rest.
 *
 * When an instance is created, it takes an InputStream, from which it reads and parses headers immediately.  Headers
 * may be accessed via getHeaders(), and may also be modified.  The headers of the message may then be reassembled with
 * reassembleHeaders().  Reassembled headers may be sent to an output stream with forwardHeaders().  However, you
 * probably do not want to do this, because this class offers a more powerful forwarding mechanism, described below.
 *
 * This class may also be used to forward the entire HTTP message into an output stream.  In this case, the
 * forwardMessage() method should be used (without calling forwardHeaders()).  This function may modify the headers in
 * the following ways:
 * - If Transfer-Encoding: identity is specified, it will be replaced with Transfer-Encoding: chunked, so that
 *   forwarding doesn't require knowledge of the size of message.
 *
 * Created by stephen on 3/18/16.
 */
public abstract class HttpMessage {

    /**
     * This helper function takes an input stream and reads a line, up to CRLF.  It assumes that the caller intends for
     * there to be a full line of input, so if the connection terminates (e.g. by read() returning -1) prematurely, this
     * will throw an IOException.
     * @param stream Stream to read a line from.
     * @return A byte array of line content.
     * @throws IOException if read() fails, or if it returns -1 when we expect a character.
     */
    static byte[] readUntilNewline(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int state = 0;
        while (state != 2) {
            int character = stream.read();
            if (character == -1) throw new IOException("Unexpected end of stream.");
            buffer.write(character);
            if (state == 0 && character == '\r') {
                state = 1;
            } else if (state == 1 && character == '\n') {
                state = 2;
            } else {
                state = 0;
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Helper function: send a certain number of bytes from an input stream to an output stream, using a buffer.
     * @param input Stream to send from.
     * @param output Stream to send to.
     * @param nbytes Number of bytes to send.
     * @throws IOException
     */
    static void sendNBytes(InputStream input, OutputStream output, int nbytes) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int sent = 0;
        while (sent < nbytes) {
            int read = input.read(buffer, 0, Math.min(nbytes - sent, buffer.length));
            output.write(buffer, 0, read);
            sent += read;
        }
    }


    /**
     * All of my body forwarding messages use byte arrays so they can read and write larger blocks of bytes at a time,
     * rather than just byte by byte.
     */
    static final int BUFFER_SIZE = 4096;

    /**
     * The input stream of the connection.  Probably from Socket.getInputStream().
     */
    protected InputStream stream;
    /**
     * The unparsed header data (not including first line) read from the socket.
     */
    protected String rawHeaders;

    /**
     * A HttpHeaders object manages headers so we don't need to worry about them.
     */
    protected HttpHeaders headers;

    /**
     * Return the header mapping.
     * @return Header mapping.
     */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /**
     * Create a HttpMessage.  This immediately reads the message's start-line and headers.
     * @param stream The input stream from the socket.
     * @throws IOException If reading fromString the socket fails before we're through.
     */
    protected HttpMessage(InputStream stream) throws IOException {
        this.stream = stream;
        this.readHeaders();
        this.headers = new HttpHeaders(this.rawHeaders);
    }

    /**
     * The start line of a HTTP message differs between requests and responses, and is thus delegated to subclasses for
     * implementation.
     */
    protected abstract void parseFirstLine() throws IOException;

    /**
     * Returns the reassembled first line of the HTTP message.
     * @return see above :P
     */
    protected abstract String reassembleFirstLine();

    /**
     * Read the headers from the input stream.  This converts them to a string for further parsing.
     * @throws IOException
     */
    private void readHeaders() throws IOException {
        this.parseFirstLine();
        ByteArrayOutputStream bstr = new ByteArrayOutputStream();

        // This reads until CRLFCRLF, which is when the headers are done.
        int state = 0;
        while (state != 4) {
            int character = this.stream.read();
            if (character == -1) {
                System.out.println("Early termination in readHeaders()");
                break;
            }
            bstr.write(character);
            if (character == '\r' && (state == 0 || state == 2)) {
                state++;
            } else if (character == '\n' && (state == 1 || state == 3)) {
                state++;
            } else {
                state = 0;
            }
        }

        this.rawHeaders = bstr.toString();
        this.rawHeaders = this.rawHeaders.substring(0, this.rawHeaders.length() - 4);  // remove CRLF CRLF
    }

    /**
     * Return which body type is indicated by the headers.
     * @return true when body present
     */
    public BodyType bodyType() {
        if (this.headers.contains("Content-Length")){
            return BodyType.ContentLength;
        } else if (this.headers.contains("Transfer-Encoding")) {
            String value = this.headers.get("Transfer-Encoding").toLowerCase();
            if (value.equals("identity")) {
                return Identity;
            } else {
                return BodyType.Chunked;
            }
        } else {
            return BodyType.None;
        }
    }

    /**
     * Returns a string containing all the headers, reassembled once more.
     * @return the reassembled headers.
     */
    public String reassembleHeaders() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.reassembleFirstLine());
        sb.append("\r\n");
        sb.append(this.headers.assemble());
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Forwards headers into an OutputStream.
     */
    public void forwardHeaders(OutputStream os) throws IOException {
        String headers = this.reassembleHeaders();
        os.write(headers.getBytes());
    }

    /**
     * Forwards a message that uses identity transfer encoding.  This forwards the message using chunked transfer
     * encoding, so the headers should be updated to reflect that.
     * @param os Stream to forward into.
     * @throws IOException
     */
    private void forwardIdentityBody(OutputStream os) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        for (;;) {
            int bytes = this.stream.read(buffer);
            if (bytes == -1) break;
            String chunkHeader = Integer.toHexString(bytes) + "\r\n";
            os.write(chunkHeader.getBytes());
            os.write(buffer, 0, bytes);
            os.write("\r\n".getBytes());
        }
        os.write("0\r\n".getBytes());
    }

    /**
     * Forward a body that uses chunked transfer encoding.  Requires no changes to headers.
     * @param os OutputStream to forward into.
     * @throws IOException
     */
    private void forwardChunkedBody(OutputStream os) throws IOException {
        for(;;) {
            // First, read the header.  That is, read until \r\n
            byte[] header = readUntilNewline(this.stream);

            // Then get the number of bytes in the chunk.  This is written leniently, to allow for the possibility of a
            // chunk extension as mentioned in RFC 2616.
            String headerstr = new String(header);
            int lastIndex = headerstr.indexOf(";");
            lastIndex = lastIndex == -1 ? headerstr.length() - 2 : lastIndex;
            int chunkSize = Integer.parseInt(headerstr.substring(0, lastIndex), 16);
            os.write(header);

            // Stop when header shows 0 byte chunk.
            if (chunkSize == 0) {
                break;
            }

            // Send the chunk, plus the CRLF
            sendNBytes(this.stream, os, chunkSize + 2);
        }
        // TODO allow headers after the chunked body, as mentioned in RFC 2616.
        os.write("\r\n".getBytes());
    }

    /**
     * Forward a message body that is determined by a content-length.
     * @param os OutputStream to write to.
     * @throws IOException
     */
    private void forwardContentLengthBody(OutputStream os) throws IOException {
        int contentLength = Integer.parseInt(this.headers.get("Content-Length"));
        sendNBytes(this.stream, os, contentLength);
    }

    /**
     * Forwards an entire message into an OutputStream.  This may modify some headers!
     */
    public void forwardMessage(OutputStream os) throws IOException {
        if (this.bodyType() == Identity) {
            this.headers.set("Transfer-Encoding", "chunked");
        }
        this.forwardHeaders(os);
        switch (this.bodyType()) {
            case Identity:
                this.forwardIdentityBody(os);
                break;
            case Chunked:
                this.forwardChunkedBody(os);
                break;
            case ContentLength:
                this.forwardContentLengthBody(os);
                break;
        }
        os.flush();
    }
}
