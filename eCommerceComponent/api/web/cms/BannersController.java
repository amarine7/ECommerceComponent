package com.retapps.smartbip.api.web.cms;

import authentication.common.security.PrincipalResolver;
import cms.common.engines.CmsBannersImageDecorator;
import cms.common.models.Banner;
import cms.common.services.BannersService;
import common.audit.AuditLogger;
import common.config.Properties;
import common.services.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;

import static common.audit.services.AuditService.ModuleCms.BANNERS;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@RestController
@RequestMapping("/cli/cms/banners")
@PreAuthorize(PrincipalResolver.ROLE_USER)
public class BannersController {

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    BannersService bannersService;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    CmsBannersImageDecorator cmsBannersImageDecorator;

    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<Banner> list(Principal principal, HttpServletRequest request,
                                              @RequestParam(value = "tid", required = false) String tid,
                                              @RequestParam(value = "category", required = false) String category,
                                              @RequestParam(value = "storeId", required = false) String storeId) {

        tid = principalResolver.getRetailerId(principal, tid);

        // Audit
        auditLogger.log(tid, BANNERS.get(), principal, request);

        List<Banner> list = bannersService.readValid(tid, trimToNull(category), trimToNull(storeId), View.FULL);

        decorate(tid, list);

        Integer size = list.size();
        return new PagedSmartBipResponse<>(new PageImpl<>(list,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    void decorate(String tid, List<Banner> list) {

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        list.forEach(s -> {
            decorate(tid, s);
        });
    }

    void decorate(String tid, Banner banner) {

        if (banner != null) {
            cmsBannersImageDecorator.decorate(tid, banner);
        }
    }
}
