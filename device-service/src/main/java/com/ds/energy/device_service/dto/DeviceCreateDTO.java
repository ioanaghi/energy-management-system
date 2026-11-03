package com.ds.energy.device_service.dto;

import java.math.BigDecimal;

public class DeviceCreateDTO {
    @io.swagger.v3.oas.annotations.media.Schema(description = "Meter name", example = "Kitchen meter")
    private String name;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Maximum hourly consumption in kWh. Positive, at most 999999999.999, at most 3 decimal places.", example = "4.5")
    private BigDecimal maxConsumption;
    private String address;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Existing user ID; null removes the assignment", example = "2")
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
