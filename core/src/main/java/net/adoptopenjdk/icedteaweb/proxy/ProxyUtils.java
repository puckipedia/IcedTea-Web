package net.adoptopenjdk.icedteaweb.proxy;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static net.adoptopenjdk.icedteaweb.proxy.ProxyConstants.PAC_TOKEN_DIRECT_PREFIX;
import static net.adoptopenjdk.icedteaweb.proxy.ProxyConstants.PAC_TOKEN_PROXY_PREFIX;
import static net.adoptopenjdk.icedteaweb.proxy.ProxyConstants.PAC_TOKEN_SOCKS_PREFIX;

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
        } catch (final UnknownHostException e) {}
        try {
            if (Objects.equals(host, InetAddress.getLocalHost().getHostName())) {
                return true;
            }
        } catch (final UnknownHostException e) {}
        try {
            if (Objects.equals(host, InetAddress.getLocalHost().getHostAddress())) {
                return true;
            }
        } catch (final UnknownHostException e) {}
        return false;
    }

    public static List<Proxy> getFromArguments(final URI uri,
                                               final boolean sameProxy, final boolean sameProxyIncludesSocket,
                                               final String proxyHttpsHost, final int proxyHttpsPort,
                                               final String proxyHttpHost, final int proxyHttpPort,
                                               final String proxyFtpHost, final int proxyFtpPort,
                                               final String proxySocks4Host, final int proxySocks4Port) {
        Assert.requireNonNull(uri, "uri");

        final List<Proxy> proxies = new ArrayList<>();

        final String scheme = uri.getScheme();

        boolean socksProxyAdded = false;

        if (sameProxy) {
            if (proxyHttpHost != null) {
                final SocketAddress sa = new InetSocketAddress(proxyHttpHost, proxyHttpPort);
                if ((scheme.equals(ProxyConstants.HTTPS_SCHEME) || scheme.equals(ProxyConstants.HTTP_SCHEME) || scheme.equals(ProxyConstants.FTP_SCHEME))) {
                    final Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
                    proxies.add(proxy);
                } else if (scheme.equals(ProxyConstants.SOCKET_SCHEME) && sameProxyIncludesSocket) {
                    final Proxy proxy = new Proxy(Proxy.Type.SOCKS, sa);
                    proxies.add(proxy);
                    socksProxyAdded = true;
                }
            }
        } else if (scheme.equals(ProxyConstants.HTTP_SCHEME) && proxyHttpHost != null) {
            final SocketAddress sa = new InetSocketAddress(proxyHttpHost, proxyHttpPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        } else if (scheme.equals(ProxyConstants.HTTPS_SCHEME) && proxyHttpsHost != null) {
            final SocketAddress sa = new InetSocketAddress(proxyHttpsHost, proxyHttpsPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        } else if (scheme.equals(ProxyConstants.FTP_SCHEME) && proxyFtpHost != null) {
            final SocketAddress sa = new InetSocketAddress(proxyFtpHost, proxyFtpPort);
            proxies.add(new Proxy(Proxy.Type.HTTP, sa));
        }

        if (!socksProxyAdded && (proxySocks4Host != null)) {
            final SocketAddress sa = new InetSocketAddress(proxySocks4Host, proxySocks4Port);
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
    public static List<Proxy> getProxiesFromPacResult(final String pacString) {
        final List<Proxy> proxies = new ArrayList<>();

        final String[] tokens = pacString.split(";");
        for (final String token : tokens) {
            if (token.startsWith(PAC_TOKEN_PROXY_PREFIX)) {
                final String hostPortPair = token.substring(PAC_TOKEN_PROXY_PREFIX.length()).trim();
                if (!hostPortPair.contains(":")) {
                    continue;
                }
                final String host = hostPortPair.split(":")[0];
                int port;
                try {
                    port = Integer.valueOf(hostPortPair.split(":")[1]);
                } catch (final NumberFormatException nfe) {
                    continue;
                }
                final SocketAddress sa = new InetSocketAddress(host, port);
                proxies.add(new Proxy(Proxy.Type.HTTP, sa));
            } else if (token.startsWith(PAC_TOKEN_SOCKS_PREFIX)) {
                final String hostPortPair = token.substring(PAC_TOKEN_SOCKS_PREFIX.length()).trim();
                if (!hostPortPair.contains(":")) {
                    continue;
                }
                final String host = hostPortPair.split(":")[0];
                int port;
                try {
                    port = Integer.valueOf(hostPortPair.split(":")[1]);
                } catch (NumberFormatException nfe) {
                    continue;
                }
                final SocketAddress sa = new InetSocketAddress(host, port);
                proxies.add(new Proxy(Proxy.Type.SOCKS, sa));
            } else if (token.startsWith(PAC_TOKEN_DIRECT_PREFIX)) {
                proxies.add(Proxy.NO_PROXY);
            } else {
                LOG.debug("Unrecognized proxy token: {}", token);
            }
        }
        return Collections.unmodifiableList(proxies);
    }

    public static ProxyType getProxyTypeFromConfigValue(final int value) {
        if(value == -1) {
            return ProxyType.PROXY_TYPE_UNKNOWN;
        }
        if(value == 0) {
            return ProxyType.PROXY_TYPE_NONE;
        }
        if(value == 1) {
            return ProxyType.PROXY_TYPE_MANUAL;
        }
        if(value == 2) {
            return ProxyType.PROXY_TYPE_AUTO;
        }
        if(value == 3) {
            return ProxyType.PROXY_TYPE_BROWSER;
        }
        LOG.warn("Invalid config value for proxy type. Will use " + ProxyType.PROXY_TYPE_UNKNOWN);
        return ProxyType.PROXY_TYPE_UNKNOWN;

    }
}
