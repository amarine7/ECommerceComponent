package com.retapps.smartbip.api.web.cms;

import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.authentication.common.models.ProfileUser;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.cms.common.models.BrandProduct;
import com.retapps.smartbip.cms.common.services.BrandProductsService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.PRODUCT;
import static com.retapps.smartbip.common.audit.services.AuditService.ModuleCms.PRODUCTS;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_LIMIT;
import static com.retapps.smartbip.common.config.Properties.DEFAULT_PAGE;

@RestController
@RequestMapping("/cli/cms/products")
@PreAuthorize(PrincipalResolver.ROLE_USER)
public class BrandProductsController {

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    BrandProductsService brandProductsService;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<BrandProduct> list(Principal principal, HttpServletRequest request,
                                                    @RequestParam(value = "q", required = false) String query,
                                                    @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                                    @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        // Audit
        auditLogger.log(profileUser.getTid(), PRODUCTS.get(), principal, request);

        Page<BrandProduct> result = brandProductsService.search(profileUser.getTid(), query, View.FULL, new PageRequest(page, size));

        return new PagedSmartBipResponse<>(result);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SmartBipResponse<BrandProduct> fetch(Principal principal, HttpServletRequest request, @PathVariable("id") String id) {

        ProfileUser profileUser = principalResolver.getProfile(principal);

        // Audit
        auditLogger.log(profileUser.getTid(), PRODUCT.get("id=" + id), principal, request);

        return new SmartBipResponse<>(brandProductsService.read(profileUser.getTid(), id));
    }
}
