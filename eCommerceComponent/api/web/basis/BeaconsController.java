package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.web.basis.dtos.ShopBeaconDto;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.services.BeaconsService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleStores.BEACONS;

@RestController
@RequestMapping("/cli/beacons")
public class BeaconsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BeaconsController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    BeaconsService beaconsService;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(value = "/shop/{id}", method = RequestMethod.GET)
    public SmartBipResponse<ShopBeaconDto> fetch(Principal principal, HttpServletRequest request, @PathVariable("id") String id) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        // Audit
        auditLogger.log(profileUser.getTid(), BEACONS.get("id=" + id), principal, request);

        return new SmartBipResponse<>(ShopBeaconDto.to(beaconsService.read(profileUser.getTid(), id)));
    }

    @RequestMapping(value = "/shop/code/{code}", method = RequestMethod.GET)
    public SmartBipResponse<ShopBeaconDto> fetchByCode(Principal principal, HttpServletRequest request, @PathVariable("code") String code) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        // Audit
        auditLogger.log(profileUser.getTid(), BEACONS.get("code=" + code), principal, request);

        return new SmartBipResponse<>(ShopBeaconDto.to(beaconsService.readByCode(profileUser.getTid(), code, View.FULL)));
    }

    @RequestMapping(value = "/shop/major/{major}/minor/{minor}", method = RequestMethod.GET)
    public SmartBipResponse<ShopBeaconDto> fetchByMajorMinor(Principal principal, HttpServletRequest request,
                                                             @PathVariable("major") String major,
                                                             @PathVariable("minor") String minor) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        // Audit
        auditLogger.log(profileUser.getTid(), BEACONS.get("major=" + major + ",minor=" + minor), principal, request);

        Assert.notNull(major, "Invalid major");
        Assert.notNull(minor, "Invalid minor");

        String code = String.format("%s-%s", major, minor);
        LOGGER.debug("Created beacon code {} from major {} and minor {}", code, major, minor);

        return new SmartBipResponse<>(ShopBeaconDto.to(beaconsService.readByCode(profileUser.getTid(), code, View.FULL)));
    }
}
