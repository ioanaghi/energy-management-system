package com.ds.energy.device_service.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ds.energy.device_service.dto.DeviceCreateDTO;
import com.ds.energy.device_service.dto.DeviceDTO;
import com.ds.energy.device_service.entity.Device;
import com.ds.energy.device_service.repository.DeviceRepository;

@Service
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getAllDevices() {
        return deviceRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<DeviceDTO> getDevicesByUserId(Long userId) {
        validatePositiveId(userId, "User ID");
        return deviceRepository.findAllByUserId(userId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DeviceDTO getDeviceById(Long id) {
        return toDTO(findDevice(id));
    }

    public DeviceDTO createDevice(DeviceCreateDTO request) {
        validateRequest(request);
        validateOptionalUserId(request.getUserId());

        Device device = new Device();
        applyRequest(device, request);
        return toDTO(deviceRepository.save(device));
    }

    public DeviceDTO updateDevice(Long id, DeviceCreateDTO request) {
        validateRequest(request);
        validateOptionalUserId(request.getUserId());

        Device device = findDevice(id);
        applyRequest(device, request);
        return toDTO(deviceRepository.save(device));
    }

    public DeviceDTO assignDeviceToUser(Long id, Long userId) {
        validatePositiveId(userId, "User ID");
        Device device = findDevice(id);
        device.setUserId(userId);
        return toDTO(deviceRepository.save(device));
    }

    public DeviceDTO unassignDeviceFromUser(Long id) {
        Device device = findDevice(id);
        device.setUserId(null);
        return toDTO(deviceRepository.save(device));
    }

    public void deleteDevice(Long id) {
        deviceRepository.delete(findDevice(id));
    }

    private Device findDevice(Long id) {
        validatePositiveId(id, "Device ID");
        return deviceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    private void validateRequest(DeviceCreateDTO request) {
        if (request == null || request.getName() == null || request.getName().isBlank() || request.getName().length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device name is required and must have at most 150 characters");
        }
        if (request.getAddress() != null && request.getAddress().length() > 250) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Address must have at most 250 characters");
        }
        BigDecimal maxConsumption = request.getMaxConsumption();
        if (maxConsumption == null || maxConsumption.compareTo(BigDecimal.ZERO) <= 0
                || maxConsumption.compareTo(new BigDecimal("999999999.999")) > 0
                || maxConsumption.stripTrailingZeros().scale() > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Maximum consumption must be positive, at most 999999999.999, with at most 3 decimal places");
        }
    }

    private void validateOptionalUserId(Long userId) {
        if (userId != null) {
            validatePositiveId(userId, "User ID");
        }
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be a positive number");
        }
    }

    private void applyRequest(Device device, DeviceCreateDTO request) {
        device.setName(request.getName().trim());
        device.setMaxConsumption(request.getMaxConsumption());
        device.setAddress(request.getAddress());
        device.setUserId(request.getUserId());
    }

    private DeviceDTO toDTO(Device device) {
        DeviceDTO dto = new DeviceDTO();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setMaxConsumption(device.getMaxConsumption());
        dto.setAddress(device.getAddress());
        dto.setUserId(device.getUserId());
        return dto;
    }
}
