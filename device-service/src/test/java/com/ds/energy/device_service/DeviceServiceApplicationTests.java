package com.ds.energy.device_service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ds.energy.device_service.dto.DeviceCreateDTO;
import com.ds.energy.device_service.service.DeviceService;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DeviceServiceApplicationTests {
    @Autowired
    private DeviceService service;

    private DeviceCreateDTO request(String consumption) {
        DeviceCreateDTO data = new DeviceCreateDTO();
        data.setName("Smart meter");
        data.setMaxConsumption(new BigDecimal(consumption));
        return data;
    }

    @Test
    void createsUpdatesAndDeletesDevices() {
        var created = service.createDevice(request("4.500"));
        assertEquals("Smart meter", service.getDeviceById(created.getId()).getName());
        var updated = service.updateDevice(created.getId(), request("7.250"));
        assertEquals(0, updated.getMaxConsumption().compareTo(new BigDecimal("7.25")));
        service.deleteDevice(created.getId());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.getDeviceById(created.getId())).getStatusCode().value());
    }

    @Test
    void assignsAndUnassignsDevices() {
        var created = service.createDevice(request("4.5"));
        service.assignDeviceToUser(created.getId(), 42L);
        assertEquals(1, service.getDevicesByUserId(42L).size());
        assertEquals(42L, service.getDeviceById(created.getId()).getUserId());
        service.unassignDeviceFromUser(created.getId());
        assertTrue(service.getDevicesByUserId(42L).isEmpty());
        assertNull(service.getDeviceById(created.getId()).getUserId());
    }

    @Test
    void rejectsInvalidConsumptionAndAssignmentIds() {
        for (String value : new String[]{"0", "-1", "0.0001", "1000000000"}) {
            assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.createDevice(request(value))).getStatusCode().value());
        }
        var created = service.createDevice(request("4.5"));
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> service.assignDeviceToUser(created.getId(), -1L)).getStatusCode().value());
    }
}
