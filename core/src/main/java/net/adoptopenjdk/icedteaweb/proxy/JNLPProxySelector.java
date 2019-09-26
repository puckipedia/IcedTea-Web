// Copyright (C) 2010 Red Hat, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

package net.adoptopenjdk.icedteaweb.proxy;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.IcedTeaWebConstants;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.sourceforge.jnlp.config.DeploymentConfiguration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A ProxySelector specific to JNLPs. This proxy uses the deployment
 * configuration to determine what to do.
 *
 * @see java.net.ProxySelector
 */
public abstract class JNLPProxySelector extends ProxySelector {

    private final static Logger LOG = LoggerFactory.getLogger(JNLPProxySelector.class);

    private final JNLPProxyConfig proxyConfig;

    public JNLPProxySelector(DeploymentConfiguration config) {
        proxyConfig = new JNLPProxyConfig(config);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOG.error("Proxy: connection failed", ioe);
    }

    @Override
    public List<Proxy> select(final URI uri) {
        LOG.debug("Selecting proxy for: {}", uri);
        if (shouldBypass(uri)) {
            return Collections.singletonList(Proxy.NO_PROXY);
        } else if (Objects.equals(proxyConfig.getProxyType(), ProxyType.PROXY_TYPE_MANUAL)) {
            return Collections.unmodifiableList(getFromConfiguration(uri));
        } else if (Objects.equals(proxyConfig.getProxyType(), ProxyType.PROXY_TYPE_AUTO)) {
            return Collections.unmodifiableList(getFromPAC(uri));
        } else if (Objects.equals(proxyConfig.getProxyType(), ProxyType.PROXY_TYPE_BROWSER)) {
            return Collections.unmodifiableList(getFromBrowser(uri));
        }
        return Collections.singletonList(Proxy.NO_PROXY);
    }

    /**
     * Returns true if the uri should be bypassed for proxy purposes
     */
    private boolean shouldBypass(final URI uri) {
        Assert.requireNonNull(uri, "uri");
        try {
            final String scheme = uri.getScheme();
            if (Objects.equals(scheme, ProxyConstants.HTTP_SCHEME) ||
                    Objects.equals(scheme, ProxyConstants.HTTP_SCHEME) ||
                    Objects.equals(scheme, ProxyConstants.HTTP_SCHEME)) {
                final URL url = uri.toURL();
                if (proxyConfig.isBypassLocal() && ProxyUtils.isLocalHost(url.getHost())) {
                    return true;
                }
                if (proxyConfig.getBypassList().contains(url.getHost())) {
                    return true;
                }
            } else if (Objects.equals(scheme, ProxyConstants.SOCKET_SCHEME)) {
                final String host = uri.getHost();
                if (proxyConfig.isBypassLocal() && ProxyUtils.isLocalHost(host)) {
                    return true;
                }
                if (proxyConfig.getBypassList().contains(host)) {
                    return true;
                }
            } else {
                LOG.warn("Unsupported scheme for proxy: {}", scheme);
            }
        } catch (final MalformedURLException e) {
            LOG.error("Can not check uri for proxy bypass", e);
        }
        return false;
    }

    /**
     * Returns a list of proxies by using the information in the deployment
     * configuration
     *
     * @param uri uri to read
     * @return a List of Proxy objects
     */
    private List<Proxy> getFromConfiguration(final URI uri) {
        return ProxyUtils.getFromArguments(uri, proxyConfig.isSameProxy(), false,
                proxyConfig.getProxyHttpsHost(), proxyConfig.getProxyHttpsPort(),
                proxyConfig.getProxyHttpHost(), proxyConfig.getProxyHttpPort(),
                proxyConfig.getProxyFtpHost(), proxyConfig.getProxyFtpPort(),
                proxyConfig.getProxySocks4Host(), proxyConfig.getProxySocks4Port());
    }

    /**
     * Returns a list of proxies by using the Proxy Auto Config (PAC) file. See
     * http://en.wikipedia.org/wiki/Proxy_auto-config#The_PAC_file for more
     * information.
     *
     * @param uri uri to PAC
     * @return a List of valid Proxy objects
     */
    private List<Proxy> getFromPAC(final URI uri) {
        Assert.requireNonNull(uri, "uri");
        if (proxyConfig.getAutoConfigUrl() == null || uri.getScheme().equals("socket")) {
            return Arrays.asList(new Proxy[]{Proxy.NO_PROXY});
        }

        final List<Proxy> proxies = new ArrayList<>();

        try {
            String proxiesString = proxyConfig.getPacEvaluator().getProxies(uri.toURL());
            proxies.addAll(ProxyUtils.getProxiesFromPacResult(proxiesString));
        } catch (MalformedURLException e) {
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, e);
            proxies.add(Proxy.NO_PROXY);
        }

        return Collections.unmodifiableList(proxies);
    }

    /**
     * Returns a list of proxies by querying the browser
     *
     * @param uri the uri to get proxies for
     * @return a list of proxies
     */
    protected abstract List<Proxy> getFromBrowser(URI uri);


}
