package com.retapps.smartbip.api.web.basis;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cli/checkin")
public class CheckinController {

//    @Autowired
//    private StoreProfilesService storeCustomersService;
//
//    @Autowired
//    private ProfilesService profilesService;
//
////    @Autowired
////    CouponingProvider couponingProvider;
//
//    @Autowired
//    private PrincipalResolver principalResolver;
//
//    @RequestMapping(value = "/checkin/{store}", method = RequestMethod.POST)
//    public
//    @ResponseStatus(HttpStatus.OK)
//    void checkinInStore(Principal principal, HttpServletRequest httpRequest,
//                        @PathVariable(value = "store") String storeId) {
//
//        // Audit:
//        log(STORE_CUSTOMER_CREATE.get("action: CHECKIN, store: " + storeId), principal, httpRequest);
//
//        // Get the already authenticated profile:
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//        Profile profile = profilesService.read(profileUser.getTid(), profileUser.getId());
//
//        // Create the store customer action:
//        StoreProfile customer = new StoreProfile();
//        customer.setStoreId(storeId);
//        customer.setProfileId(profile.getId());
//        customer.setAction(StoreProfile.Action.CHECKIN);
//        customer.setCustomerDescription(profile.getCredentials() != null ? profile.getCredentials().getUsername() : profile.getId());
//        storeCustomersService.create(profileUser.getTid(), customer);
//
////        if (profile.getConfiguration().getEnabledCoupon()) {
////            // Issue coupons
////            couponingProvider.trigger(profile.getPk().getId(), storeId, profile.getPk().getId(), Coupon.Trigger.CHECKIN, null);
////        }
//    }
//
//    @RequestMapping(value = "/checkin/{store}/shoplane/{shoplane}", method = RequestMethod.POST)
//    public
//    @ResponseStatus(HttpStatus.OK)
//    void checkinInShoplane(Principal principal, HttpServletRequest httpRequest,
//                           @PathVariable(value = "store") String storeId,
//                           @PathVariable(value = "shoplane") String shopLane) {
//
//        // Audit:
//        log(STORE_CUSTOMER_CREATE.get("action: SHOPLANE, store: " + storeId + ", shop lane: " + shopLane), principal, httpRequest);
//
//        // Get the already authenticated profile:
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        // Create the store customer action:
//        StoreProfile customer = new StoreProfile();
//        customer.setStoreId(storeId);
//        customer.setProfileId(profileUser.getId());
//        customer.setAction(StoreProfile.Action.SHOPLANE);
//        customer.setCustomerDescription(profileUser.getUsername());
//        customer.setShoplane(shopLane);
//        storeCustomersService.create(profileUser.getTid(), customer);
//    }
//
//    @RequestMapping(value = "/checkout/{store}", method = RequestMethod.POST)
//    public
//    @ResponseStatus(HttpStatus.OK)
//    void checkoutFromStore(Principal principal, HttpServletRequest httpRequest,
//                           @PathVariable(value = "store") String storeId) {
//
//        // Audit:
//        log(STORE_CUSTOMER_CREATE.get("action: CHECKOUT, store: " + storeId), principal, httpRequest);
//
//        // Get the already authenticated profile:
//        ProfileUser profileUser = principalResolver.getProfile(principal);
//
//        // Create the store customer action:
//        StoreProfile customer = new StoreProfile();
//        customer.setStoreId(storeId);
//        customer.setProfileId(profileUser.getId());
//        customer.setAction(StoreProfile.Action.CHECKOUT);
//        customer.setCustomerDescription(profileUser.getUsername());
//        storeCustomersService.create(profileUser.getTid(), customer);
//    }
}
