package net.adoptopenjdk.icedteaweb.proxy;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.sourceforge.jnlp.config.ConfigurationConstants;
import net.sourceforge.jnlp.config.DeploymentConfiguration;
import net.sourceforge.jnlp.runtime.PacEvaluator;
import net.sourceforge.jnlp.runtime.PacEvaluatorFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JNLPProxyConfig {


    private final static Logger LOG = LoggerFactory.getLogger(JNLPProxyConfig.class);

    /**
     * The default port to use as a fallback. Currently squid's default port
     */

    private final PacEvaluator pacEvaluator;

    /**
     * The proxy type. See PROXY_TYPE_* constants
     */
    private final ProxyType proxyType;

    /**
     * the URL to the PAC file
     */
    private final URL autoConfigUrl;

    /**
     * a list of URLs that should be bypassed for proxy purposes
     */
    private final List<String> bypassList;

    /**
     * whether localhost should be bypassed for proxy purposes
     */
    private final boolean bypassLocal;

    /**
     * whether the http proxy should be used for https and ftp protocols as well
     */
    private final boolean sameProxy;

    private final String proxyHttpHost;

    private final int proxyHttpPort;

    private final String proxyHttpsHost;

    private final int proxyHttpsPort;

    private final String proxyFtpHost;

    private final int proxyFtpPort;

    private final String proxySocks4Host;

    private final int proxySocks4Port;

    public JNLPProxyConfig(final DeploymentConfiguration config) {
        Assert.requireNonNull(config, "config");

        proxyType = ProxyUtils.getProxyTypeFromConfigValue(Integer.valueOf(config.getProperty(ConfigurationConstants.KEY_PROXY_TYPE)));

        autoConfigUrl = Optional.ofNullable(config.getProperty(ConfigurationConstants.KEY_PROXY_AUTO_CONFIG_URL))
                .map(s -> {
                    try {
                        return new URL(s);
                    } catch (MalformedURLException e) {
                        LOG.error("Can not load auto config url for proxy", e);
                        return null;
                    }
                }).orElse(null);

        pacEvaluator = Optional.ofNullable(autoConfigUrl)
                .map(u -> PacEvaluatorFactory.getPacEvaluator(autoConfigUrl))
                .orElse(null);


        final String proxyBypass = config.getProperty(ConfigurationConstants.KEY_PROXY_BYPASS_LIST);
        if (proxyBypass != null) {
            bypassList = Arrays.asList(proxyBypass.split(",")).stream().filter(s -> s != null && s.trim().length() != 0).collect(Collectors.toList());
        } else {
            bypassList = Collections.emptyList();
        }

        bypassLocal = Boolean.valueOf(config
                .getProperty(ConfigurationConstants.KEY_PROXY_BYPASS_LOCAL));

        sameProxy = Boolean.valueOf(config.getProperty(ConfigurationConstants.KEY_PROXY_SAME));

        proxyHttpHost = getHost(config, ConfigurationConstants.KEY_PROXY_HTTP_HOST);
        proxyHttpPort = getPort(config, ConfigurationConstants.KEY_PROXY_HTTP_PORT);

        proxyHttpsHost = getHost(config, ConfigurationConstants.KEY_PROXY_HTTPS_HOST);
        proxyHttpsPort = getPort(config, ConfigurationConstants.KEY_PROXY_HTTPS_PORT);

        proxyFtpHost = getHost(config, ConfigurationConstants.KEY_PROXY_FTP_HOST);
        proxyFtpPort = getPort(config, ConfigurationConstants.KEY_PROXY_FTP_PORT);

        proxySocks4Host = getHost(config, ConfigurationConstants.KEY_PROXY_SOCKS4_HOST);
        proxySocks4Port = getPort(config, ConfigurationConstants.KEY_PROXY_SOCKS4_PORT);
    }

    /**
     * Uses the given key to get a host from the configuration
     */
    private String getHost(final DeploymentConfiguration config, final String key) {
        Assert.requireNonNull(config, "config");
        return Optional.ofNullable(config.getProperty(key))
                .map(v -> v.trim())
                .orElse(null);
    }

    /**
     * Uses the given key to get a port from the configuration
     */
    private int getPort(final DeploymentConfiguration config, final String key) {
        Assert.requireNonNull(config, "config");
        return Optional.ofNullable(config.getProperty(key))
                .map(v -> {
                    try {
                        return Integer.valueOf(v);
                    } catch (NumberFormatException e) {
                        LOG.error("Can not parse proxy port", e);
                        return ProxyConstants.FALLBACK_PROXY_PORT;
                    }
                })
                .orElseThrow(() -> new IllegalStateException("Error while reading proxy port"));
    }

    public PacEvaluator getPacEvaluator() {
        return pacEvaluator;
    }

    public ProxyType getProxyType() {
        return proxyType;
    }

    public URL getAutoConfigUrl() {
        return autoConfigUrl;
    }

    public List<String> getBypassList() {
        return bypassList;
    }

    public boolean isBypassLocal() {
        return bypassLocal;
    }

    public boolean isSameProxy() {
        return sameProxy;
    }

    public String getProxyHttpHost() {
        return proxyHttpHost;
    }

    public int getProxyHttpPort() {
        return proxyHttpPort;
    }

    public String getProxyHttpsHost() {
        return proxyHttpsHost;
    }

    public int getProxyHttpsPort() {
        return proxyHttpsPort;
    }

    public String getProxyFtpHost() {
        return proxyFtpHost;
    }

    public int getProxyFtpPort() {
        return proxyFtpPort;
    }

    public String getProxySocks4Host() {
        return proxySocks4Host;
    }

    public int getProxySocks4Port() {
        return proxySocks4Port;
    }
}
