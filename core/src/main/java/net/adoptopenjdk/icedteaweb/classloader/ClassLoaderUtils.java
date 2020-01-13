package net.adoptopenjdk.icedteaweb.classloader;

import net.adoptopenjdk.icedteaweb.jnlp.element.EntryPoint;
import net.adoptopenjdk.icedteaweb.jnlp.element.application.AppletDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.application.ApplicationDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.adoptopenjdk.icedteaweb.manifest.ManifestAttributesReader;
import net.adoptopenjdk.icedteaweb.resources.cache.Cache;
import net.sourceforge.jnlp.JNLPFile;
import net.sourceforge.jnlp.JNLPMatcher;
import net.sourceforge.jnlp.JNLPMatcherException;
import net.sourceforge.jnlp.LaunchException;
import net.sourceforge.jnlp.runtime.classloader.JNLPClassLoader;
import net.sourceforge.jnlp.runtime.classloader.SecurityDelegateImpl;
import net.sourceforge.jnlp.util.JarFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.jar.JarEntry;
import java.util.stream.Collectors;

import static java.util.jar.Attributes.Name.MAIN_CLASS;
import static net.adoptopenjdk.icedteaweb.i18n.Translator.R;
import static net.sourceforge.jnlp.LaunchException.FATAL;
import static net.sourceforge.jnlp.util.UrlUtils.FILE_PROTOCOL;

public class ClassLoaderUtils {

    /**
     * Signed JNLP File and Template
     */
    private static final String TEMPLATE = "JNLP-INF/APPLICATION_TEMPLATE.JNLP";
    private static final String APPLICATION = "JNLP-INF/APPLICATION.JNLP";

    private static final Logger LOG = LoggerFactory.getLogger(ClassLoaderUtils.class);


    public static <V> V waitForCompletion(Future<V> f, String message) {
        try {
            return f.get();
        } catch (final Exception e) {
            throw new RuntimeException(message, e);
        }
    }

