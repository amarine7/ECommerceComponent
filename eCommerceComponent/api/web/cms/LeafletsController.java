package com.retapps.smartbip.api.web.cms;

import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.cms.common.engines.CmsLeafletsImageDecorator;
import com.retapps.smartbip.common.config.Properties;
import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.cms.common.models.Leaflet;
import com.retapps.smartbip.cms.common.services.LeafletsService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.LEAFLET;
import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.LEAFLETS;

@RestController
@RequestMapping("/cli/cms/leaflets")
@PreAuthorize(PrincipalResolver.ROLE_USER)
public class LeafletsController {

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    LeafletsService leafletsService;

    @Autowired
    AuditLogger auditLogger;

    @Autowired
    CmsLeafletsImageDecorator cmsLeafletsImageDecorator;

    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<Leaflet> list(Principal principal, HttpServletRequest request) {

        String tid = getTid(principal);

        // Audit
        auditLogger.log(tid, LEAFLETS.get(), principal, request);

        List<Leaflet> list = leafletsService.readValid(tid, View.FULL);

        decorate(tid, list);

        Integer size = list.size();

        return new PagedSmartBipResponse<>(new PageImpl<>(list,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/store/{id}", method = RequestMethod.GET)
    public PagedSmartBipResponse<Leaflet> listByStore(Principal principal, HttpServletRequest request,
                                                      @PathVariable("id") String id) {

        String tid = getTid(principal);

        // Audit
        auditLogger.log(tid, LEAFLETS.get("storeId=" + id), principal, request);

        List<Leaflet> list = leafletsService.readValidByStore(tid, id, View.FULL);

        decorate(tid, list);

        Integer size = list.size();

        return new PagedSmartBipResponse<>(new PageImpl<>(list,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SmartBipResponse<Leaflet> fetch(Principal principal, HttpServletRequest request,
                                           @PathVariable("id") String id) {

        String tid = getTid(principal);

        // Audit
        auditLogger.log(tid, LEAFLET.get("id=" + id), principal, request);

        Leaflet leaflet = leafletsService.read(tid, id);

        decorate(tid, leaflet);

        return new SmartBipResponse<>(leaflet);
    }

    String getTid(Principal principal) {
        String tid;
        try {
            ProfileUser profileUser = principalResolver.getProfile(principal);
            tid = profileUser.getTid();
        } catch (IllegalArgumentException e) {
            tid = Properties.DEFAULT_TENANT;
        }
        return tid;
    }

    void decorate(String tid, List<Leaflet> list) {

        if (CollectionUtils.isEmpty(list)) {
            return;
        }

        list.forEach(s -> {
            decorate(tid, s);
        });
    }

    void decorate(String tid, Leaflet leaflet) {

        if (leaflet != null) {
            cmsLeafletsImageDecorator.decorate(tid, leaflet);
        }
    }
}
