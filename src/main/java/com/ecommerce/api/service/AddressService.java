package com.ecommerce.api.service;

import com.ecommerce.api.domain.Address;
import com.ecommerce.api.domain.User;
import com.ecommerce.api.dto.address.AddressRequest;
import com.ecommerce.api.dto.address.AddressResponse;
import com.ecommerce.api.exception.ResourceNotFoundException;
import com.ecommerce.api.repository.AddressRepository;
import com.ecommerce.api.repository.UserRepository;
import com.ecommerce.api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        Long userId = SecurityUtils.currentUser().getId();
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AddressResponse create(AddressRequest request) {
        User user = currentUser();
        if (request.defaultAddress()) {
            clearDefault(user.getId());
        }
        Address address = Address.builder()
                .user(user)
                .title(request.title())
                .fullName(request.fullName())
                .phone(request.phone())
                .city(request.city())
                .district(request.district())
                .addressLine(request.addressLine())
                .postalCode(request.postalCode())
                .defaultAddress(request.defaultAddress())
                .build();
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Long id, AddressRequest request) {
        Address address = getOwnedAddress(id);
        if (request.defaultAddress() && !address.isDefaultAddress()) {
            clearDefault(address.getUser().getId());
        }
        address.setTitle(request.title());
        address.setFullName(request.fullName());
        address.setPhone(request.phone());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setAddressLine(request.addressLine());
        address.setPostalCode(request.postalCode());
        address.setDefaultAddress(request.defaultAddress());
        return toResponse(address);
    }

    @Transactional
    public void delete(Long id) {
        Address address = getOwnedAddress(id);
        addressRepository.delete(address);
    }

    @Transactional(readOnly = true)
    public Address getOwnedAddressEntity(Long id) {
        return getOwnedAddress(id);
    }

    private Address getOwnedAddress(Long id) {
        Long userId = SecurityUtils.currentUser().getId();
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private void clearDefault(Long userId) {
        addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId).forEach(a -> {
            if (a.isDefaultAddress()) {
                a.setDefaultAddress(false);
            }
        });
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getTitle(),
                address.getFullName(),
                address.getPhone(),
                address.getCity(),
                address.getDistrict(),
                address.getAddressLine(),
                address.getPostalCode(),
                address.isDefaultAddress(),
                address.getCreatedAt()
        );
    }
}
