package com.ds.energy.device_service.dto;

import java.math.BigDecimal;

public class DeviceCreateDTO {
    private String name;
    private BigDecimal maxConsumption;
    private String address;
    private Long userId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMaxConsumption() {
        return maxConsumption;
    }

    public void setMaxConsumption(BigDecimal maxConsumption) {
        this.maxConsumption = maxConsumption;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
