package main.java.com.ds.energy.device_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ds.energy.device_service.dto.DeviceCreateDTO;
import com.ds.energy.device_service.dto.DeviceDTO;
import com.ds.energy.device_service.service.DeviceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "List devices", description = "Returns every device and its optional user association.")
    @ApiResponse(responseCode = "200", description = "Device list returned")
    @GetMapping
    public List<DeviceDTO> getAllDevices() {
        return deviceService.getAllDevices();
    }

    @Operation(summary = "List devices assigned to a user", description = "Filters devices by the associated user ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devices returned; an empty list means none are assigned"),
            @ApiResponse(responseCode = "400", description = "User ID is invalid")
    })
    @GetMapping("/by-user/{userId}")
    public List<DeviceDTO> getDevicesByUserId(@PathVariable Long userId) {
        return deviceService.getDevicesByUserId(userId);
    }

    @Operation(summary = "Get a device", description = "Returns one device by its ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device returned"),
            @ApiResponse(responseCode = "404", description = "Device does not exist")
    })
    @GetMapping("/{id}")
    public DeviceDTO getDeviceById(@PathVariable Long id) {
        return deviceService.getDeviceById(id);
    }

    @Operation(summary = "Create a device", description = "Creates a device. Name and positive maximum consumption are required.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Device created"),
            @ApiResponse(responseCode = "400", description = "Required data is invalid")
    })
    @PostMapping
    public ResponseEntity<DeviceDTO> createDevice(@RequestBody DeviceCreateDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(deviceService.createDevice(request));
    }

    @Operation(summary = "Update a device", description = "Replaces the device fields. Set userId to null to remove its user association.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device updated"),
            @ApiResponse(responseCode = "400", description = "Required data is invalid"),
            @ApiResponse(responseCode = "404", description = "Device does not exist")
    })
    @PutMapping("/{id}")
    public DeviceDTO updateDevice(@PathVariable Long id, @RequestBody DeviceCreateDTO request) {
        return deviceService.updateDevice(id, request);
    }

    @Operation(summary = "Assign a device to a user", description = "Associates the device with the supplied user ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device assigned"),
            @ApiResponse(responseCode = "400", description = "ID is invalid"),
            @ApiResponse(responseCode = "404", description = "Device does not exist")
    })
    @PutMapping("/{id}/assignment/{userId}")
    public DeviceDTO assignDeviceToUser(@PathVariable Long id, @PathVariable Long userId) {
        return deviceService.assignDeviceToUser(id, userId);
    }

    @Operation(summary = "Remove a device assignment", description = "Unassigns the device but keeps the device record.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Device unassigned"),
            @ApiResponse(responseCode = "404", description = "Device does not exist")
    })
    @DeleteMapping("/{id}/assignment")
    public DeviceDTO unassignDeviceFromUser(@PathVariable Long id) {
        return deviceService.unassignDeviceFromUser(id);
    }

    @Operation(summary = "Delete a device", description = "Permanently deletes the device record.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Device deleted"),
            @ApiResponse(responseCode = "404", description = "Device does not exist")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDevice(@PathVariable Long id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }
}
