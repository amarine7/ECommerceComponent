package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.models.OperatorLogin;
import com.retapps.smartbip.authentication.common.models.OperatorUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.Operator;
import com.retapps.smartbip.basis.models.Store;
import com.retapps.smartbip.basis.services.OperatorsAuthenticationService;
import com.retapps.smartbip.basis.services.StoresService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.TenantsService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.OPERATOR_LOGIN;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@RequestMapping("/ope/operators")
public class OperatorsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorsController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    StoresService storesService;

    @Autowired
    OperatorsAuthenticationService authenticationService;

    @Autowired
    TenantsService tenantsService;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public SmartBipResponse<OperatorLogin> login(Principal principal, HttpServletRequest request) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), OPERATOR_LOGIN.get(principal.toString()), principal, request);

        Operator operator = authenticationService.readOperator(operatorUser);

        try {
            operator.setTenant(tenantsService.readByTenantId(operatorUser.getTid()));
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Error fetching tenant {}: {}", operatorUser.getTid(), ExceptionUtils.getMessage(e));
        }

        Store store = null;
        try {
            if (isNotBlank(operatorUser.getStoreId())) {
                store = storesService.read(operatorUser.getTid(), operatorUser.getStoreId());
            }
        } catch (EntityNotFoundException e) {
            LOGGER.warn("Error fetching store {} for tenant {}: {}", operatorUser.getStoreId(), operatorUser.getTid(),
                    ExceptionUtils.getMessage(e));
        }

        return new SmartBipResponse<>(new OperatorLogin(operator, store));
    }
}