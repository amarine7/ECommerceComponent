package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.monitoring.models.SystemStatus;
import com.retapps.smartbip.monitoring.services.MonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Deprecated
//@RestController
//@RequestMapping("/monitoring")
public class MonitoringController {

    @Autowired
    MonitoringService service;

    @RequestMapping(value = "", method = RequestMethod.GET)
    @ResponseBody
    public SmartBipResponse<SystemStatus> fetch(@RequestParam(required = false, defaultValue = "10") Integer offlineDeviceMinutes) {

        return new SmartBipResponse<>(service.getSystemStatus(offlineDeviceMinutes));
    }
}