    /**
     * *
     * Checks for the jar that contains the main class. If the main class was
     * found, it checks to see if the jar is signed and whether it contains a
     * signed JNLP file
     *
     * @param jars Jars that are checked to see if they contain the main class
     * @throws LaunchException Thrown if the signed JNLP file, within the main
     *                         jar, fails to be verified or does not match
     */
    public static void checkForMain(final String mainClass, final JNLPClassLoader classLoader, final List<JARDesc> jars) throws LaunchException {
        if (mainClass == null) {
            throw new LaunchException("no main class found");
        }

        final String desiredJarEntryName = mainClass + ".class";

        for (JARDesc jar : jars) {

            try {
                final File localFile = classLoader.getTracker().getCacheFile(jar.getLocation());

                if (localFile == null) {
                    LOG.warn("checkForMain JAR {} not found. Continuing.", jar.getLocation());
                    continue; // JAR not found. Keep going.
                }

                try (final JarFile jarFile = new JarFile(localFile)) {
                    for (JarEntry entry : Collections.list(jarFile.entries())) {
                        String jeName = entry.getName().replaceAll("/", ".");
                        if (jeName.equals(desiredJarEntryName)) {
                            classLoader.setFoundMainJar(true);
                            if (classLoader.getCertVerifier().isFullySigned()) {
                                final boolean verified = verifySignedJNLP(classLoader.getJNLPFile(), jarFile);
                                classLoader.setSignedJNLP(verified);
                            }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                /*
                 * After this exception is caught, it is escaped. This will skip
                 * the jarFile that may have thrown this exception and move on
                 * to the next jarFile (if there are any)
                 */
            }
        }
    }

    /**
     * Is called by checkForMain() to check if the jar file is signed and if it
     * contains a signed JNLP file.
     *
     * @param jarFile the jar file
     * @throws LaunchException thrown if the signed JNLP file, within the main
     *                         jar, fails to be verified or does not match
     */
    private static boolean verifySignedJNLP(final JNLPFile file, final JarFile jarFile) throws LaunchException {
        try {
            // NOTE: verification should have happened by now. In other words,
            // calling jcv.verifyJars(desc, tracker) here should have no affect.

            for (JarEntry je : Collections.list(jarFile.entries())) {
                String jeName = je.getName().toUpperCase();

                if (jeName.equals(TEMPLATE) || jeName.equals(APPLICATION)) {
                    LOG.debug("Creating Jar InputStream from JarEntry");
                    InputStream inStream = jarFile.getInputStream(je);
                    LOG.debug("Creating File InputStream from launching JNLP file");
                    File jn;
                    // If the file is on the local file system, use original path, otherwise find cached file
                    if (file.getFileLocation().getProtocol().toLowerCase().equals(FILE_PROTOCOL)) {
                        jn = new File(file.getFileLocation().getPath());
                    } else {
                        jn = Cache.getCacheFile(file.getFileLocation(), file.getFileVersion());
                    }

                    InputStream jnlpStream = new FileInputStream(jn);
                    JNLPMatcher matcher;
                    if (jeName.equals(APPLICATION)) { // If signed application was found
                        LOG.debug("APPLICATION.JNLP has been located within signed JAR. Starting verification...");
                        matcher = new JNLPMatcher(inStream, jnlpStream, false, file.getParserSettings());
                    } else { // Otherwise template was found
                        LOG.debug("APPLICATION_TEMPLATE.JNLP has been located within signed JAR. Starting verification...");
                        matcher = new JNLPMatcher(inStream, jnlpStream, true, file.getParserSettings());
                    }
                    // If signed JNLP file does not matches launching JNLP file, throw JNLPMatcherException
                    if (!matcher.isMatch()) {
                        throw new JNLPMatcherException("Signed Application did not match launching JNLP File");
                    }
                    LOG.debug("Signed Application Verification Successful");

                    return true;
                }
            }
        } catch (JNLPMatcherException e) {

            /*
             * Throws LaunchException if signed JNLP file fails to be verified
             * or fails to match the launching JNLP file
             */
            LaunchException ex = new LaunchException(file, null, FATAL, "Application Error",
                    "The signed JNLP file did not match the launching JNLP file.", R(e.getMessage()));
            SecurityDelegateImpl.consultCertificateSecurityException(ex);
            /*
             * Throwing this exception will fail to initialize the application
             * resulting in the termination of the application
             */

        } catch (Exception e) {
            LOG.error("failed to validate the JNLP file itself", e);

            /*
             * After this exception is caught, it is escaped. If an exception is
             * thrown while handling the jar file, (mainly for
             * JarCertVerifier.add) it assumes the jar file is unsigned and
             * skip the check for a signed JNLP file
             */
        }
        LOG.debug("Ending check for signed JNLP file...");

        return false;
    }

    public static String getMainClass(final JNLPFile file, final JNLPClassLoader classLoader) {
        final String fromEntryPoint = getMainClassFromEntryPoint(file);
        if (fromEntryPoint != null) {
            return fromEntryPoint;
        }

        //TODO: MainClass aus Manifest

        final List<JARDesc> mainJars = file.getJnlpResources().getJARs().stream()
                .filter(JARDesc::isMain)
                .collect(Collectors.toList());
        if (mainJars.size() == 1) {
            final JARDesc jarDesc = mainJars.get(0);
            final String fromManifest = ManifestAttributesReader.getAttributeFromJar(MAIN_CLASS, jarDesc.getLocation(), classLoader.getTracker());
            if (fromManifest != null) {
                return fromManifest;
            }
        } else if (mainJars.size() == 0) {
            final JARDesc jarDesc = file.getJnlpResources().getJARs().get(0);
            final String fromManifest = ManifestAttributesReader.getAttributeFromJar(MAIN_CLASS, jarDesc.getLocation(), classLoader.getTracker());
            if (fromManifest != null) {
                return fromManifest;
            }
        }
        return null;
    }

    private static String getMainClassFromEntryPoint(final JNLPFile file) {
        final EntryPoint entryPoint = file.getEntryPointDesc();
        if (entryPoint instanceof ApplicationDesc || entryPoint instanceof AppletDesc) {
            return entryPoint.getMainClass();
        }
        return null;
    }
}
