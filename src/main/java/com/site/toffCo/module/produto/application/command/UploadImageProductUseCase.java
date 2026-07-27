package com.site.toffCo.module.produto.application.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@Slf4j
public class UploadImageProductUseCase {

    @Value("${app.upload-dir:/app/upload}")
    private String uploadDir;

    public String uploadImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Imagem vazia");
        }

        String contentType = image.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Arquivo enviado nao e uma imagem");
        }

        try {
            Path directory = Paths.get(uploadDir);
            Files.createDirectories(directory);

            String extension = switch (contentType) {
                case "image/png" -> ".png";
                case "image/jpeg" -> ".jpg";
                case "image/webp" -> ".webp";
                default -> throw new IllegalArgumentException("Formato de imagem invalido");
            };

            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename).normalize();

            image.transferTo(destination);

            return "/uploads/" + filename;
        } catch (IOException exception) {
            throw new RuntimeException("Nao foi possivel salvar a imagem", exception);
        }
    }
}
