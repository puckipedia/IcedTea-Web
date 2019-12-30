package net.sourceforge.jnlp.runtime.classloader2.progress;

import java.net.URL;
import java.util.function.BiConsumer;

public interface ProgressTask {

    void addBiConsumer(BiConsumer<Long, Long> progressListener);

    URL getResourceUrl();

    String getResourceVersion();

    void waitFor();
}
