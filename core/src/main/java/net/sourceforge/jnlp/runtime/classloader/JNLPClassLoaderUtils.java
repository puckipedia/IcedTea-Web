package net.sourceforge.jnlp.runtime.classloader;

import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.resource.ResourcesDesc;
import net.adoptopenjdk.icedteaweb.jnlp.version.VersionString;
import net.adoptopenjdk.icedteaweb.resources.ResourceTracker;
import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.util.JarFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;

public class JNLPClassLoaderUtils {

    /**
     * Returns the JNLPClassLoader associated with the given ClassLoader, or
     * null.
     * @param cl a ClassLoader
     * @return JNLPClassLoader or null
     */
    public static AbstractJNLPClassLoader getJnlpClassLoader(final ClassLoader cl) {
        // Since we want to deal with JNLPClassLoader, extract it if this
        // is a codebase loader
        if (cl instanceof CodeBaseClassLoader) {
            return ((CodeBaseClassLoader) cl).getParentJNLPClassLoader();
        }

        if (cl instanceof AbstractJNLPClassLoader) {
            JNLPClassLoader loader = (JNLPClassLoader) cl;
            return loader;
        }

        return null;
    }

    /**
     * Returns jars from the JNLP file with the part name provided.
     * @param rootClassLoader Root JNLPClassLoader of the application.
     * @param ref Path of the launch or extension JNLP File containing the
     * resource. If null, main JNLP's file location will be used instead.
     * @param part The name of the part.
     * @param version version of jar
     * @return jars found.
     */
    public static JARDesc[] findJars(final AbstractJNLPClassLoader rootClassLoader, final URL ref, final String part, final VersionString version) {
        final AbstractJNLPClassLoader foundLoader = getLoaderByJnlpFile(rootClassLoader, ref);

        if (foundLoader != null) {
            final List<JARDesc> foundJars = new ArrayList<>();
            final ResourcesDesc resources = foundLoader.getApplication().getJNLPFile().getResources();

            for (final JARDesc aJar : resources.getJARs(part)) {
                if (Objects.equals(version, aJar.getVersion()))
                    foundJars.add(aJar);
            }

            return foundJars.toArray(new JARDesc[foundJars.size()]);
        }

        return new JARDesc[] {};
    }

    /**
     * Removes jars from cache.
     * @param classLoader JNLPClassLoader of the application that is associated to the resource.
     * @param ref Path of the launch or extension JNLP File containing the
     * resource. If null, main JNLP's file location will be used instead.
     * @param jars Jars marked for removal.
     */
    public static void removeCachedJars(final AbstractJNLPClassLoader classLoader, final URL ref, final JARDesc[] jars) {
        AbstractJNLPClassLoader foundLoader = getLoaderByJnlpFile(classLoader, ref);

        if (foundLoader != null)
            foundLoader.removeJars(jars);
    }

    /**
     * Downloads jars identified by part name.
     * @param classLoader JNLPClassLoader of the application that is associated to the resource.
     * @param ref Path of the launch or extension JNLP File containing the
     * resource. If null, main JNLP's file location will be used instead.
     * @param part The name of the path.
     * @param version version of jar to be downloaded
     */
    public static void downloadJars(final AbstractJNLPClassLoader classLoader, final URL ref, final String part, final VersionString version) {
        final AbstractJNLPClassLoader foundLoader = getLoaderByJnlpFile(classLoader, ref);

        if (foundLoader != null)
            foundLoader.initializeNewJarDownload(ref, part, version);
    }

    /**
     * Downloads and initializes resources which are not mentioned in the jnlp file.
     * Used by DownloadService.
     * @param rootClassLoader Root JNLPClassLoader of the application.
     * @param ref Path to the resource.
     * @param version The version of resource. If null, no version is specified.
     */

    public static void loadExternalResourceToCache(final AbstractJNLPClassLoader rootClassLoader, final URL ref, final String version) {
        rootClassLoader.manageExternalJars(ref, version, DownloadAction.DOWNLOAD_TO_CACHE);
    }

