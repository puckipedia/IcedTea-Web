package net.sourceforge.jnlp.runtime.classloader2;

import net.adoptopenjdk.icedteaweb.jnlp.element.resource.JARDesc;
import net.adoptopenjdk.icedteaweb.jnlp.element.resource.PackageDesc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * ...
 */
public class Part {

    private final String name;

    private boolean lazy = true;

    private final Extension extension;

    private boolean downloaded;

    private final List<JARDesc> jars = new ArrayList<>();

    private final List<PackageDesc> packages = new ArrayList<>();

    private final List<Part> extParts = new ArrayList<>();

    Part(final String name) {
        this(null, name);
    }

    Part(final Extension extension, final String name) {
        this.extension = extension;
        this.name = name;
    }

    public void addExtPart(final Part extPart) {
        extParts.add(extPart);
    }

    void addJar(final JARDesc jarDescription) {
        if (!jarDescription.isLazy()) {
            markAsEager();
        }

        jars.add(jarDescription);
    }

    void addPackage(final PackageDesc packageDef) {
        packages.add(packageDef);
    }

    void markAsEager() {
        lazy = false;
    }

    public List<JARDesc> getJars() {
        final List<JARDesc> result = new ArrayList<>(jars);
        extParts.forEach(p -> result.addAll(p.getJars()));
        return Collections.unmodifiableList(result);
    }

    public List<PackageDesc> getPackages() {
        return Collections.unmodifiableList(packages);
    }

    public boolean supports(final String resourceName) {
        return packages.stream().anyMatch(partPackage -> partPackage.matches(resourceName));
    }

    public String getName() {
        return name;
    }

    public boolean isLazy() {
        return lazy;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(final boolean downloaded) {
        this.downloaded = downloaded;
        extParts.forEach(p -> p.setDownloaded(downloaded));
    }

    public Extension getExtension() {
        return extension;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Part part = (Part) o;
        return Objects.equals(name, part.name) &&
                Objects.equals(extension, part.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, extension);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Part.class.getSimpleName() + "{", "}")
                .add("name='" + name + "'")
                .add("lazy=" + lazy)
                .add("jars=" + jars)
                .add("packages=" + packages)
                .toString();
    }

}
