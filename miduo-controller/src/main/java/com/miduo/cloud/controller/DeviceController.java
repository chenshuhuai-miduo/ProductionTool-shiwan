package com.miduo.cloud.controller;

import com.miduo.cloud.application.device.DeviceApplicationService;
import com.miduo.cloud.common.dto.ApiResult;
import com.miduo.cloud.entity.dto.device.IoDeviceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * IO设备管理控制器
 * 提供IO设备的配置、查询接口
 */
@RestController
@RequestMapping("/api/device")
@CrossOrigin
public class DeviceController {
    
    @Autowired
    private DeviceApplicationService deviceApplicationService;
    
    /**
     * 获取所有IO设备
     * GET /api/device/list
     */
    @GetMapping("/list")
    public ApiResult<List<IoDeviceDTO>> getAllDevices() {
        try {
            List<IoDeviceDTO> devices = deviceApplicationService.getAllDevices();
            return ApiResult.success("查询成功", devices);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据ID获取IO设备
     * GET /api/device/get/{id}
     */
    @GetMapping("/get/{id}")
    public ApiResult<IoDeviceDTO> getDeviceById(@PathVariable("id") String id) {
        try {
            IoDeviceDTO device = deviceApplicationService.getDeviceById(id);
            if (device == null) {
                return ApiResult.error("设备不存在");
            }
            return ApiResult.success("查询成功", device);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("查询失败：" + e.getMessage());
        }
    }
    
    /**
     * 添加IO设备
     * POST /api/device/add
     */
    @PostMapping("/add")
    public ApiResult<String> addDevice(@RequestBody IoDeviceDTO device) {
        try {
            String deviceId = deviceApplicationService.addDevice(device);
            return ApiResult.success("添加成功", deviceId);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("添加失败：" + e.getMessage());
        }
    }
    
    /**
     * 更新IO设备
     * PUT /api/device/update
     */
    @PutMapping("/update")
    public ApiResult<Boolean> updateDevice(@RequestBody IoDeviceDTO device) {
        try {
            Boolean success = deviceApplicationService.updateDevice(device);
            return ApiResult.success("更新成功", success);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("更新失败：" + e.getMessage());
        }
    }
    
    /**
     * 删除IO设备
     * DELETE /api/device/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public ApiResult<Boolean> deleteDevice(@PathVariable("id") String id) {
        try {
            Boolean success = deviceApplicationService.deleteDevice(id);
            return ApiResult.success("删除成功", success);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("删除失败：" + e.getMessage());
        }
    }
}

