HTTP Proxy
==========

This is a HTTP proxy server written in Java.  Although this was an assignment in my EECS 325 Networking class, this is
not the proxy I wrote for that class.  This is a proxy I have written since then, with the benefit of much more
knowledge about HTTP.

In my original proxy, I did not bother understanding that there are several different ways to determine the length of a
message body, and thus know when to stop forwarding a request or response body.  So I simply wrote a thread that would
forward bytes until a connection closed, or until I killed it because I had received a new request.  That strategy is
perhaps easier, but not a good idea at all.

In this proxy, I fully parse the request, determine what type of message body it uses, and forward it using a method
that understands that transfer encoding.  That way, the forwarding always stops at the correct time, and I can reuse
client connections properly.

Build with `javac`, (make sure you are aware of the classpath, `io.brennan.proxy`).  Run with
`java io.brennan.proxy.Main [portnum]`.