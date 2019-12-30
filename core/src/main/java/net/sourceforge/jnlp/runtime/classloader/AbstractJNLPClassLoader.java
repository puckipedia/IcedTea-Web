package net.sourceforge.jnlp.runtime.classloader;

import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.security.SecurityDesc;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionString;
import net.adoptopenjdk.icedteaweb.resources.ResourceTracker;
import net.sourceforge.jnlp.runtime.ApplicationInstance;

import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;

public abstract class AbstractJNLPClassLoader extends URLClassLoader {

    protected AbstractJNLPClassLoader(final URL[] urls, final ClassLoader parent) {
        super(urls, parent);
    }

    public abstract ResourceTracker getTracker();

    public abstract SecurityDesc getSecurity();

    public abstract PermissionCollection getPermissions(CodeSource codesource);

    public abstract void addPermission(final Permission perm);

    public abstract ApplicationInstance getApplication();

    public abstract void removeExtensionPart(final URL ref, final String version, final String part);

    public abstract void removePart(final String part);

    public abstract boolean isFullSigned();





    abstract AbstractJNLPClassLoader[] getLoaders();

    abstract boolean manageExternalJars(final URL ref, final String version, final DownloadAction downloadToCache);

    abstract void initializeNewJarDownload(final URL ref, final String part, final VersionString version);

    abstract void removeJars(final JARDesc[] jars);
}
