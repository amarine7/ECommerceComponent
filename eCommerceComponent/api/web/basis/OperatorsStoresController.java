package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.web.basis.requests.StoreMssCartOpenEnableRequest;
import com.retapps.smartbip.authentication.common.models.OperatorUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.Store;
import com.retapps.smartbip.basis.services.StoresService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.OPERATOR_ENABLE_MSS_CART_OPEN;
import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.OPERATOR_STORE_READ;

@RestController
@RequestMapping("/ope/stores")
public class OperatorsStoresController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorsStoresController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    StoresService storesService;

    @RequestMapping(method = RequestMethod.GET)
    public SmartBipResponse<Store> read(Principal principal, HttpServletRequest request) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), OPERATOR_STORE_READ.get(principal.toString()), principal, request);

        Store store = storesService.read(operatorUser.getTid(), operatorUser.getStoreId());

        return new SmartBipResponse<>(store);
    }

    @RequestMapping(value = "/mss/enabled", method = RequestMethod.POST)
    public SmartBipResponse<Store> read(Principal principal, HttpServletRequest request,
                                        @RequestBody StoreMssCartOpenEnableRequest storeMssCartOpenEnableRequest) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), OPERATOR_ENABLE_MSS_CART_OPEN.get(storeMssCartOpenEnableRequest.toString()), principal, request);

        LOGGER.info("Changing MSS cart open enabled state to {} by operator {}", storeMssCartOpenEnableRequest.getMssCartOpenEnabled(), operatorUser);

        storesService.updateMssCartOpenEnabled(operatorUser.getTid(), operatorUser.getStoreId(),
                storeMssCartOpenEnableRequest.getMssCartOpenEnabled());

        return new SmartBipResponse<>(storesService.read(operatorUser.getTid(), operatorUser.getStoreId()));
    }
}