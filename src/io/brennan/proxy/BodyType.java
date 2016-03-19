package io.brennan.proxy;

/**
 * An enumeration representing the possible types of message body an HTTP message con have.
 * None: no message body
 * ContentLength: a Content-Length header specifies the length of the body in bytes
 * Identity: server will close the connection when the body is finished (can't be used by clients)
 * Chunked: server will send several chunks, consisting of a number of bytes, followed by that many bytes of message
 */
public enum BodyType {
    None, ContentLength, Identity, Chunked
}
