package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.web.basis.requests.StoreDeviceAvailableRequest;
import com.retapps.smartbip.api.web.basis.requests.StoreDeviceRefreshRequest;
import com.retapps.smartbip.api.web.basis.requests.StoreDeviceStatusRequest;
import com.retapps.smartbip.authentication.common.models.OperatorUser;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.StoreDevice;
import com.retapps.smartbip.basis.models.StoreDeviceConfiguration;
import com.retapps.smartbip.basis.services.StoresDevicesService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleStores.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@RequestMapping("/ope/stores/devices")
public class StoreDevicesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoreDevicesController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    StoresDevicesService service;

    @RequestMapping(value = "/available", method = RequestMethod.POST)
    public SmartBipResponse<StoreDevice> updateAvailable(Principal principal, HttpServletRequest request,
                                                         @RequestBody StoreDeviceAvailableRequest deviceAvailableRequest) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), STORE_DEVICE_UPDATE_AVAILABLE.get(deviceAvailableRequest.toString()), principal, request);

        StoreDevice storeDevice;

        if (isNotBlank(deviceAvailableRequest.getCode())) {

            storeDevice = service.setAvailableByCode(operatorUser.getTid(), operatorUser.getStoreId(),
                    deviceAvailableRequest.getCode(), deviceAvailableRequest.getIsAvailable());
        } else {

            // backward compatibility with static IP address devices
            storeDevice = service.setAvailable(operatorUser.getTid(), operatorUser.getStoreId(),
                deviceAvailableRequest.getIpAddress(), deviceAvailableRequest.getIsAvailable());
        }

        return new SmartBipResponse<>(storeDevice);
    }

    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    public SmartBipResponse<StoreDevice> refresh(HttpServletRequest request, @RequestBody StoreDeviceRefreshRequest refreshRequest) {

        Assert.hasLength(refreshRequest.getTid(), "Cannot refresh store device, tenant not set");
        Assert.hasLength(refreshRequest.getStoreId(), "Cannot refresh store device, store id not set");

        auditLogger.log(refreshRequest.getTid(), STORE_DEVICE_REFRESH.get(refreshRequest.toString()), null, request);

        StoreDevice storeDevice = service.fetchByDeviceTypeAndIpAddress(refreshRequest.getTid(),
                refreshRequest.getStoreId(), StoreDevice.DeviceType.MSS_CLIENT, refreshRequest.getIpAddress(), View.SUMMARY);
        if (storeDevice == null) {
            throw new EntityNotFoundException("Store device not found for store " + refreshRequest.getStoreId() +
                    " and ip address " + refreshRequest.getIpAddress());
        }
        LOGGER.trace("Fetched store device {}", storeDevice.getId());

        storeDevice = service.updateStatus(refreshRequest.getTid(), storeDevice, null);

        return new SmartBipResponse<>(storeDevice);
    }

    @RequestMapping(value = "/status", method = RequestMethod.POST)
    public SmartBipResponse<StoreDevice> status(Principal principal, HttpServletRequest request,
                                                 @RequestBody StoreDeviceStatusRequest storeDeviceStatusRequest) {

        String tid;
        if (principal != null) {
            try {
                OperatorUser operatorUser = principalResolver.getOperator(principal);
                tid = operatorUser.getTid();
            } catch (IllegalArgumentException ignored) {
                LOGGER.debug("Device is not in cradle, using profileUser to get tenant");
                ProfileUser profileUser = principalResolver.getProfile(principal);
                tid = profileUser.getTid();
            }
        } else {
            tid = storeDeviceStatusRequest.getTid();
        }

        StoreDevice storeDevice = storeDeviceStatusRequest.getStoreDevice();

        if (storeDevice.getDeviceType() == null) {
            storeDevice.setDeviceType(StoreDevice.DeviceType.MSS_CLIENT);
        }

        if (isNotBlank(storeDeviceStatusRequest.getStoreId())) {
            storeDevice.setStoreId(storeDeviceStatusRequest.getStoreId());
        }

        auditLogger.log(tid, STORE_DEVICE_STATUS.get(storeDeviceStatusRequest.toString()), principal, request);

        StoreDevice updatedStoreDevice = service.updateStatus(tid, storeDevice, storeDeviceStatusRequest.getBoot());

        return new SmartBipResponse<>(updatedStoreDevice);
    }

    @RequestMapping(value = "/config", method = RequestMethod.GET)
    public SmartBipResponse<StoreDeviceConfiguration> config(Principal principal, HttpServletRequest request,
                                                             @RequestParam String tid,
                                                             @RequestParam String storeId) {

        auditLogger.log(tid, STORE_DEVICE_CONFIG.get(), principal, request);

        StoreDeviceConfiguration configuration = service.getDeviceConfiguration(tid, storeId);

        return new SmartBipResponse<>(configuration);
    }

    @RequestMapping(value = "/logs", method = RequestMethod.POST)
    public SmartBipResponse<String> logs(Principal principal, HttpServletRequest request,
                                         @RequestParam(required = false) String tid,
                                         @RequestParam(required = false) String storeId,
                                         @RequestParam(required = false) String deviceCode,
                                         @RequestParam(value = "file", required = false) MultipartFile file) {

        auditLogger.log("01", STORE_DEVICE_LOGS.get(), principal, request);

        LOGGER.trace("Receiving logs file {} for device {} store {} and tid {}",
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "NULL",
                deviceCode, storeId, tid);

        service.storeLogs(tid, storeId, deviceCode, file);

        return new SmartBipResponse<>("OK");
    }

    @RequestMapping(value = "/blink", method = RequestMethod.GET)
    public SmartBipResponse<String> blink(Principal principal, HttpServletRequest request,
                                         @RequestParam(required = false) String deviceCode,
                                         @RequestParam(required = false) String appVersion) {

        OperatorUser operator = principalResolver.getOperator(principal);
        String tid = operator.getTid();
        String storeId = operator.getStoreId();

        auditLogger.log(tid, STORE_DEVICE_BLINK.get(), principal, request);

        if (isBlank(deviceCode)) {

            LOGGER.trace("Blinking all devices for store {} and app version {}", storeId, appVersion);
            service.requestDeviceBlinkAll(tid, storeId, appVersion);
        } else {

            LOGGER.trace("Blinking all device for store {} and code {}", storeId, deviceCode);
            service.requestDeviceBlink(tid, service.fetchByDeviceTypeAndCode(tid, storeId,
                    StoreDevice.DeviceType.MSS_CLIENT, deviceCode, View.SUMMARY).getId());
        }

        return new SmartBipResponse<>("OK");
    }
}
