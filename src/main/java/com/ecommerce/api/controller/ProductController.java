package com.ecommerce.api.controller;

import com.ecommerce.api.dto.common.PageResponse;
import com.ecommerce.api.dto.product.ProductRequest;
import com.ecommerce.api.dto.product.ProductResponse;
import com.ecommerce.api.service.FileStorageService;
import com.ecommerce.api.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final com.ecommerce.api.service.StockAlertSubscriptionService stockAlertSubscriptionService;

    @GetMapping
    @Operation(summary = "Search & list products with pagination (public)")
    public PageResponse<ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(required = false) String sort
    ) {
        return productService.search(q, categoryId, minPrice, maxPrice, inStock, page, size, sort);
    }

    @GetMapping("/featured")
    @Operation(summary = "Featured products for homepage")
    public List<ProductResponse> featured(@RequestParam(defaultValue = "8") @Min(1) @Max(24) int size) {
        return productService.getFeatured(size);
    }

    @GetMapping("/bestsellers")
    @Operation(summary = "Best-selling products by review count")
    public List<ProductResponse> bestsellers(@RequestParam(defaultValue = "8") @Min(1) @Max(24) int size) {
        return productService.getBestsellers(size);
    }

    @PostMapping("/{id}/stock-alerts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Notify when product is back in stock")
    public void stockAlert(@PathVariable Long id) {
        stockAlertSubscriptionService.subscribe(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by id (public)")
    public ProductResponse findById(@PathVariable Long id) {
        return productService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product (admin)")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product (admin)")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete product (admin)")
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload primary product image (admin)")
    public ProductResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProductImage(id, file);
        return productService.updateImage(id, url);
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add gallery image (admin)")
    public ProductResponse uploadGalleryImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeProductImage(id, file);
        return productService.addGalleryImage(id, url);
    }
}
