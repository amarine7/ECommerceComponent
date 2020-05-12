package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.api.web.basis.requests.CreateMailBoxRequest;
import com.retapps.smartbip.api.web.basis.requests.DeleteMailBoxRequest;
import com.retapps.smartbip.api.web.basis.requests.MailBoxUpdateRequest;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.MailBox;
import com.retapps.smartbip.basis.services.MailBoxesService;
import com.retapps.smartbip.basis.services.ProfilesService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleAuth.MAIL_BOXES;
import static com.retapps.smartbip.common.config.Properties.*;

@RestController
@RequestMapping("/cli/mail/boxes")
public class MailBoxesController {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailBoxesController.class);

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    ProfilesService profilesService;

    @Autowired
    MailBoxesService notificationsService;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<MailBox> list(Principal principal, HttpServletRequest request,
                                               @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                               @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        LOGGER.debug("Listing mail boxes for profile {} and page {} and size {}", profileUser.getId(), page, size);

        auditLogger.log(null, MAIL_BOXES.get(), null, request);
        PageRequest pageRequest = new PageRequest(page, size, Sort.Direction.DESC, "created");

        Page<MailBox> result = notificationsService.readAllByProfileId(profileUser.getTid(), profileUser.getId(), pageRequest);

        return new PagedSmartBipResponse<>(result);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public SmartBipResponse<MailBox> update(Principal principal, HttpServletRequest request,
                                            @PathVariable("id") String id,
                                            @RequestBody @Valid MailBoxUpdateRequest updateRequest) {

        LOGGER.debug("Requested mail box update with: {}", updateRequest);

        ProfileUser profileUser = principalResolver.getProfile(principal);

        MailBox mailBox = notificationsService.read(profileUser.getTid(), id);
        mailBox.setDisplayed(updateRequest.getDisplayed());
        mailBox = notificationsService.update(profileUser.getTid(), mailBox);

        LOGGER.debug("Updated mail box: {}", mailBox);

        return new SmartBipResponse<>(mailBox);
    }

    @RequestMapping(value = "/delete", method = RequestMethod.POST)
    public PagedSmartBipResponse<MailBox> delete(Principal principal, HttpServletRequest request,
                                                 @RequestBody DeleteMailBoxRequest deleteMailBoxRequest) {

        LOGGER.debug("Mail box DELETE with request: {}", deleteMailBoxRequest);

        ProfileUser profileUser = principalResolver.getProfile(principal);

        notificationsService.delete(profileUser.getTid(), deleteMailBoxRequest.getId());

        PageRequest pageRequest = new PageRequest(0, 20, Sort.Direction.DESC, "created");
        Page<MailBox> result = notificationsService.readAllByProfileId(profileUser.getTid(), profileUser.getId(), pageRequest);

        return new PagedSmartBipResponse<>(result);
    }
}
