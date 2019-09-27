/* BrowserAwareProxySelector.java
   Copyright (C) 2011 Red Hat, Inc.

This file is part of IcedTea.

IcedTea is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License as published by
the Free Software Foundation, version 2.

IcedTea is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with IcedTea; see the file COPYING.  If not, write to
the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301 USA.

Linking this library statically or dynamically with other modules is
making a combined work based on this library.  Thus, the terms and
conditions of the GNU General Public License cover the whole
combination.

As a special exception, the copyright holders of this library give you
permission to link this library with independent modules to produce an
executable, regardless of the license terms of these independent
modules, and to copy and distribute the resulting executable under
terms of your choice, provided that you also meet, for each linked
independent module, the terms and conditions of the license of that
module.  An independent module is a module which is not derived from
or based on this library.  If you modify this library, you may extend
this exception to your version of the library, but you are not
obligated to do so.  If you do not wish to do so, delete this
exception statement from your version.
*/
package net.adoptopenjdk.icedteaweb.proxy.browser;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.IcedTeaWebConstants;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.adoptopenjdk.icedteaweb.proxy.JNLPProxySelector;
import net.adoptopenjdk.icedteaweb.proxy.ProxyConstants;
import net.adoptopenjdk.icedteaweb.proxy.ProxyUtils;
import net.sourceforge.jnlp.config.DeploymentConfiguration;
import net.sourceforge.jnlp.runtime.PacEvaluator;
import net.sourceforge.jnlp.runtime.PacEvaluatorFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.AUTOCONFIG_URL_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_FTP_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_FTP_PORT_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_HTTPS_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_HTTPS_PORT_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_HTTP_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_HTTP_PORT_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_SOCKS_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_SOCKS_PORT_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.PROXY_TYPE_NAME;
import static net.adoptopenjdk.icedteaweb.proxy.browser.FirefoxPreferencesConstants.SHARE_PROXY_SETTINGS_NAME;

/**
 * A ProxySelector which can read proxy settings from a browser's
 * configuration and use that.
 *
 * @see JNLPProxySelector
 */
public class BrowserAwareProxySelector extends JNLPProxySelector {

    private final static Logger LOG = LoggerFactory.getLogger(BrowserAwareProxySelector.class);

    /* firefox's constants */
    public static final int BROWSER_PROXY_TYPE_NONE = 0;

    public static final int BROWSER_PROXY_TYPE_MANUAL = 1;

    public static final int BROWSER_PROXY_TYPE_PAC = 2;

    /** use gconf, WPAD and then env (and possibly others)*/
    public static final int BROWSER_PROXY_TYPE_AUTO = 4;

    /** use env variables */
    public static final int BROWSER_PROXY_TYPE_SYSTEM = 5;

    private int browserProxyType;

    private URL browserAutoConfigUrl;

    /** Whether the http proxy should be used for http, https, ftp and socket protocols */
    private Boolean browserUseSameProxy;
    private String browserHttpProxyHost;
    private int browserHttpProxyPort;
    private String browserHttpsProxyHost;
    private int browserHttpsProxyPort;
    private String browserFtpProxyHost;
    private int browserFtpProxyPort;
    private String browserSocks4ProxyHost;
    private int browserSocks4ProxyPort;

    private PacEvaluator browserProxyAutoConfig = null;


