package com.ecommerce.api.service;

import com.ecommerce.api.domain.Order;
import com.ecommerce.api.domain.OrderStatus;
import com.ecommerce.api.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String storeProductImage(Long productId, MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Only image files allowed");
        }

        try {
            Path dir = Path.of(uploadDir, "products");
            Files.createDirectories(dir);
            String ext = switch (contentType) {
                case "image/png" -> ".png";
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/webp" -> ".webp";
                default -> "";
            };
            String filename = "product-" + productId + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/products/" + filename;
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }
}
