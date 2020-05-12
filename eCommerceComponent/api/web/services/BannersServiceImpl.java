package com.retapps.smartbip.cms.services;

import com.retapps.smartbip.basis.models.Store;
import com.retapps.smartbip.basis.services.StoresService;
import com.retapps.smartbip.cms.common.models.Banner;
import com.retapps.smartbip.cms.common.services.BannersService;
import com.retapps.smartbip.cms.entities.BannerEntity;
import com.retapps.smartbip.cms.repositories.BannersRepository;
import com.retapps.smartbip.common.models.ImageSize;
import com.retapps.smartbip.common.services.StorageService;
import com.retapps.smartbip.common.services.View;
import com.retapps.smartbip.common.utils.ResourcesLocationResolver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.retapps.smartbip.cms.mappers.BannerMapper.*;
import static com.retapps.smartbip.cms.repositories.BannersSpecifications.*;
import static org.apache.commons.lang3.StringUtils.*;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Validated
@Transactional
public class BannersServiceImpl implements BannersService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BannersServiceImpl.class);

    private static final ImageSize DEFAULT_BANNER_SIZE = new ImageSize(800, 500);

    private static final Map<String, ImageSize> BANNER_SIZES = new HashMap<>();

    @Autowired
    private StoresService storesService;

    static {

        BANNER_SIZES.put("MC18", new ImageSize(480, 660));
        BANNER_SIZES.put("CC5000", new ImageSize(1000, 600));
        BANNER_SIZES.put("WEB", new ImageSize(1920, 485));
        BANNER_SIZES.put("APPS", new ImageSize(480, 660));
    }

    @Autowired
    private BannersRepository repository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ResourcesLocationResolver resourcesResolver;

    @Override
    public Page<Banner> readAll(@NotNull String tid, View view, PageRequest request) {

        LOGGER.debug("Reading all banners for request {} and tenant {}", request, tid);

        Page<BannerEntity> entities = repository.findAll(where(hasTenant(tid)), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public Banner read(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Reading banner #{} for tenant {}", id, tid);

        BannerEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Banner #" + id + " not found for tenant " + tid);
        }

        LOGGER.debug("Found banner {}", entity.getId());

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Banner create(@NotNull String tid, @NotNull Banner item) throws EntityExistsException {

        LOGGER.debug("Creating banner {} for tenant {}", item, tid);

        if (isNotBlank(item.getId())) {
            if (repository.exists(item.getId())) {
                throw new EntityExistsException("Banner #" + item.getId() + " already exists for tenant " + tid);
            }
        }

        BannerEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Banner update(@NotNull String tid, @NotNull Banner item) throws EntityNotFoundException {

        LOGGER.debug("Updating banner {} for tenant {}", item, tid);

        Assert.hasLength(item.getId(), "Invalid id");

        if (!repository.exists(item.getId())) {
            throw new EntityNotFoundException("Banner #" + item.getId() + " not found for tenant " + tid);
        }

        BannerEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Banner upsert(@NotNull String tid, @NotNull Banner item) {

        LOGGER.debug("Upserting banner {} for tenant {}", item, tid);

        if (isBlank(item.getId()) || !repository.exists(item.getId())) {
            return create(tid, item);
        } else {
            return update(tid, item);
        }
    }

    @Override
    public void delete(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Deleting banner #{} for tenant {}", id, tid);

        if (!repository.exists(id)) {
            throw new EntityNotFoundException("Banner #" + id + " not found for tenant " + tid);
        }

        repository.delete(id);
    }

    @Override
    public void deleteAll(@NotNull String tid) {

        LOGGER.debug("Deleting all banners for tenant {}", tid);

        repository.deleteByTid(tid);
    }

    @Override
    public Page<Banner> search(@NotNull String tid, @NotNull String query, View view, PageRequest request) {

        LOGGER.debug("Searching all banners for query {} and request {} and tenant {}", query, request, tid);

        Specifications<BannerEntity> specifications = where(hasTenant(tid));

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
        }

        Page<BannerEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public long count(@NotNull String tid) {

        LOGGER.debug("Counting all banners for tenant {}", tid);

        return repository.countByTid(tid);
    }

    @Override
    public Banner enable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Enabling banner #{} for tenant {}", id, tid);

        BannerEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Banner #" + id + " not found for tenant " + tid);
        }

        entity.setEnabled(true);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Banner disable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Disabling banner {} for tenant {}", id, tid);

        BannerEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Banner #" + id + " not found for tenant " + tid);
        }

        entity.setEnabled(false);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public List<Banner> readValid(@NotNull String tid, View view) {

        LOGGER.debug("Reading valid banners for tenant {}", tid);

        Specifications<BannerEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<BannerEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public List<Banner> readValid(@NotNull String tid, String category, View view) {

        LOGGER.debug("Reading valid banners for category {} and tenant {}", category, tid);

        Specifications<BannerEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        if (isNotBlank(category)) {
            specifications = specifications.and(hasCategory(trimToNull(category)));
        }

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<BannerEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public List<Banner> readValid(@NotNull String tid, String category, String storeId, View view) {

        LOGGER.debug("Reading valid banners for category {} and tenant {}", category, tid);

        Specifications<BannerEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        if (isNotBlank(category)) {
            specifications = specifications.and(hasCategory(trimToNull(category)));
        }

        if (isNotBlank(storeId)) {
            specifications = specifications.and(hasStore(trimToNull(storeId)));
        }

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<BannerEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }


    @Override
    public Page<Banner> search(String tid, String query, Boolean isEnabled, Boolean isValid,
                               Date validFrom, Date validTo, View view, PageRequest request) {

        LOGGER.debug("Searching all banners for query {} and request {} and tenant {}", query, request, tid);

        Specifications<BannerEntity> specifications = where(null);

        if (isNotBlank(tid)) {
            specifications = specifications.and(hasTenant(tid));
        }

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
        }

        if (BooleanUtils.isTrue(isEnabled)) {
            specifications = specifications.and(isEnabled());
        }

        if (BooleanUtils.isTrue(isValid)) {
            specifications = specifications.and(isEnabled());
            specifications = specifications.and(isValid());
        }

        if (validFrom != null) {
            specifications = specifications.and(isValidFrom(validFrom));
        }

        if (validTo != null) {
            specifications = specifications.and(isValidTo(validTo));
        }

        Page<BannerEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public Banner update(@NotNull String tid, @NotNull Banner item, MultipartFile file) throws EntityNotFoundException, IOException {

        try {

            Assert.notNull(item.getCategoryId(), "Invalid category id");

            ImageSize imageSize = BANNER_SIZES.containsKey(item.getCategoryId())
                    ? BANNER_SIZES.get(item.getCategoryId()) : DEFAULT_BANNER_SIZE;

            String resourcesFolder = resourcesResolver.getCmsBannersPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder, imageSize);

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing multipart image {} for banner #{}: {}",
                    file.getOriginalFilename(), item.getId(), ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public Banner update(@NotNull String tid, @NotNull Banner item, File file) throws EntityNotFoundException, IOException {

        try {

            Assert.notNull(item.getCategoryId(), "Invalid category id");

            ImageSize imageSize = BANNER_SIZES.containsKey(item.getCategoryId())
                    ? BANNER_SIZES.get(item.getCategoryId()) : DEFAULT_BANNER_SIZE;

            String resourcesFolder = resourcesResolver.getCmsBannersPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder, imageSize);

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing image {} for banner #{}: {}", file.getName(), item.getId(), ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public List<Store> readStores(@NotNull String tid, @NotNull String id) {

        LOGGER.debug("Reading stores for banner #{} and tenant", id, tid);

        BannerEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Banner #" + id + " not found for tenant " + tid);
        }

        List<Store> stores = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(entity.getStores())) {
            Store store;
            for (String storeId : entity.getStores()) {
                try {
                    store = storesService.read(tid, storeId);
                    stores.add(store);
                } catch (EntityNotFoundException e) {
                    LOGGER.trace("Store {} not found for tenant {}: {}", storeId, tid, ExceptionUtils.getMessage(e));
                }
            }
        }

        LOGGER.trace("Read {} stores for leaflet {} and tenant {}", stores.size(), tid, id);

        return stores;
    }

    @Override
    public Banner updateStores(@NotNull String tid, @NotNull String id, @NotNull List<Store> stores) {

        LOGGER.debug("Adding {} stores to banner #{} and tenant {}", stores.size(), id, tid);

        BannerEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
        }

        entity.setStores(new ArrayList<String>());

        for (Store store : stores) {
            if (isNotBlank(store.getId()) && !entity.getStores().contains(store.getId())) {
                entity.getStores().add(store.getId());
                LOGGER.trace("Store {} added to banner {} for tenant {}", store, id, tid);
            }
        }

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }
}
