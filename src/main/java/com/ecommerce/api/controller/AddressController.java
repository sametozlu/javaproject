package com.ecommerce.api.controller;

import com.ecommerce.api.dto.address.AddressRequest;
import com.ecommerce.api.dto.address.AddressResponse;
import com.ecommerce.api.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List my delivery addresses")
    public List<AddressResponse> list() {
        return addressService.getMyAddresses();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add delivery address")
    public AddressResponse create(@Valid @RequestBody AddressRequest request) {
        return addressService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update delivery address")
    public AddressResponse update(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        return addressService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete delivery address")
    public void delete(@PathVariable Long id) {
        addressService.delete(id);
    }
}
