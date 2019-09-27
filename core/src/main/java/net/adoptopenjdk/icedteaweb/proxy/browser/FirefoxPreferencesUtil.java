package net.adoptopenjdk.icedteaweb.proxy.browser;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.JavaSystemProperties;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.adoptopenjdk.icedteaweb.os.OsUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FirefoxPreferencesUtil {

    private final static Logger LOG = LoggerFactory.getLogger(FirefoxPreferencesUtil.class);

    public static Map<String, String> getFirefoxPreferences() throws IOException {
        return parseFirefoxPreferences(findFirefoxPreferences());
    }

    private static String getFirefoxConfigPath() {
        if (OsUtil.isWindows()) {
            final Map<String, String> env = System.getenv();
            if (env != null) {
                final String appdata = env.get("APPDATA");
                if (appdata != null) {
                    return appdata + File.separator + "Mozilla"
                            + File.separator + "Firefox" + File.separator;
                }
            }
        }
        return JavaSystemProperties.getUserHome() + File.separator + ".mozilla"
                + File.separator + "firefox" + File.separator;
    }

    private static File findFirefoxPreferences() throws IOException {

        final String configPath = getFirefoxConfigPath();
        final String profilesPath = configPath + "profiles.ini";

        if (!(new File(profilesPath).isFile())) {
            throw new FileNotFoundException(profilesPath);
        }
        LOG.info("Using firefox's profiles file: {}", profilesPath);

        final BufferedReader reader = new BufferedReader(new FileReader(profilesPath));

        List<String> linesInSection = new ArrayList<>();
        boolean foundDefaultSection = false;

        /*
         * The profiles.ini file is an ini file. This is a quick hack to read
         * it. It is very likely to break given anything strange.
         */

        // find the section with an entry Default=1
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.startsWith("[Profile") && line.endsWith("]")) {
                    if (foundDefaultSection) {
                        break;
                    }
                    // new section
                    linesInSection = new ArrayList<>();
                } else {
                    linesInSection.add(line);
                    int equalSignPos = line.indexOf('=');
                    if (equalSignPos > 0) {
                        final String key = line.substring(0, equalSignPos).trim();
                        final String value = line.substring(equalSignPos+1).trim();
                        if (key.toLowerCase().equals("default") && value.equals("1")) {
                            foundDefaultSection = true;
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }

        if (!foundDefaultSection && linesInSection.size() == 0) {
            throw new FileNotFoundException("preferences file");
        }

        String path = null;
        for (String line : linesInSection) {
            if (line.startsWith("Path=")) {
                path = line.substring("Path=".length());
            }
        }

        if (path == null) {
            throw new FileNotFoundException("preferences file");
        } else {
            final String fullPath = configPath + path + File.separator + "prefs.js";
            LOG.info("Found preferences file: ", fullPath);
            return new File(fullPath);
        }
    }

    private static Map<String, String> parseFirefoxPreferences(final File firefoxPreferencesFile) throws IOException {
        Assert.requireNonNull(firefoxPreferencesFile, "firefoxPreferencesFile");
        /*
         * The Firefox preference file is actually in javascript. It does seem
         * to be nicely formatted, so it should be possible to hack reading it.
         * The correct way of course is to use a javascript library and extract
         * the user_pref object
         */
        final Map<String, String> prefs = new HashMap<>();

        try (final BufferedReader reader = new BufferedReader(new FileReader(firefoxPreferencesFile))) {
            while (true) {
                String line = reader.readLine();
                // end of stream
                if (line == null) {
                    break;
                }

                line = line.trim();
                if (line.startsWith("user_pref")) {

                    /*
                     * each line is of the form: user_pref("key",value); where value
                     * can be a string in double quotes or an integer or float or
                     * boolean
                     */

                    boolean foundKey = false;

                    // extract everything inside user_pref( and );
                    final String pref = line.substring("user_pref(".length(), line.length() - 2);
                    // key and value are separated by a ,
                    final int firstCommaPos = pref.indexOf(',');
                    if (firstCommaPos >= 1) {
                        String key = pref.substring(0, firstCommaPos).trim();
                        if (key.startsWith("\"") && key.endsWith("\"")) {
                            key = key.substring(1, key.length() - 1);
                            if (key.trim().length() > 0) {
                                foundKey = true;
                            }
                        }

                        if (pref.length() > firstCommaPos + 1) {
                            String value = pref.substring(firstCommaPos + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1).trim();
                            }
                            if (foundKey) {
                                LOG.debug("Adding Firefox property {}, {}", key, value);
                                prefs.put(key, value);
                            }
                        }
                    }
                }
            }
        }
        LOG.info("Read {} entries from Firefox's preferences", prefs.size());
        return Collections.unmodifiableMap(prefs);
    }
}
