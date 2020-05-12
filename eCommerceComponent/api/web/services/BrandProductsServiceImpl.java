package com.retapps.smartbip.cms.services;

import com.retapps.smartbip.cms.common.models.BrandProduct;
import com.retapps.smartbip.cms.common.services.BrandProductsService;
import com.retapps.smartbip.cms.entities.BrandProductEntity;
import com.retapps.smartbip.cms.repositories.BrandProductsRepository;
import com.retapps.smartbip.common.models.ImageSize;
import com.retapps.smartbip.common.services.StorageService;
import com.retapps.smartbip.common.services.View;
import com.retapps.smartbip.common.utils.ResourcesLocationResolver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

import static com.retapps.smartbip.cms.mappers.BrandProductMapper.*;
import static com.retapps.smartbip.cms.repositories.BrandProductsSpecifications.*;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Validated
@Transactional
public class BrandProductsServiceImpl implements BrandProductsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrandProductsServiceImpl.class);

    private static final ImageSize DEFAULT_IMAGE_SIZE = new ImageSize(800, 800);

    @Autowired
    private BrandProductsRepository repository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ResourcesLocationResolver resourcesResolver;

    @Override
    public Page<BrandProduct> readAll(@NotNull String tid, View view, PageRequest request) {

        LOGGER.debug("Reading all brand products for request {} and tenant {}", request, tid);

        Page<BrandProductEntity> entities = repository.findAll(where(hasTenant(tid)), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public BrandProduct read(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Reading brand product #{} for tenant {}", id, tid);

        BrandProductEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Brand product #" + id + " not found for tenant " + tid);
        }

        BrandProduct brandProduct = fromEntity(View.SUMMARY, entity);

        LOGGER.trace("Found brand product {}", brandProduct);

        return brandProduct;
    }

    @Override
    public BrandProduct readByCode(@NotNull String tid, @NotNull String barcode,@NotNull View view) throws EntityNotFoundException {

        LOGGER.debug("Reading brand product by barcode {} for tenant {}", barcode, tid);

        Specifications<BrandProductEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(hasBarcode(barcode));

        BrandProductEntity entity = repository.findOne(specifications);
        if (entity == null) {
            throw new EntityNotFoundException("Brand product by barcode" + barcode + " not found for tenant " + tid);
        }

        return fromEntity(view, entity);
    }

    @Override
    public BrandProduct create(@NotNull String tid, @NotNull BrandProduct item) throws EntityExistsException {

        LOGGER.debug("Creating brand product {}", item);

        Assert.hasLength(item.getBarcode(), "Invalid barcode");

        BrandProductEntity entity = toEntity(tid, item);

        if (repository.existsByTidAndBarcode(tid, item.getBarcode())) {
            throw new EntityExistsException("Brand product " + item.getId() + " already exists for tenant " + tid);
        }

        return fromEntity(View.SUMMARY, repository.save(entity));
    }

    @Override
    public BrandProduct update(@NotNull String tid, @NotNull BrandProduct item) throws EntityNotFoundException {

        LOGGER.debug("Updating brand product {}", item);

        Assert.hasLength(item.getId(), "Invalid id");

        BrandProductEntity entity = toEntity(tid, item);

        if (!repository.exists(item.getId())) {
            throw new EntityNotFoundException("Brand product #" + item.getId() + " not found for tenant " + tid);
        }

        return fromEntity(View.SUMMARY, repository.save(entity));
    }

    @Override
    public BrandProduct upsert(@NotNull String tid, @NotNull BrandProduct item) {

        LOGGER.debug("Upserting brand product {}", item);

        Assert.hasLength(item.getBarcode(), "Invalid barcode");

        if (!repository.existsByTidAndBarcode(tid, item.getBarcode())) {
            return create(tid, item);
        } else {
            return update(tid, item);
        }
    }

    @Override
    public void delete(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Deleting brand product {} for tenant {}", id, tid);

        if (!repository.exists(id)) {
            throw new EntityNotFoundException("Brand product #" + id + " not found for tenant " + tid);
        }

        repository.delete(id);
    }

    @Override
    public void deleteAll(@NotNull String tid) {

        LOGGER.debug("Deleting all brand products for tenant {}", tid);

        repository.deleteByTid(tid);
    }

    @Override
    public Page<BrandProduct> search(@NotNull String tid, @NotNull String query, View view, PageRequest request) {

        LOGGER.debug("Searching all brand products for query {} and request {} and tenant {}", query, request, tid);

        Specifications<BrandProductEntity> specifications = where(hasTenant(tid));

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
            LOGGER.trace("Adding query specification: {}", query);
        }

        Page<BrandProductEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public long count(@NotNull String tid) {

        LOGGER.debug("Counting all brand products for tenant {}", tid);

        return repository.countByTid(tid);
    }

    @Override
    public BrandProduct update(@NotNull String tid, @NotNull BrandProduct item, MultipartFile file) throws EntityNotFoundException, IOException {

        LOGGER.debug("Updating brand product {} with multipart file", item);

        try {
            String resourcesFolder = resourcesResolver.getCmsBrandProductsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder, DEFAULT_IMAGE_SIZE);

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }
        } catch (IOException e) {
            LOGGER.warn("Error storing multipart image {} for brand product #{}: {}",
                    file.getOriginalFilename(), item.getId(), ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public BrandProduct update(@NotNull String tid, @NotNull BrandProduct item, File file) throws EntityNotFoundException, IOException {

        LOGGER.debug("Updating brand product {} with file", item);

        try {
            String resourcesFolder = resourcesResolver.getCmsBrandProductsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder, DEFAULT_IMAGE_SIZE);

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing image {} for brand product #{} : {}", file.getName(), item.getId(), ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }
}
