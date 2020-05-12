package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.web.basis.dtos.ProfileUpdateDto;
import com.retapps.smartbip.api.web.basis.requests.ProfileDeleteRequest;
import com.retapps.smartbip.api.web.basis.responses.ProfileDeleteResponse;
import com.retapps.smartbip.authentication.common.models.OperatorUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.Profile;
import com.retapps.smartbip.basis.services.PrivacyEngine;
import com.retapps.smartbip.basis.services.ProfilesService;
import com.retapps.smartbip.basis.validators.CodeValidator;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.shopping.lib.handlers.CartPrivacyEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.*;

@RestController
@RequestMapping("/ope/profiles")
public class OperatorsProfilesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatorsProfilesController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    ProfilesService profilesService;

    @Autowired
    @Qualifier("cardCodeValidator")
    CodeValidator codeValidator;

    @Autowired
    AuditLogger auditLogger;

    @Autowired(required = false)
    PrivacyEngine privacyEngine;

    @Autowired(required = false)
    CartPrivacyEngine cartPrivacyEngine;

    @RequestMapping(method = RequestMethod.POST)
    public SmartBipResponse<Profile> update(Principal principal, HttpServletRequest request,
                                            @RequestBody @Valid ProfileUpdateDto dto) {

        LOGGER.debug("Requested profile update with: {}", dto);

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        auditLogger.log(operatorUser.getTid(), OPERATOR_PROFILE_UPDATE.get(dto.toString()), principal, request);

        Assert.hasLength(dto.getId(), "Error updating the profile, missing id");

        Profile existingProfile = profilesService.read(operatorUser.getTid(), dto.getId());

        Profile profile = ProfileUpdateDto.merge(existingProfile, dto);

        codeValidator.validateCards(profile.getCards());

        profile = profilesService.update(operatorUser.getTid(), profile);

        LOGGER.debug("Updated profile: {}", profile);

        return new SmartBipResponse<>(profile);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public SmartBipResponse<ProfileDeleteResponse> delete(Principal principal, HttpServletRequest request,
                                                          @RequestBody @Valid ProfileDeleteRequest deleteRequest) {

        LOGGER.debug("Requested profile delete (archive) with request {}", deleteRequest);

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        String tid = operatorUser.getTid();
        auditLogger.log(tid, OPERATOR_PROFILE_DELETE.get(deleteRequest.toString()), principal, request);

        String profileId = deleteRequest.getProfileId();
        Assert.hasLength(profileId, "Error archiving the profile, missing id");

        if (cartPrivacyEngine != null) {
            cartPrivacyEngine.anonymizeAllCartsForProfile(tid, profilesService.read(tid, profileId), Boolean.TRUE);
        }

        profilesService.archive(tid, profileId);

        LOGGER.debug("Archived profile: {}", profileId);

        return new SmartBipResponse<>(new ProfileDeleteResponse(profileId));
    }

    @RequestMapping(value = "/card/{card}", method = RequestMethod.GET)
    public SmartBipResponse<Profile> login(@PathVariable("card") String card, Principal principal, HttpServletRequest request) {

        OperatorUser operatorUser = principalResolver.getOperator(principal);

        LOGGER.debug("Fetching profile by card {} for tenant {}", card, operatorUser.getTid());

        auditLogger.log(operatorUser.getTid(), OPERATOR_PROFILE_FETCH.get(principal.toString()), principal, request);

        Profile profile = profilesService.readByCard(operatorUser.getTid(), card);

        LOGGER.trace("Fetched profile {} by card {} for tenant {}", profile.getId(), card, operatorUser.getTid());

        return new SmartBipResponse<>(profile);
    }
}