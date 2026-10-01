package com.example.emergencywardrobe.service;

import com.example.emergencywardrobe.dto.AddressDto;
import com.example.emergencywardrobe.dto.AddressRequest;
import com.example.emergencywardrobe.entity.Address;
import com.example.emergencywardrobe.entity.User;
import com.example.emergencywardrobe.exception.ResourceNotFoundException;
import com.example.emergencywardrobe.exception.UnauthorizedException;
import com.example.emergencywardrobe.repository.AddressRepository;
import com.example.emergencywardrobe.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(AddressRepository addressRepository, UserRepository userRepository) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    public List<AddressDto> getMyAddresses(String email) {
        User user = getUser(email);
        return addressRepository.findByUserId(user.getId()).stream()
                .map(AddressDto::fromEntity)
                .toList();
    }

    public AddressDto addAddress(String email, AddressRequest request) {
        User user = getUser(email);
        Address address = new Address();
        address.setUser(user);
        applyRequest(address, request);
        return AddressDto.fromEntity(addressRepository.save(address));
    }

    public AddressDto updateAddress(String email, Long addressId, AddressRequest request) {
        User user = getUser(email);
        Address address = getOwnedAddressOrThrow(user, addressId);
        applyRequest(address, request);
        return AddressDto.fromEntity(addressRepository.save(address));
    }

    public void deleteAddress(String email, Long addressId) {
        User user = getUser(email);
        Address address = getOwnedAddressOrThrow(user, addressId);
        addressRepository.delete(address);
    }

    private void applyRequest(Address address, AddressRequest request) {
        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine(request.getAddressLine());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Address getOwnedAddressOrThrow(User user, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("This address does not belong to you");
        }
        return address;
    }
}