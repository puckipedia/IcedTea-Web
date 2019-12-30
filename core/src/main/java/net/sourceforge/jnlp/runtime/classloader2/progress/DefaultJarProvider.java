package net.sourceforge.jnlp.runtime.classloader2.progress;

import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.sourceforge.jnlp.runtime.classloader2.ClassLoaderUtils;

import javax.jnlp.DownloadServiceListener;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class DefaultJarProvider implements JarProvider {

    private static final Executor BACKGROUND_EXECUTOR = Executors.newCachedThreadPool();

    private final DownloadServiceListener listener;

    public DefaultJarProvider(final DownloadServiceListener listener) {
        this.listener = listener;
    }

    @Override
    public CompletableFuture<List<URL>> provideLocalUrl(final List<JARDesc> jarDesc) {
        final List<CompletableFuture<URL>> futures = jarDesc.stream()
                .map(this::handle)
                .collect(Collectors.toList());

        final CompletableFuture<List<URL>> result = new CompletableFuture<>();
        BACKGROUND_EXECUTOR.execute(() -> {
            try {
                final List<URL> urls = futures.stream()
                        .map(future -> ClassLoaderUtils.waitForCompletion(future, "Error while handling task"))
                        .collect(Collectors.toList());
                result.complete(urls);
            } catch (final Exception e) {
                result.completeExceptionally(e);
            } finally {
                if(listener instanceof ExtendedDownloadServiceListener) {
                    ((ExtendedDownloadServiceListener)listener).done();
                }
            }
        });
        return result;
    }

    private CompletableFuture<URL> handle(final JARDesc jarDesc) {
        final CompletableFuture<URL> result = new CompletableFuture<>();
        BACKGROUND_EXECUTOR.execute(() -> {
            try {
                final File localFile = downloadResource(jarDesc);
                validateResource(jarDesc, localFile);
                final URL url = localFile.toURI().toURL();
                result.complete(url);
            } catch (final Exception e) {
                result.completeExceptionally(e);
                onError(jarDesc);
            }
        });
        return result;
    }

    private void onError(final JARDesc jarDesc) {
        try {
            listener.downloadFailed(jarDesc.getLocation(), jarDesc.getVersion().toString());
        } catch (final Exception ignore) {}
    }

    private File downloadResource(final JARDesc jarDesc) {
        final URL location = jarDesc.getLocation();
        final String version = jarDesc.getVersion().toString();
        ProgressTask progressTask = null;
        progressTask.addBiConsumer((current, total) -> listener.progress(location, version, current, total, -1));
        progressTask.waitFor();
        return null;
    }

    private void validateResource(final JARDesc jarDesc, final File file) {
        final URL location = jarDesc.getLocation();
        final String version = jarDesc.getVersion().toString();
        ProgressTask progressTask = null;
        progressTask.addBiConsumer((current, total) -> listener.validating(location, version, current, total, -1));
        progressTask.waitFor();
    }
}
