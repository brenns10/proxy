package io.brennan.proxy;

import java.util.*;

/**
 * This class provides a nice, simple interface to HTTP Headers.
 * Created by stephen on 3/19/16.
 */
public class HttpHeaders {
    /**
     * The parsed headers.  I use a list of strings as the value, because a single header key can occur multiple times.
     */
    private Map<String,String> headers;

    private String normalizeLWS(String fieldValue) {
        //                 remove leading LWS        remove trailing LWS      replace inner LWS with single space
        return fieldValue.replaceAll("^[ \t]+", "").replaceAll("[ \t]+$", "").replaceAll("[ \t]+", " ");
    }

    /**
     * Create an HttpHeader object to parse and manage headers.
     * @param rawHeaders The string containing headers.
     */
    public HttpHeaders(String rawHeaders) {
        this.headers = new LinkedHashMap<>(); // Linked Hash Map remembers the order of keys inserted.
        String[] lines = rawHeaders.split("\r\n");
        String lastKey = null;
        for (String line : lines) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                add(lastKey, normalizeLWS(line));
            } else {
                int colonIndex = line.indexOf(":");
                lastKey = line.substring(0, colonIndex);
                add(lastKey, normalizeLWS(line.substring(colonIndex + 1)));
            }
        }
    }

    /**
     * Return true if the header is in this object.
     * @param key Header key.
     * @return true if it exists
     */
    public boolean contains(String key) {
        return this.headers.containsKey(key);
    }

    /**
     * Return the value of a header.
     * @param key Header name.
     * @return value if it exists, or null
     */
    public String get(String key) {
        return this.headers.get(key);
    }

    /**
     * Remove a header if it exists (otherwise do nothing)
     * @param key header name to remove
     */
    public void remove(String key) {
        this.headers.remove(key);
    }

    /**
     * Set a header to a value (removes all prior values).
     * @param key Header name
     * @param value Header value
     */
    public void set(String key, String value) {
        this.headers.put(key, value);
    }

    /**
     * Add another value to a header.  This should only work for values that are comma separated.
     * @param key Header name
     * @param value Header value
     */
    public void add(String key, String value) {
        if (contains(key)) {
            value = get(key) + "," + value;
        }
        set(key, value);
    }

    /**
     * Return a string corresponding to this header manager.  The string will be somewhat normalized, due to the
     * folowing considerations:
     * - HTTP allows headers to be continued across lines.  The parser also allows this, but it will output headers all
     *   on one line.
     * - The original order of headers MAY NOT be respected.
     * - Multiple values of the same header MAY be concatenated into a single key: value pair, with commas separating
     *   the values, as described in the RFC.
     * @return
     */
    public String assemble() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String,String> entry : this.headers.entrySet()) {
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
            sb.append("\r\n");
        }

        return sb.toString();
    }
}
