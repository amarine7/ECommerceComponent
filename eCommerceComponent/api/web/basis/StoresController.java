package com.retapps.smartbip.api.web.basis;

import com.retapps.smartbip.api.models.PagedSmartBipResponse;
import com.retapps.smartbip.authentication.common.security.PrincipalResolver;
import com.retapps.smartbip.basis.models.AddressCounty;
import com.retapps.smartbip.basis.models.AddressState;
import com.retapps.smartbip.basis.models.Store;
import com.retapps.smartbip.basis.services.StoresService;
import com.retapps.smartbip.common.audit.AuditLogger;
import com.retapps.smartbip.common.models.DeliveryMode;
import com.retapps.smartbip.common.models.responses.SmartBipResponse;
import com.retapps.smartbip.common.services.View;
import com.retapps.smartbip.commons.engines.BasisStoreImageDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

import static com.retapps.smartbip.common.audit.services.AuditService.ModuleStores.STORES;
import static com.retapps.smartbip.common.config.Properties.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@RestController
@RequestMapping("/cli/stores")
public class StoresController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StoresController.class);

    @Autowired
    StoresService storesService;

    @Autowired
    BasisStoreImageDecorator basisStoreImageDecorator;

    @Autowired
    PrincipalResolver principalResolver;

    @Autowired
    AuditLogger auditLogger;

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public SmartBipResponse<Store> fetch(Principal principal, HttpServletRequest request, @PathVariable("id") String id,
                                         @RequestParam(value = "tid", required = false) String tid,
                                         @RequestParam(value = "lat", required = false) Double latitude,
                                         @RequestParam(value = "lon", required = false) Double longitude) {

        tid = principalResolver.getRetailerId(principal, tid);

        auditLogger.log(tid, STORES.get("id=" + id), principal, request);

        Store result = storesService.read(tid, id, latitude, longitude);

        decorate(tid, result);

        return new SmartBipResponse<>(result);
    }

    /**
     * @Deprecated use version with module in the url
     */
    @RequestMapping(method = RequestMethod.GET)
    public PagedSmartBipResponse<Store> list(Principal principal, HttpServletRequest request,
                                             @RequestParam(value = "tid", required = false) String tid,
                                             @RequestParam(value = "q", required = false) String query,
                                             @RequestParam(value = "state", required = false) String state,
                                             @RequestParam(value = "county", required = false) String county,
                                             @RequestParam(value = "city", required = false) String city,
                                             @RequestParam(value = "lat", required = false) Double latitude,
                                             @RequestParam(value = "lon", required = false) Double longitude,
                                             @RequestParam(value = "mss", required = false, defaultValue = "") Boolean enabledMss,
                                             @RequestParam(value = "blists", required = false, defaultValue = "") Boolean enabledBlists,
                                             @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                             @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        tid = principalResolver.getRetailerId(principal, tid);

        auditLogger.log(tid, STORES.get(), principal, request);

        PageRequest pageRequest;
        if (latitude == null && longitude == null) {
            pageRequest = new PageRequest(page, size, Sort.Direction.ASC, "name");
        } else {
            pageRequest = new PageRequest(page, size);
        }

        Page<Store> result = storesService.search(tid, query, state, county, city, latitude, longitude,
                true, true, enabledMss, enabledBlists, View.FULL, pageRequest);

        decorate(tid, result);
        return new PagedSmartBipResponse<>(result);
    }

    @RequestMapping(value = "/shop", method = RequestMethod.GET)
    public PagedSmartBipResponse<Store> listShop(Principal principal, HttpServletRequest request,
                                                 @RequestParam(value = "tid", required = false) String tid,
                                                 @RequestParam(value = "q", required = false) String query,
                                                 @RequestParam(value = "state", required = false) String state,
                                                 @RequestParam(value = "county", required = false) String county,
                                                 @RequestParam(value = "city", required = false) String city,
                                                 @RequestParam(value = "lat", required = false) Double latitude,
                                                 @RequestParam(value = "lon", required = false) Double longitude,
                                                 @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                                 @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        return list(principal, request, tid, query, state, county, city, latitude, longitude, true, null, page, size);
    }

    @RequestMapping(value = "/ecommerce", method = RequestMethod.GET)
    public PagedSmartBipResponse<Store> listEcommerce(Principal principal, HttpServletRequest request,
                                                      @RequestParam(value = "tid", required = false) String tid,
                                                      @RequestParam(value = "q", required = false) String query,
                                                      @RequestParam(value = "state", required = false) String state,
                                                      @RequestParam(value = "county", required = false) String county,
                                                      @RequestParam(value = "city", required = false) String city,
                                                      @RequestParam(value = "delivery", required = false) String delivery,
                                                      @RequestParam(value = "lat", required = false) Double latitude,
                                                      @RequestParam(value = "lon", required = false) Double longitude,
                                                      @RequestParam(value = "p", required = false, defaultValue = DEFAULT_PAGE) int page,
                                                      @RequestParam(value = "s", required = false, defaultValue = DEFAULT_LIMIT) int size) {

        return list(principal, request, tid, query, state, county, city, latitude, longitude, null, true, page, size);
    }

    @RequestMapping(value = "/ecommerce/delivery", method = RequestMethod.GET)
    public List<Store> listEcommerceByDelivery(Principal principal, HttpServletRequest request,
                                               @RequestParam(value = "delivery") String delivery,
                                               @RequestParam(value = "tid", required = false) String tid,
                                               @RequestParam(value = "state", required = false) String state,
                                               @RequestParam(value = "county", required = false) String county,
                                               @RequestParam(value = "city", required = false) String city) {

        tid = isBlank(tid) ? DEFAULT_TENANT : tid;

        auditLogger.log(tid, STORES.get(), principal, request);

        List<Store> result;
        View view = View.FULL;
        Sort sort = new Sort(Sort.Direction.ASC, "name");

        if (isNotBlank(city)) {
            result = storesService.readAllEnabledByDeliveryModeAndCity(tid, DeliveryMode.valueOf(delivery), city, view, sort);
        } else if (isNotBlank(county)) {
            result = storesService.readAllEnabledByDeliveryModeAndCounty(tid, DeliveryMode.valueOf(delivery), county, view, sort);
        } else if (isNotBlank(state)) {
            result = storesService.readAllEnabledByDeliveryModeAndState(tid, DeliveryMode.valueOf(delivery), state, view, sort);
        } else {
            result = Collections.emptyList();
        }

        decorate(tid, result);
        return result;
    }

    @RequestMapping(value = "/states", method = RequestMethod.GET)
    public List<AddressState> listStates(@RequestParam(value = "tid", required = false) String tid) {

        tid = isBlank(tid) ? DEFAULT_TENANT : tid;

        return storesService.readAllStates(tid);
    }


    @RequestMapping(value = "/counties", method = RequestMethod.GET)
    public List<AddressCounty> listCounties(@RequestParam(value = "tid", required = false) String tid,
                                            @RequestParam(value = "stateId", required = false) String state,
                                            @RequestParam(value = "q", required = false) String query) {

        tid = isBlank(tid) ? DEFAULT_TENANT : tid;

        return storesService.findCounties(tid, state, query);
    }

    //    @RequestMapping(value = "/stores/cities", method = RequestMethod.GET)
