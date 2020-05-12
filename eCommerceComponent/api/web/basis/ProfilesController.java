package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.api.web.basis.dtos.NotificationDto;
import com.retapps.smartbip.api.web.basis.dtos.ProfileSignUpDto;
import com.retapps.smartbip.api.web.basis.dtos.ProfileUpdateDto;
import com.retapps.smartbip.api.web.basis.requests.*;
import com.retapps.smartbip.api.web.basis.responses.ProfileDeleteResponse;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.*;
import com.retapps.smartbip.basis.services.ProfilesAuthenticationService;
import com.retapps.smartbip.basis.services.ProfilesService;
import com.retapps.smartbip.basis.validators.CodeValidator;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.config.Properties;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.notifications.models.Notification;
import com.retapps.smartbip.notifications.services.NotificationsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.*;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_LIMIT;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_PAGE;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@RestController
@RequestMapping("/cli/profiles")
public class ProfilesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfilesController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    ProfilesService profilesService;

    @Autowired
    ProfilesAuthenticationService authenticationService;

    @Autowired
    NotificationsService notificationsService;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    @Qualifier("cardCodeValidator")
    CodeValidator codeValidator;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public SmartBipResponse<Profile> login(Principal principal, HttpServletRequest request) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        auditLogger.log(profileUser.getTid(), PROFILE_LOGIN.get(), principal, request);

        Profile profile = authenticationService.readProfile(profileUser);

        LOGGER.trace("Profile {} logged in", profile.getId());

        return new SmartBipResponse<>(profile);
    }

