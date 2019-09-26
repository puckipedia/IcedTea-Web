package net.adoptopenjdk.icedteaweb.proxy;

import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProxyUtils {

    private final static Logger LOG = LoggerFactory.getLogger(ProxyUtils.class);

    /**
     * @param host host to verify
     * @return true if the host is the hostname or the IP address of the
     * localhost
     */
    public static boolean isLocalHost(final String host) {
        try {
            if (InetAddress.getByName(host).isLoopbackAddress()) {
                return true;
            }
        } catch (final UnknownHostException e) {
            // continue
        }
        try {
            if (Objects.equals(host, InetAddress.getLocalHost().getHostName())) {
                return true;
            }
        } catch (final UnknownHostException e) {
            // continue
        }
        try {
            if (Objects.equals(host, InetAddress.getLocalHost().getHostAddress())) {
                return true;
            }
        } catch (final UnknownHostException e) {
            // continue
        }
        return false;
    }

    public static List<Proxy> getFromArguments(URI uri,
                                               boolean sameProxy, boolean sameProxyIncludesSocket,
                                               String proxyHttpsHost, int proxyHttpsPort,
                                               String proxyHttpHost, int proxyHttpPort,
                                               String proxyFtpHost, int proxyFtpPort,
                                               String proxySocks4Host, int proxySocks4Port) {
        List<Proxy> proxies = new ArrayList<>();

        String scheme = uri.getScheme();

        boolean socksProxyAdded = false;

        if (sameProxy) {
            if (proxyHttpHost != null) {
                SocketAddress sa = new InetSocketAddress(proxyHttpHost, proxyHttpPort);
                if ((scheme.equals("https") || scheme.equals("http") || scheme.equals("ftp"))) {
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
                    proxies.add(proxy);
                } else if (scheme.equals("socket") && sameProxyIncludesSocket) {
                    Proxy proxy = new Proxy(Proxy.Type.SOCKS, sa);
                    proxies.add(proxy);
                    socksProxyAdded = true;
                }
            }
        } else if (scheme.equals("http") && proxyHttpHost != null) {
            SocketAddress sa = new InetSocketAddress(proxyHttpHost, proxyHttpPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        } else if (scheme.equals("https") && proxyHttpsHost != null) {
            SocketAddress sa = new InetSocketAddress(proxyHttpsHost, proxyHttpsPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        } else if (scheme.equals("ftp") && proxyFtpHost != null) {
            SocketAddress sa = new InetSocketAddress(proxyFtpHost, proxyFtpPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        }

        if (!socksProxyAdded && (proxySocks4Host != null)) {
            SocketAddress sa = new InetSocketAddress(proxySocks4Host, proxySocks4Port);
            proxies.add(new Proxy(Proxy.Type.SOCKS, sa));
            socksProxyAdded = true;
        }

        if (proxies.isEmpty()) {
            proxies.add(Proxy.NO_PROXY);
        }

        return proxies;
    }

    /**
     * Converts a proxy string from a browser into a List of Proxy objects
     * suitable for java.
     *
     * @param pacString a string indicating proxies. For example
     *                  "PROXY foo.bar:3128; DIRECT"
     * @return a list of Proxy objects representing the parsed string. In
     * case of malformed input, an empty list may be returned
     */
    public static List<Proxy> getProxiesFromPacResult(String pacString) {
        List<Proxy> proxies = new ArrayList<>();

        String[] tokens = pacString.split(";");
        for (String token : tokens) {
            if (token.startsWith("PROXY")) {
                String hostPortPair = token.substring("PROXY".length()).trim();
                if (!hostPortPair.contains(":")) {
                    continue;
                }
                String host = hostPortPair.split(":")[0];
                int port;
                try {
                    port = Integer.valueOf(hostPortPair.split(":")[1]);
                } catch (NumberFormatException nfe) {
                    continue;
                }
                SocketAddress sa = new InetSocketAddress(host, port);
                proxies.add(new Proxy(Proxy.Type.HTTP, sa));
            } else if (token.startsWith("SOCKS")) {
                String hostPortPair = token.substring("SOCKS".length()).trim();
                if (!hostPortPair.contains(":")) {
                    continue;
                }
                String host = hostPortPair.split(":")[0];
                int port;
                try {
                    port = Integer.valueOf(hostPortPair.split(":")[1]);
                } catch (NumberFormatException nfe) {
                    continue;
                }
                SocketAddress sa = new InetSocketAddress(host, port);
                proxies.add(new Proxy(Proxy.Type.SOCKS, sa));
            } else if (token.startsWith("DIRECT")) {
                proxies.add(Proxy.NO_PROXY);
            } else {
                LOG.debug("Unrecognized proxy token: {}", token);
            }
        }

        return proxies;
    }
}
