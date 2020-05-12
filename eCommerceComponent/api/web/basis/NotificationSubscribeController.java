package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.web.basis.requests.NotificationSubscriptionRequest;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.exceptions.NotificationException;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.notifications.services.NotificationSubscriptionService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.NOTIFICATION_SUBSCRIBE;
import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.NOTIFICATION_UNSUBSCRIBE;

/**
 * A controller exposing the API to subscribe push notification service for an user
 */
@RestController
@RequestMapping("/api/notification/subscribe/v2")
public class NotificationSubscribeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationSubscribeController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    NotificationSubscriptionService notificationSubscriptionService;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<SmartBipResponse<String>> subscribe(Principal principal, HttpServletRequest request,
                                                              @RequestBody NotificationSubscriptionRequest notificationSubscriptionRequest) {

        ProfileUser profile = principalResolver.getProfile(principal);

        auditLogger.log(profile.getTid(), NOTIFICATION_SUBSCRIBE.get(principal.toString()), principal, request);

        // Subscribing to push notification service
        try {
            notificationSubscriptionService.subscribeOperator(profile.getTid(), profile.getId(),
                    notificationSubscriptionRequest.getRegistrationId());
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (NotificationException e) {
            LOGGER.trace("Error subscribing profile {} for request {}: {}", profile, request, ExceptionUtils.getMessage(e));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/{registrationId}", method = RequestMethod.DELETE)
    public ResponseEntity<SmartBipResponse<String>> unsubscribe(Principal principal, HttpServletRequest request,
                                                                @PathVariable(value = "registrationId") String registrationId) {

        ProfileUser profile = principalResolver.getProfile(principal);

        auditLogger.log(profile.getTid(), NOTIFICATION_UNSUBSCRIBE.get(principal.toString()), principal, request);

        // Unsubscribing from push notification service
        try {
            notificationSubscriptionService.unsubscribeProfile(profile.getTid(),
                    profile.getId(), registrationId);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (NotificationException e) {

            LOGGER.trace("Error subscribing profile {} for request {}: {}", profile, request, ExceptionUtils.getMessage(e));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}