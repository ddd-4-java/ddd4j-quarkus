package io.ddd4j.quarkus.validation;

import io.ddd4j.extension.validation.ValidatableFile;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;

/**
 * 将 Quarkus REST {@link FileUpload} 适配为框架无关的文件校验模型。
 */
public final class QuarkusFileUploadAdapter implements ValidatableFile {

    private final FileUpload fileUpload;

    public QuarkusFileUploadAdapter(FileUpload fileUpload) {
        this.fileUpload = Objects.requireNonNull(fileUpload, "fileUpload must not be null");
    }

    @Override
    public String fileName() {
        return fileUpload.fileName();
    }

    @Override
    public String contentType() {
        return fileUpload.contentType();
    }

    @Override
    public long size() {
        return fileUpload.size();
    }

    @Override
    public InputStream openStream() throws IOException {
        return Files.newInputStream(fileUpload.filePath());
    }
}
