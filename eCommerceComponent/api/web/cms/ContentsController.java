package com.retapps.smartbip.api.web.cms;

import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.cms.common.engines.CmsContentsImageDecorator;
import com.retapps.smartbip.cms.common.models.Content;
import com.retapps.smartbip.cms.common.services.ContentsService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.CONTENT;
import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.CONTENTS;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_LIMIT;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_PAGE;

@RestController
@RequestMapping("/cli/cms/contents")
public class ContentsController {

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    ContentsService contentsService;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    CmsContentsImageDecorator cmsContentsImageDecorator;

    @PreAuthorize(PrincipalResolver.ROLE_USER)
    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<Content> list(Principal principal, HttpServletRequest request,
                                               @RequestParam(value = "tid", required = false) String tid,
                                               @RequestParam(value = "q", required = false) String query,
                                               @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                               @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        tid = principalResolver.getRetailerId(principal, tid);

        // Audit
        auditLogger.log(tid, CONTENTS.get(), principal, request);

        Page<Content> result = contentsService.search(tid, query, null, null, true, null,
                null, View.FULL, new PageRequest(page, size, new Sort(Sort.Direction.ASC, "code")));

        decorate(tid, result);

        return new PagedSmartBipResponse<>(result);
    }

    //    @PreAuthorize(PrincipalResolver.ROLE_USER)
    @RequestMapping(value = "/promoted", method = RequestMethod.GET)
    public PagedSmartBipResponse<Content> listPromoted(Principal principal, HttpServletRequest request,
                                                       @RequestParam(value = "tid", required = false) String tid,
                                                       @RequestParam(value = "q", required = false) String query,
                                                       @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                                       @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {
//        String tid = principalResolver.getRetailerId(principal);
        tid = principalResolver.getRetailerId(principal, tid);

        // Audit
//        auditLogger.log(tid, CONTENTS.get("promoted=true"), principal, request);
        auditLogger.log(tid, CONTENTS.get("promoted=true"), null, request);

        Page<Content> result = contentsService.search(tid, query, true, null, true, null, null, View.FULL, new PageRequest(page, size));

        decorate(tid, result);

        return new PagedSmartBipResponse<>(result);
    }

    @PreAuthorize(PrincipalResolver.ROLE_USER)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SmartBipResponse<Content> fetch(Principal principal, HttpServletRequest request,
                                           @RequestParam(value = "tid", required = false) String tid,
                                           @PathVariable("id") String id) {

        tid = principalResolver.getRetailerId(principal, tid);

        // Audit
        auditLogger.log(tid, CONTENT.get("id=" + id), principal, request);

        Content result = contentsService.read(tid, id);

        decorate(tid, result);

        return new SmartBipResponse<>(result);
    }

    void decorate(String tid, Page<Content> result) {

        if (result == null || CollectionUtils.isEmpty(result.getContent())) {
            return;
        }

        result.getContent().forEach(s -> {
            decorate(tid, s);
        });
    }

    void decorate(String tid, Content content) {

        if (content != null) {
            cmsContentsImageDecorator.decorate(tid, content);
        }
    }
}