//    @RequestMapping(value = "/logout-success", method = RequestMethod.GET)
//    public void logout(Principal principal, HttpServletRequest request) {
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        auditLogger.log(profileUser.getTid(), PROFILE_LOGOUT.get(), principal, request);
//    }

    @RequestMapping(value = "/logout", method = RequestMethod.POST)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        for (Cookie cookie : request.getCookies()) {
            cookie.setMaxAge(0);
        }
    }

    @RequestMapping(value = "/signup", method = RequestMethod.POST)
    public SmartBipResponse<Profile> signUp(HttpServletRequest request,
                                            @RequestBody @Valid ProfileSignUpRequest dto) {

        LOGGER.debug("Requested profile sign up for  dto {}", dto);

        String tid = Properties.DEFAULT_TENANT;
        LOGGER.trace("Setting default tenant {} for profile sign up", tid);

        auditLogger.log(tid, PROFILE_SIGNUP.get(dto.toString()), null, request);

        Assert.notNull(dto.getProfile(), "Invalid profile");
        Assert.hasLength(dto.getUsername(), "Invalid username");
        Assert.hasLength(dto.getPassword(), "Invalid password");

        codeValidator.validateCards(dto.getProfile().getCards());
        Profile profile = profilesService.signUp(tid, ProfileSignUpDto.convert(dto.getProfile()), dto.getUsername(),
                dto.getPassword());


        //LOGGER.trace("Profile after sign up for tenant {} is {}", tid, profile);

        return new SmartBipResponse<>(profile);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public SmartBipResponse<Profile> update(Principal principal, HttpServletRequest request,
                                            @RequestBody @Valid ProfileUpdateDto dto) {

        LOGGER.debug("Requested profile update with: {}", dto);

        ProfileUser profileUser = principalResolver.getProfile(principal);

        auditLogger.log(profileUser.getTid(), PROFILE_UPDATE.get(dto.toString()), principal, request);

        Assert.hasLength(profileUser.getId(), "Error updating the profile, missing id");

        Profile existingProfile = profilesService.read(profileUser.getTid(), profileUser.getId());

        Profile profile = ProfileUpdateDto.merge(existingProfile, dto);

        codeValidator.validateCards(profile.getCards());

        profile = profilesService.update(profileUser.getTid(), profile);

        LOGGER.debug("Updated profile: {}", profile);

        return new SmartBipResponse<>(profile);
    }

    @RequestMapping(value = "/flags", method = RequestMethod.PUT)
    public SmartBipResponse<Profile> updateFlags(Principal principal, HttpServletRequest request,
                                                 @RequestBody ProfileFlagsRequest flags) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Requested flags update for profile #{} with flags: {} for tenant {}", profileUser.getId(), flags,
                profileUser.getTid());

        auditLogger.log(profileUser.getTid(), PROFILE_UPDATE_PRIVACY.get(flags.toString()), principal, request);

        Profile profile = profilesService.updateFlags(profileUser.getTid(), profileUser.getId(), flags.getAccept1(),
                flags.getAccept2(), flags.getAccept3(), flags.getAccept4());
        LOGGER.trace("Updated flags for profile {}", profile.getId());

        return new SmartBipResponse<>(profile);
    }

    @RequestMapping(value = "/employee", method = RequestMethod.POST)
    public SmartBipResponse<Profile> setEmployeeInformation(Principal principal, HttpServletRequest request,
                                                            @RequestBody ProfileEmployeeRequest employeeRequest) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Requested employee information update for profile #{} with request: {} for tenant {}",
                profileUser.getId(), employeeRequest, profileUser.getTid());

        auditLogger.log(profileUser.getTid(), PROFILE_UPDATE_EMPLOYEE.get(employeeRequest.toString()), principal, request);

        Profile profile = profilesService.read(profileUser.getTid(), profileUser.getId());
        LOGGER.trace("Fetched profile #{}", profile.getId());

        // Setting employee information
        profile.setEmployee(true);

        ProfileCard card = new ProfileCard();
        card.setCode(trimToNull(employeeRequest.getCardCode()));
        card.setEnabled(true);
        card.setPrincipal(false);
        if (profile.getCards() != null && !profile.getCards().contains(card)) {
            profile.addCard(card);
        }

        return new SmartBipResponse<>(profilesService.update(profileUser.getTid(), profile));
    }

    @RequestMapping(value = "/preferences/store", method = RequestMethod.POST)
    public SmartBipResponse<Profile> preferencesStore(Principal principal, HttpServletRequest request,
                                                      @RequestBody ProfilePreferences preferences) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Requested profile preferences store update for profile #{} with preferences {} for tenant {}", profileUser.getId(),
                preferences, profileUser.getTid());

        auditLogger.log(profileUser.getTid(), PROFILE_PREFS_STORE_UPDATE.get(preferences.toString()), null, request);

        Profile profile = profilesService.updatePreferencesStore(profileUser.getTid(), profileUser.getId(), preferences.getStoreId());
        LOGGER.trace("Profile after updating preferences {}", profile);

        return new SmartBipResponse<>(profile);
    }

    @RequestMapping(value = "/preferences", method = RequestMethod.POST)
    public SmartBipResponse<Profile> preferences(Principal principal, HttpServletRequest request,
                                                 @RequestBody ProfilePreferences preferences) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Requested profile preferences update for profile #{} with preferences {} for tenant {}", profileUser.getId(),
                preferences, profileUser.getTid());

        auditLogger.log(profileUser.getTid(), PROFILE_PREFS_UPDATE.get(preferences.toString()), null, request);

        Profile profile = profilesService.updatePreferences(profileUser.getTid(), profileUser.getId(), preferences);
        LOGGER.trace("Profile after updating preferences {}", profile);

        return new SmartBipResponse<>(profile);
    }

    /**
     * Password reset for unauthenticated profiles
     */
    @RequestMapping(value = "/password", method = RequestMethod.POST)
    public ResponseEntity<SmartBipResponse> resetPassword(Principal principal, HttpServletRequest request,
                                                          @RequestBody ProfilePasswordResetRequest passwordRequest) {

        LOGGER.debug("Requested password reset with request {}", passwordRequest);

        Assert.hasLength(passwordRequest.getTid(), "Cannot request password reset, invalid tid");
        Assert.hasLength(passwordRequest.getEmail(), "Cannot request password reset, invalid email");

        auditLogger.log(passwordRequest.getTid(), PASSWORD_RESET_REQUEST.get(request.toString()), principal, request);

        profilesService.requestPasswordReset(passwordRequest.getTid(), passwordRequest.getEmail());
        LOGGER.trace("Password reset requested for profile {} and tenant {}", passwordRequest.getEmail(), passwordRequest.getTid());

        return new ResponseEntity<>(new SmartBipResponse(), HttpStatus.OK);
    }

    /**
     * Password change for authenticated profiles
     */
    @RequestMapping(value = "/password/change", method = RequestMethod.POST)
    public ResponseEntity<SmartBipResponse> changePassword(Principal principal, HttpServletRequest request,
                                                           @RequestBody ProfilePasswordChangeRequest passwordRequest) {

        LOGGER.debug("Requested password change with request {}", passwordRequest);

        ProfileUser profileUser = principalResolver.getProfile(principal);
        auditLogger.log(profileUser.getTid(), PASSWORD_CHANGE.get(request.toString()), principal, request);

        Assert.hasLength(passwordRequest.getOldPassword(), "Cannot request password change, invalid old password");
        Assert.hasLength(passwordRequest.getNewPassword(), "Cannot request password change, invalid new password");

        profilesService.changePassword(profileUser.getTid(), profileUser.getId(), passwordRequest.getOldPassword(), passwordRequest.getNewPassword());

        return new ResponseEntity<>(new SmartBipResponse(), HttpStatus.OK);
    }

    /**
     * Validates the activation OTP and enables the profile
     */
    @RequestMapping(value = "/{profileId}/otp/{otp}", method = RequestMethod.POST)
    public SmartBipResponse<Profile> validateActivationOtp(HttpServletRequest request,
                                                           @PathVariable("profileId") String profileId,
                                                           @PathVariable("otp") String otp) {

        LOGGER.debug("Validating activation OTP {} for profile {}", otp, profileId);

        auditLogger.log(null, PROFILE_OTP_ACTIVATION.get("profileId=" + profileId + ",otp=" + otp), null, request);

        Assert.hasLength(otp, "Invalid otp");
        Assert.hasLength(profileId, "Invalid profile id");

        return new SmartBipResponse<>(profilesService.enableByOtp(profileId, otp));
    }

    @RequestMapping(value = "/notifications", method = RequestMethod.GET)
    public PagedSmartBipResponse<NotificationDto> list(Principal principal, HttpServletRequest request,
                                                       @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                                       @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Listing notification for profile {} and page {} and size {}", profileUser.getId(), page, size);

        auditLogger.log(null, PROFILE_NOTIFICATIONS.get(), null, request);

        PageRequest pageRequest = new PageRequest(page, size, Sort.Direction.DESC, "updated");
        Page<Notification> result = notificationsService.findAllByProfileId(profileUser.getTid(), profileUser.getId(), pageRequest);

        List<NotificationDto> dtos = new ArrayList<>(result.getContent().size());
        for (Notification notification : result.getContent()) {
            dtos.add(NotificationDto.to(notification));
        }

        return new PagedSmartBipResponse<>(dtos, result.getTotalElements(), result.getTotalPages(), result.isFirst(),
                result.isLast(), result.getNumber(), result.getSize());
    }

    @RequestMapping(value = "/addresses", method = RequestMethod.GET)
    public PagedSmartBipResponse<ProfileAddress> addresses(Principal principal, HttpServletRequest request) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Fetching addresses for profile {}", profileUser.getId());

        Profile profile = profilesService.read(profileUser.getTid(), profileUser.getId());
        List<ProfileAddress> result = profile.getAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/addresses", method = RequestMethod.POST)
    public PagedSmartBipResponse<ProfileAddress> upsertAddress(Principal principal, HttpServletRequest request,
                                                               @RequestBody @Valid ProfileAddress address) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Upserting address for profile {}", profileUser.getId());

        Profile profile = profilesService.upsertAddress(profileUser.getTid(), profileUser.getId(), address);
        List<ProfileAddress> result = profile.getAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/addresses/delete", method = RequestMethod.POST)
    public PagedSmartBipResponse<ProfileAddress> deleteAddress(Principal principal, HttpServletRequest request,
                                                               @RequestBody @Valid ProfileAddress address) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Deleting address for profile {}", profileUser.getId());

        Profile profile = profilesService.deleteAddress(profileUser.getTid(), profileUser.getId(), address.getId());
        List<ProfileAddress> result = profile.getAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }


    @RequestMapping(value = "/billing/addresses", method = RequestMethod.GET)
    public PagedSmartBipResponse<ProfileBillingAddress> billingAddresses(Principal principal, HttpServletRequest request) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Fetching billing addresses for profile {}", profileUser.getId());

        Profile profile = profilesService.read(profileUser.getTid(), profileUser.getId());
        List<ProfileBillingAddress> result = profile.getBillingAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileBillingAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/billing/addresses", method = RequestMethod.POST)
    public PagedSmartBipResponse<ProfileBillingAddress> upsertBillingAddress(Principal principal, HttpServletRequest request,
                                                               @RequestBody @Valid ProfileBillingAddress address) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Upserting billing address for profile {}", profileUser.getId());

        Profile profile = profilesService.upsertBillingAddress(profileUser.getTid(), profileUser.getId(), address);
        List<ProfileBillingAddress> result = profile.getBillingAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileBillingAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/billing/addresses/delete", method = RequestMethod.POST)
    public PagedSmartBipResponse<ProfileBillingAddress> deleteBillingAddress(Principal principal, HttpServletRequest request,
                                                               @RequestBody @Valid ProfileBillingAddress address) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Deleting billing address for profile {}", profileUser.getId());

        Profile profile = profilesService.deleteBillingAddress(profileUser.getTid(), profileUser.getId(), address.getId());
        List<ProfileBillingAddress> result = profile.getBillingAddresses();

        Integer size = result.size();
        return new PagedSmartBipResponse<>(new PageImpl<ProfileBillingAddress>(result,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public SmartBipResponse<ProfileDeleteResponse> delete(Principal principal, HttpServletRequest request) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        String tid = profileUser.getTid();
        String profileId = profileUser.getId();

        LOGGER.debug("Requested profile delete (archive) from user {}", profileId);

        auditLogger.log(tid, PROFILE_DELETE.get(profileId), principal, request);

        Assert.hasLength(profileId, "Error archiving the profile, missing id");

        profilesService.archive(tid, profileId);

        LOGGER.debug("Archived profile: {}", profileId);

        return new SmartBipResponse<>(new ProfileDeleteResponse(profileId));
    }

    @RequestMapping(value = "/cards/{id}/disable", method = RequestMethod.POST)
    public SmartBipResponse<List<ProfileCard>> disableCard(Principal principal, HttpServletRequest request,
                                                    @PathVariable("id") String id) {

        ProfileUser profileUser = principalResolver.getProfile(principal);
        LOGGER.trace("Updating profile card with id {}", id);

        Profile profile = profilesService.disableCard(profileUser.getTid(), profileUser.getId(), id);
        List<ProfileCard> result = profile.getCards();

        return new SmartBipResponse<>(result);
    }

    //    @RequestMapping(value = "/login/token", method = RequestMethod.GET)
//    public Profile tokenLogin(@RequestParam("token") String token, Principal principal, HttpServletRequest request) {
//
//        LOGGER.trace("Login with token: " + token);
//
//        // Loading principal from the SecurityContextHolder, otherwise is null
//        principal = (principal == null ? SecurityContextHolder.getContext().getAuthentication() : principal);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        log(PROFILE_LOGIN_TOKEN.get(), principal, request);
//
//        // Reloading profile to avoid possible stale information from the authenticated session
//        Profile profile = profilesService.read(profileUser.getTid(), profileUser.getId());
//
//        LOGGER.trace("Profile {} logged in with token", profile.getId());
//
//        return profile;
//    }

//    @RequestMapping(value = "/update", method = RequestMethod.PUT)
//    public Profile update(Principal principal, HttpServletRequest request, @RequestBody Profile updated) {
//
//        LOGGER.debug("Requested profile update with: " + updated);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        log(profileUser.getTid(), PROFILE_UPDATE.get(updated.toString()), principal, request);
//
//        if (isBlank(updated.getId())) {
//            throw new IllegalArgumentException("Profile ID is null");
//        }
//
//        // Override the retailer in request:
//        updated = profilesService.update(profileUser.getTid(), updated);
//
//        LOGGER.debug("Updated profile: " + updated);
//
//        return updated;
//    }
//
//    @RequestMapping(value = "/update/privacy", method = RequestMethod.PUT)
//    public Profile updatePrivacy(Principal principal, HttpServletRequest request, @RequestBody Profile profile) {
//
//        LOGGER.debug("Requested profile update with: " + profile);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        log(profileUser.getTid(), PROFILE_UPDATE_PRIVACY.get(profile.toString()), principal, request);
//
//        if (isBlank(profile.getId())) {
//            throw new IllegalArgumentException("Profile ID is null");
//        }
//
//        try {
//            Profile existingProfile = profilesService.read(profileUser.getTid(), profile.getId());
//
//            // Setting privacy data
//            existingProfile.setAccept1(BooleanUtils.isTrue(profile.getAccept1()));
//            existingProfile.setAccept2(BooleanUtils.isTrue(profile.getAccept2()));
//            existingProfile.setAccept3(BooleanUtils.isTrue(profile.getAccept3()));
//            existingProfile.setAccept4(BooleanUtils.isTrue(profile.getAccept4()));
//
//            return profilesService.update(profileUser.getTid(), profile);
//
//        } catch (EntityNotFoundException e) {
//
//            throw new NotFoundException("Profile #" + profile.getId() + " not found");
//        }
//    }
//
//    @RequestMapping(value = "/password/reset", method = RequestMethod.PUT)
//    @ResponseStatus(HttpStatus.OK)
//    public void resetPassword(@RequestParam("password") String password, HttpServletRequest request, Principal principal) {
//
//        LOGGER.debug("Requested password reset: " + principal);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        // Audit
//        log(profileUser.getTid(), PASSWORD_RESET.get(), principal, request);
//
//        profilesService.updatePassword(profileUser.getTid(), profileUser.getId(), password);
//    }
//
//
//    @RequestMapping(value = "/email", method = RequestMethod.GET)
//    public String email(Principal principal, HttpServletRequest request,
//                        @RequestParam("username") String username) {
//
//        LOGGER.debug("Requested email: " + username);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        // Audit
//        log(profileUser.getTid(), PASSWORD_RESET.get(profileUser.toString()), principal, request);
//
//        Profile profile = profilesService.readByUsername(username);
//
//        return profile.getEmailAddress();
//    }
//
//    @RequestMapping(value = "/reset", method = RequestMethod.POST)
//    @ResponseStatus(HttpStatus.OK)
//    public void resetPassword(Principal principal, HttpServletRequest request,
//                              @RequestParam("username") String username, @RequestParam("password") String password) {
//
//        LOGGER.debug("Requested password reset: " + username);
//
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        // Audit
//        log(profileUser.getTid(), PASSWORD_RESET.get(), principal, request);
//
//        ProfileCredentials credentials = new ProfileCredentials();
//        credentials.setPassword(password);
//
//        profilesService.updatePassword(profileUser.getTid(), profileUser.getId(), password);
//    }
//

//    @RequestMapping(value = "/notifications", method = RequestMethod.GET)
//    public Page<NotificationDto> notifications(@RequestParam(value = "o", defaultValue = DEFAULT_OFFSET) Integer offset,
//                                               @RequestParam(value = "l", defaultValue = DEFAULT_LIMIT) Integer limit,
//                                               Principal principal, HttpServletRequest request) {
//
//        // Audit
//        log(PROFILE_NOTIFICATIONS.get(), principal, request);
//
//        // override retailer and profile ids
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        org.springframework.data.domain.Page<NotificationDto> notifications = notificationsService.readAllByProfile(profileUser.getTid(),
//                profileUser.getId(), null, new PageRequest(offset / limit, limit));
//
//        return new Page<>(notifications.getTotalElements(), offset, limit, notifications.getContent());
//    }
}