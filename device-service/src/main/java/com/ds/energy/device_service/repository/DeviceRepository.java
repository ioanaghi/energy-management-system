package com.ds.energy.device_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ds.energy.device_service.entity.Device;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findAllByUserId(Long userId);
}