    /**
     * Removes resource which are not mentioned in the jnlp file.
     * Used by DownloadService.
     * @param rootClassLoader Root JNLPClassLoader of the application.
     * @param ref Path to the resource.
     * @param version The version of resource. If null, no version is specified.
     */
    public static void removeExternalCachedResource(final AbstractJNLPClassLoader rootClassLoader, final URL ref, final String version) {
        rootClassLoader.manageExternalJars(ref, version, DownloadAction.REMOVE_FROM_CACHE);
    }

    /**
     * Returns {@code true} if the resource (not mentioned in the jnlp file) is cached, otherwise {@code false}
     * Used by DownloadService.
     * @param rootClassLoader Root {@link JNLPClassLoader} of the application.
     * @param ref Path to the resource.
     * @param version The version of resource. If {@code null}, no version is specified.
     * @return {@code true} if the external resource is cached, otherwise {@code false}
     */
    public static boolean isExternalResourceCached(final AbstractJNLPClassLoader rootClassLoader, final URL ref, final String version) {
        return rootClassLoader.manageExternalJars(ref, version, DownloadAction.CHECK_CACHE);
    }

    public static String getMainClassNameFromManifest(final ResourceTracker tracker, final JARDesc mainJarDesc) throws IOException {
        final File f = tracker.getCacheFile(mainJarDesc.getLocation());
        if (f != null) {
            final JarFile mainJar = new JarFile(f);
            return mainJar.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        }
        return null;
    }

    /**
     * Locates the JNLPClassLoader of the JNLP file.
     * @param rootClassLoader Root JNLPClassLoader of the application.
     * @param urlToJnlpFile Path of the JNLP file. If {@code null}, main JNLP file's location
     * be used instead
     * @return the JNLPClassLoader of the JNLP file.
     */
    static AbstractJNLPClassLoader getLoaderByJnlpFile(final AbstractJNLPClassLoader rootClassLoader, URL urlToJnlpFile) {

        if (rootClassLoader == null)
            return null;

        JNLPFile file = rootClassLoader.getApplication().getJNLPFile();

        if (urlToJnlpFile == null)
            urlToJnlpFile = rootClassLoader.getApplication().getJNLPFile().getFileLocation();

        if (file.getFileLocation().equals(urlToJnlpFile))
            return rootClassLoader;

        for (AbstractJNLPClassLoader loader : rootClassLoader.getLoaders()) {
            if (rootClassLoader != loader) {
                AbstractJNLPClassLoader foundLoader = getLoaderByJnlpFile(loader, urlToJnlpFile);
                if (foundLoader != null)
                    return foundLoader;
            }
        }

        return null;
    }

    /**
     * Locates the JNLPClassLoader of the JNLP file's resource.
     * @param rootClassLoader Root JNLPClassLoader of the application.
     * @param ref Path of the launch or extension JNLP File. If {@code null},
     * main JNLP file's location will be used instead.
     * @param version The version of resource. Is null if no version is specified
     * @return the JNLPClassLoader of the JNLP file's resource.
     */
    static JNLPClassLoader getLoaderByResourceUrl(final JNLPClassLoader rootClassLoader, final URL ref, final String version) {
        VersionString resourceVersion = (version == null) ? null : VersionString.fromString(version);

        for (JNLPClassLoader loader : rootClassLoader.getLoaders()) {
            ResourcesDesc resources = loader.getApplication().getJNLPFile().getResources();

            for (JARDesc eachJar : resources.getJARs()) {
                if (ref.equals(eachJar.getLocation()) &&
                        (resourceVersion == null || resourceVersion.equals(eachJar.getVersion())))
                    return loader;
            }
        }

        for (JNLPClassLoader loader : rootClassLoader.getLoaders()) {
            if (rootClassLoader != loader) {
                JNLPClassLoader foundLoader = getLoaderByResourceUrl(loader, ref, version);

                if (foundLoader != null)
                    return foundLoader;
            }
        }

        return null;
    }
}