//    public
//    Page<City> listCities(@RequestParam(value = "state", required = false) String state,
//                          @RequestParam(value = "county", required = false) String county,
//                          @RequestParam(value = "q", required = false) String query,
//                          @RequestParam(value = "o", required = false, defaultValue = DEFAULT_OFFSET) Integer offset,
//                          @RequestParam(value = "l", required = false, defaultValue = DEFAULT_LIMIT) Integer limit,
//                          Principal principal) {
//
//        String tid = principalResolver.getApiRetailerId(principal);
//
//        List<City> cities = service.findCitiesForStoresAndStateAndCounty(retailerId, state, county);
//
//        return new Page<>(cities.size(), 0, cities.size(), cities);
//    }
//
//    @RequestMapping(value = "/stores/{storeId}/customers", method = RequestMethod.POST)
//    @PreAuthorize(PrincipalResolver.ROLE_USER)
//    public StoreProfile createStoreProfile(Principal principal, HttpServletRequest request,
//                                             @PathVariable("storeId") String storeId,
//                                             @RequestBody StoreProfile storeCustomer) {
//
//        // Audit
//        log(STORE_CUSTOMER_CREATE.get("storeId=" + storeId), principal, request);
//
//        String tid = principalResolver.getRetailerId(principal);
//
//        ProfileUser profile = principalResolver.getProfile(principal);
//        storeCustomer.setProfileId(profile.getId());
//        storeCustomer.setCustomerDescription(profile.getUsername());
//
//        storeCustomer.setStoreId(storeId);
//
//        return storeCustomersService.create(tid, storeCustomer);
//    }
//
////    @RequestMapping(value = "/beacon/{storeId}/{beaconId}", method = RequestMethod.GET)
////    @ResponseStatus(HttpStatus.OK)
////    @PreAuthorize(PrincipalResolver.ROLE_USER)
////    public void beacon(Principal principal, HttpServletRequest request,
////                       @PathVariable("storeId") String storeId,
////                       @PathVariable("beaconId") String beaconId) {
////
////        // Audit
////        log(STORE_BEACON_TRIGGERED.get("storeId=" + storeId), principal, request);
////
////        ProfileUser profile = principalResolver.getProfile(principal);
////        String tid = profile.getPk().getTid();
////
////        provider.beacon(retailerId, storeId, profile.getPk().getId(), beaconId);
////
////        // Issue coupons
////        couponingProvider.trigger(retailerId, storeId, profile.getPk().getId(), Coupon.Trigger.BEACON, beaconId);
////
////    }

    void decorate(String tid, Page<Store> result) {

        decorate(tid, result.getContent());
    }

    void decorate(String tid, List<Store> result) {

        if (result == null || CollectionUtils.isEmpty(result)) {
            return;
        }

        result.forEach(s -> {
            decorate(tid, s);
        });
    }

    void decorate(String tid, Store store) {

        if (store != null) {
            basisStoreImageDecorator.decorate(tid, store);
        }
    }
}
