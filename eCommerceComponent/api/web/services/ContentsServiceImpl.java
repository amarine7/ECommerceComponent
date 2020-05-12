package com.retapps.smartbip.cms.services;

import com.retapps.smartbip.cms.common.models.Content;
import com.retapps.smartbip.cms.common.services.ContentsService;
import com.retapps.smartbip.cms.entities.ContentEntity;
import com.retapps.smartbip.cms.repositories.ContentsRepository;
import com.retapps.smartbip.common.models.ImageSize;
import com.retapps.smartbip.common.services.StorageService;
import com.retapps.smartbip.common.services.View;
import com.retapps.smartbip.common.utils.ResourcesLocationResolver;
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
import java.util.Date;
import java.util.List;

import static com.retapps.smartbip.cms.mappers.ContentMapper.*;
import static com.retapps.smartbip.cms.repositories.ContentsSpecifications.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Validated
@Transactional
public class ContentsServiceImpl implements ContentsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentsServiceImpl.class);

    private static final int PROMOTED_MAX_SIZE = 6;

    private static final int IMAGE_WIDTH_IN_PIXEL = 800;

    private static final int IMAGE_HEIGHT_IN_PIXEL = 500;

    @Autowired
    private ContentsRepository repository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private ResourcesLocationResolver resourcesResolver;

    @Override
    public Page<Content> readAll(@NotNull String tid, View view, PageRequest request) {

        LOGGER.debug("Reading all contents for request {} and tenant {}", request, tid);

        Page<ContentEntity> entities = repository.findAll(where(hasTenant(tid)), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public Content read(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Reading content #{} for tenant {}", id, tid);

        ContentEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Content #" + id + " not found for tenant " + tid);
        }

        LOGGER.debug("Found content {}", entity.getId());

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Content create(@NotNull String tid, @NotNull Content item) throws EntityExistsException {

        LOGGER.debug("Creating content {} for tenant {}", item, tid);

        if (isNotBlank(item.getId())) {
            if (repository.exists(item.getId())) {
                throw new EntityExistsException("Content #" + item.getId() + " already exists for tenant " + tid);
            }
        }

        ContentEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Content update(@NotNull String tid, @NotNull Content item) throws EntityNotFoundException {

        LOGGER.debug("Updating content {} for tenant {}", item, tid);

        Assert.hasLength(item.getId(), "Invalid id");

        if (!repository.exists(item.getId())) {
            throw new EntityNotFoundException("Content #" + item.getId() + " not found for tenant " + tid);
        }

        ContentEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Content upsert(@NotNull String tid, @NotNull Content item) throws EntityNotFoundException {

        LOGGER.debug("Upserting content {} for tenant {}", item, tid);

        if (isBlank(item.getId()) || !repository.exists(item.getId())) {
            return create(tid, item);
        } else {
            return update(tid, item);
        }
    }

    @Override
    public void delete(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Deleting content #{} for tenant {}", id, tid);

        if (!repository.exists(id)) {
            throw new EntityNotFoundException("Content #" + id + " not found for tenant " + tid);
        }

        repository.delete(id);
    }

    @Override
    public void deleteAll(@NotNull String tid) {

        LOGGER.debug("Deleting all contents for tenant {}", tid);

        repository.deleteByTid(tid);
    }

    @Override
    public Page<Content> search(@NotNull String tid, @NotNull String query, View view, PageRequest request) {

        LOGGER.debug("Searching all contents for query {} and request {} and tenant {}", query, request, tid);

        Specifications<ContentEntity> specifications = where(hasTenant(tid));

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
        }

        Page<ContentEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public long count(@NotNull String tid) {

        LOGGER.debug("Counting all contents for tenant {}", tid);

        return repository.countByTid(tid);
    }

    @Override
    public Content enable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Enabling content #{} for tenant {}", id, tid);

        ContentEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Content #" + id + " not found for tenant " + tid);
        }

        entity.setEnabled(true);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Content disable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Disabling content #{} for tenant {}", id, tid);

        ContentEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Content #" + id + " not found for tenant " + tid);
        }

        entity.setEnabled(false);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public List<Content> readValid(@NotNull String tid, View view) {

        LOGGER.debug("Reading valid contents for tenant {}", tid);

        Specifications<ContentEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<ContentEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public List<Content> readValidAndPromoted(@NotNull String tid, View view) {

        LOGGER.debug("Reading valid and promoted contents for tenant {}", tid);

        Specifications<ContentEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isPromoted());
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        PageRequest request = new PageRequest(0, PROMOTED_MAX_SIZE, new Sort(Sort.Direction.DESC, "updated"));

        Page<ContentEntity> entities = repository.findAll(where(specifications), request);

        return fromEntities(view, entities.getContent());
    }

    @Override
    public Page<Content> search(String tid, String query, Boolean isPromoted, Boolean isEnabled, Boolean isValid,
                                Date validFrom, Date validTo, View view, PageRequest request) {

        LOGGER.debug("Searching all contents for query {} and request {} and tenant {}", query, request, tid);

        Specifications<ContentEntity> specifications = where(null);

        if (isNotBlank(tid)) {
            specifications = specifications.and(hasTenant(tid));
        }

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
        }

        if (BooleanUtils.isTrue(isPromoted)) {
            specifications = specifications.and(isPromoted());
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

        Page<ContentEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public void deleteExpired() {

        LOGGER.debug("Deleting expired contents for all tenants");

        repository.deleteByValidToLessThanEqual(new Date());
    }

    @Override
    public int deleteExpiredAndImages() {

        LOGGER.debug("Deleting all expired promotions and related media");

        Specifications<ContentEntity> specifications = where(isExpired());

        int counter = 0;
        for (ContentEntity content : repository.findAll(specifications)) {
            try {
                String resourcesFolder = resourcesResolver.getCmsContentsPath(content.getTid()).toString();
                if (isNotBlank(content.getImage())) {
                    try {
                        LOGGER.trace("About to delete media for content {} and tenant {}", content.getId(), content.getTid());
                        storageService.delete(content.getTid(), resourcesFolder, content.getImage());
                    } catch (Exception e) {
                        LOGGER.debug("Error deleting media for content {} and tenant {}", ExceptionUtils.getMessage(e));
                    }
                }

                LOGGER.trace("About to delete content {} for tenant {}", content.getId(), content.getTid());
                repository.delete(content);
                counter++;
            } catch (Exception e) {
                LOGGER.warn("Error deleting expired content {}", content.getId(), e);
            }
        }

        LOGGER.trace("Deleted {} expired contents", counter);
        return counter;
    }

    @Override
    public Content update(@NotNull String tid, @NotNull Content item, MultipartFile file) throws EntityNotFoundException, IOException {

        try {
            String resourcesFolder = resourcesResolver.getCmsContentsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }
        } catch (IOException e) {
            LOGGER.warn("Error storing multipart image {} for content {} and tenant {}: {}",
                    file.getOriginalFilename(), item.getId(), tid, ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public Content update(@NotNull String tid, @NotNull Content item, File file) throws EntityNotFoundException, IOException {

        try {
            String resourcesFolder = resourcesResolver.getCmsContentsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            if (StringUtils.isNotBlank(filename)) {
                item.setImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing image {} for content {} and tenant {}: {}", file.getName(), item.getId(), tid,
                    ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public Content readByCode(String tid, String code, View view) throws EntityNotFoundException {

        LOGGER.debug("Reading content by code {} for tenant {}", code, tid);

        ContentEntity entity = repository.findOneByTidAndCode(tid, code);
        if (entity == null) {
            throw new EntityNotFoundException("Content by code " + code + " not found");
        }

        return fromEntity(view, entity);
    }
}
