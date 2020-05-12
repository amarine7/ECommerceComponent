package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.authentication.common.models.OperatorUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.Configuration;
import com.retapps.smartbip.basis.services.ConfigurationService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.audit.services.AuditService;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.models.responses.SmartBipResponseCode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@RestController
public class ConfigurationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    ConfigurationService service;

    @RequestMapping(value = "/cli/configuration", method = RequestMethod.GET)
    public SmartBipResponse<Configuration> clientConfiguration(Principal principal, HttpServletRequest request,
                                                               @RequestParam(value = "tid", required = false) String tid) {

        tid = principalResolver.getRetailerId(principal, tid);

        auditLogger.log(tid, AuditService.ModuleAuth.CONFIGURATION.get(), principal, request);
        LOGGER.trace("Client configuration request");

        try {
            return new SmartBipResponse<>(service.clientConfiguration(tid));
        } catch (Exception e) {
            LOGGER.warn("Error building client configuration: {}", ExceptionUtils.getMessage(e), e);
            return new SmartBipResponse<>(SmartBipResponseCode.GENERIC_ERROR, "Error building client configuration");
        }
    }

    @RequestMapping(value = "/ope/configuration", method = RequestMethod.GET)
    public SmartBipResponse<Configuration> operatorConfiguration(Principal principal, HttpServletRequest request) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), AuditService.ModuleAuth.CONFIGURATION.get(), principal, request);
        LOGGER.trace("Operator configuration request");

        try {
            return new SmartBipResponse<>(service.operatorConfiguration(operatorUser.getTid()));
        } catch (Exception e) {
            LOGGER.warn("Error building operator configuration: {}", ExceptionUtils.getMessage(e), e);
            return new SmartBipResponse<>(SmartBipResponseCode.GENERIC_ERROR, "Error building operator configuration");
        }
    }
}
