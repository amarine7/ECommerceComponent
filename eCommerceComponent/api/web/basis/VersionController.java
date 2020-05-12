package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.engines.VersionEngine;
import com.retapps.smartbip.api.web.basis.requests.VersionRequest;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.audit.services.AuditService;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.models.responses.SmartBipResponseCode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

@RestController
public class VersionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionController.class);

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    VersionEngine versionEngine;

    @RequestMapping(value = "/cli/version", method = RequestMethod.POST)
    public SmartBipResponse<String> clientVersion(HttpServletRequest request, @RequestBody @Valid VersionRequest versionRequest) {

        auditLogger.log(null, AuditService.ModuleAuth.VERSION.get(), null, request);
        LOGGER.trace("Checking client version for request: {}", request);

        try {
            if (versionEngine.check(versionRequest.getOs(), versionRequest.getVersion())) {
                LOGGER.debug("Version is up to date for client request: {}", request);
                return new SmartBipResponse<>("OK"); // FIXME: i18n
            } else {
                LOGGER.warn("Version is NOT up to date for request: {}", request);
                return new SmartBipResponse<>(SmartBipResponseCode.INVALID_VERSION,
                        "Per continuare ad utilizzare l'applicazione è necessario l'aggiornamento all'ultima versione."); // FIXME: i18n
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking client version for request {}", versionRequest, ExceptionUtils.getMessage(e));
            return new SmartBipResponse<>(SmartBipResponseCode.INVALID_VERSION, "Error checking version");
        }
    }

    @RequestMapping(value = "/ope/version", method = RequestMethod.POST)
    public SmartBipResponse<String> operatorVersion(HttpServletRequest request, @RequestBody @Valid VersionRequest versionRequest) {

        auditLogger.log(null, AuditService.ModuleAuth.VERSION.get(), null, request);
        LOGGER.trace("Checking operator version for request: {}", request);

        try {
            if (versionEngine.check(versionRequest.getOs(), versionRequest.getVersion())) {
                LOGGER.debug("Version is up to date for operator request: {}", request);
                return new SmartBipResponse<>("OK"); // FIXME: i18n
            } else {
                LOGGER.warn("Version is NOT up to date for request: {}", request);
                return new SmartBipResponse<>(SmartBipResponseCode.INVALID_VERSION,
                        "Per continuare ad utilizzare l'applicazione è necessario l'aggiornamento all'ultima versione."); // FIXME: i18n
            }
        } catch (Exception e) {
            LOGGER.warn("Error checking operator version for request {}", versionRequest, ExceptionUtils.getMessage(e));
            return new SmartBipResponse<>(SmartBipResponseCode.INVALID_VERSION, "Error checking version");
        }
    }
}