    /**
     * Create a new instance of this class, reading configuration from the browser
     */
    public BrowserAwareProxySelector(DeploymentConfiguration config) {
        super(config);

        try {
            Map<String, String> prefs = FirefoxPreferencesUtil.getFirefoxPreferences();

            String type = prefs.get(PROXY_TYPE_NAME);
            if (type != null) {
                browserProxyType = Integer.valueOf(type);
            } else {
                browserProxyType = BROWSER_PROXY_TYPE_AUTO;
            }

            try {
                String url = prefs.get(AUTOCONFIG_URL_NAME);
                if (url != null) {
                    browserAutoConfigUrl = new URL(url);
                }
            } catch (MalformedURLException e) {
                LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, e);
            }

            if (browserProxyType == BROWSER_PROXY_TYPE_PAC) {
                if (browserAutoConfigUrl != null) {
                    browserProxyAutoConfig = PacEvaluatorFactory.getPacEvaluator(browserAutoConfigUrl);
                }
            }

            browserUseSameProxy = Boolean.valueOf(prefs.get(SHARE_PROXY_SETTINGS_NAME));

            browserHttpProxyHost = prefs.get(PROXY_HTTP_NAME);
            browserHttpProxyPort = stringToPort(prefs.get(PROXY_HTTP_PORT_NAME));
            browserHttpsProxyHost = prefs.get(PROXY_HTTPS_NAME);
            browserHttpsProxyPort = stringToPort(prefs.get(PROXY_HTTPS_PORT_NAME));
            browserFtpProxyHost = prefs.get(PROXY_FTP_NAME);
            browserFtpProxyPort = stringToPort(prefs.get(PROXY_FTP_PORT_NAME));
            browserSocks4ProxyHost = prefs.get(PROXY_SOCKS_NAME);
            browserSocks4ProxyPort = stringToPort(prefs.get(PROXY_SOCKS_PORT_NAME));
        } catch (final IOException e) {
            LOG.error("Unable to use Firefox's proxy settings. Using 'DIRECT' as proxy type.", e);
            browserProxyType = BROWSER_PROXY_TYPE_NONE;

        }
    }

    /**
     * Returns port inside a string. Unlike {@link Integer#valueOf(String)},
     * it will not throw exceptions.
     *
     * @param string the string containing the integer to parse
     * @return the port inside the string, or Integer.MIN_VALUE
     */
    private int stringToPort(final String string) {
        try {
            return Integer.valueOf(string);
        } catch (final NumberFormatException nfe) {
            //TODO: Does this really makes sense???
            return Integer.MIN_VALUE;
        }
    }

    /**
     * <p>
     * The main entry point for {@link BrowserAwareProxySelector}. Based on
     * the browser settings, determines proxy information for a given URI.
     * </p>
     * <p>
     * The appropriate proxy may be determined by reading static information
     * from the browser's preferences file, or it may be computed dynamically,
     * by, for example, running javascript code.
     * </p>
     */
    @Override
    public List<Proxy> getFromBrowser(URI uri) {
        List<Proxy> proxies = new ArrayList<Proxy>();

        String optionDescription = null;

        switch (browserProxyType) {
            case BROWSER_PROXY_TYPE_PAC:
                proxies.addAll(getFromBrowserPAC(uri));
                break;
            case BROWSER_PROXY_TYPE_MANUAL:
                proxies.addAll(getFromBrowserConfiguration(uri));
                break;
            case BROWSER_PROXY_TYPE_NONE:
                proxies.add(Proxy.NO_PROXY);
                break;
            case BROWSER_PROXY_TYPE_AUTO:
                // firefox will do a whole lot of stuff to automagically
                // figure out the right settings. gconf, WPAD, and ENV are used.
                // https://bugzilla.mozilla.org/show_bug.cgi?id=66057#c32
                // TODO this is probably not easy/quick to do. using libproxy might be
                // the simpler workaround
                if (optionDescription == null) {
                    optionDescription = "Automatic";
                }
            case BROWSER_PROXY_TYPE_SYSTEM:
                // means use $http_proxy, $ftp_proxy etc.
                // TODO implement env vars if possible
                if (optionDescription == null) {
                    optionDescription = "System";
                }
            default:
                if (optionDescription == null) {
                    optionDescription = "Unknown";
                }
                LOG.debug("Browser proxy option \"{}\" ({}) not supported yet.", browserProxyType, optionDescription);
                proxies.add(Proxy.NO_PROXY);
        }

        LOG.info("Browser selected proxies: {}", proxies);

        return proxies;
    }

    /**
     * Get an appropriate proxy for a given URI using a PAC specified in the
     * browser.
     */
    private List<Proxy> getFromBrowserPAC(final URI uri) {
        Assert.requireNonNull(uri, "uri");
        if (browserAutoConfigUrl == null || uri.getScheme().equals(ProxyConstants.SOCKET_SCHEME)) {
            return Arrays.asList(new Proxy[] { Proxy.NO_PROXY });
        }

        List<Proxy> proxies = new ArrayList<>();

        try {
            String proxiesString = browserProxyAutoConfig.getProxies(uri.toURL());
            proxies.addAll(ProxyUtils.getProxiesFromPacResult(proxiesString));
        } catch (MalformedURLException e) {
            LOG.error(IcedTeaWebConstants.DEFAULT_ERROR_MESSAGE, e);
            proxies.add(Proxy.NO_PROXY);
        }

        return proxies;
    }

    /**
     * Get an appropriate proxy for the given URI using static information from
     * the browser's preferences file.
     */
    private List<Proxy> getFromBrowserConfiguration(URI uri) {
        return ProxyUtils.getFromArguments(uri, browserUseSameProxy, true,
                browserHttpsProxyHost, browserHttpsProxyPort,
                browserHttpProxyHost, browserHttpProxyPort,
                browserFtpProxyHost, browserFtpProxyPort,
                browserSocks4ProxyHost, browserSocks4ProxyPort);
    }

}
