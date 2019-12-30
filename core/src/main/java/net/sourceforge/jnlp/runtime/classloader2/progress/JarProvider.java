package net.sourceforge.jnlp.runtime.classloader2.progress;

import net.adoptopenjdk.icedteaweb.Assert;
import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface JarProvider {

    @Deprecated
    default CompletableFuture<URL> provideLocalUrl(final JARDesc jarDesc) {
        return provideLocalUrl(Collections.singletonList(jarDesc)).thenApply(list -> {
            Assert.requireNonNull(list, "list");
            if(list.size() != 1) {
                throw new IllegalStateException("1 JARDesc should only end in 1 URL!");
            }
           return list.get(0);
        });
    }

    CompletableFuture<List<URL>> provideLocalUrl(final List<JARDesc> jarDesc);
}
