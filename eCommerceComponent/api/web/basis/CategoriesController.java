package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.common.config.Properties;
import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.services.CategoriesService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.categories.TreeCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleStores.CATEGORIES;

@RestController
@RequestMapping("/cli/categories")
public class CategoriesController {

    public static final String CATEGORIES_MAX_LEVEL = "4";

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    CategoriesService service;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(value = "", method = RequestMethod.GET)
    public PagedSmartBipResponse<TreeCategory> list(Principal principal, HttpServletRequest request,
                                                    @RequestParam(value = "categoryId", required = false) String categoryId,
                                                    @RequestParam(name = "depth", required = false, defaultValue = CATEGORIES_MAX_LEVEL) Integer maxDepth) {

        String tid = getTid(principal);

        auditLogger.log(tid, CATEGORIES.get(), principal, request);

        List<TreeCategory> list = service.readTree(tid, categoryId, maxDepth);

        Integer size = list.size();

        return new PagedSmartBipResponse<>(new PageImpl<>(list,
                new PageRequest(0, size == 0 ? Properties.DEFAULT_SIZE : size), size.longValue()));
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

}
