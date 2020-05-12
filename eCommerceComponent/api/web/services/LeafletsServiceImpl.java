package com.retapps.smartbip.cms.services;

import com.retapps.smartbip.basis.models.Store;
import com.retapps.smartbip.basis.services.StoresService;
import com.retapps.smartbip.cms.common.models.Leaflet;
import com.retapps.smartbip.cms.common.services.LeafletsService;
import com.retapps.smartbip.cms.entities.LeafletEntity;
import com.retapps.smartbip.cms.repositories.LeafletsRepository;
import com.retapps.smartbip.common.helpers.CalendarHelper;
import com.retapps.smartbip.common.models.ImageSize;
import com.retapps.smartbip.common.services.StorageService;
import com.retapps.smartbip.common.services.View;
import com.retapps.smartbip.common.utils.ResourcesLocationResolver;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.validation.constraints.NotNull;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.retapps.smartbip.cms.mappers.LeafletMapper.*;
import static com.retapps.smartbip.cms.repositories.LeafletsSpecifications.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.jpa.domain.Specifications.where;

@Service
@Validated
@Transactional
public class LeafletsServiceImpl implements LeafletsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeafletsServiceImpl.class);

    private static final int IMAGE_WIDTH_IN_PIXEL = 1000;

    private static final int IMAGE_HEIGHT_IN_PIXEL = 1000;

    @Autowired
    private LeafletsRepository repository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private StoresService storesService;

    @Autowired
    private ResourcesLocationResolver resourcesResolver;

    @Override
    public Page<Leaflet> readAll(@NotNull String tid, View view, PageRequest request) {

        LOGGER.debug("Reading all leaflets for request {} and tenant {}", request, tid);

        Page<LeafletEntity> entities = repository.findAll(where(hasTenant(tid)), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public Leaflet read(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Reading leaflet #{} for tenant {}", id, tid);

        LeafletEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
        }

        LOGGER.debug("Found leaflet {}", entity.getId());

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Leaflet create(@NotNull String tid, @NotNull Leaflet item) throws EntityExistsException {

        LOGGER.debug("Creating leaflet {} for tenant {}", item, tid);

        if (isNotBlank(item.getId())) {
            if (repository.exists(item.getId())) {
                throw new EntityExistsException("Leaflet #" + item.getId() + " already exists for tenant " + tid);
            }
        }

        LeafletEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Leaflet update(@NotNull String tid, @NotNull Leaflet item) throws EntityNotFoundException {

        LOGGER.debug("Updating leaflet {} for tenant {}", item, tid);

        Assert.hasLength(item.getId(), "Invalid id");
        Assert.hasLength(item.getCode(), "Invalid leaflet id");

        if (!repository.exists(item.getId())) {
            throw new EntityNotFoundException("Leaflet #" + item.getId() + " not found for tenant " + tid);
        }

        LeafletEntity entity = repository.save(toEntity(tid, item));

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Leaflet upsert(@NotNull String tid, @NotNull Leaflet item) {

        LOGGER.debug("Upserting leaflet {} for tenant {}", item, tid);

        Calendar cal = CalendarHelper.toCalendar(item.getValidTo());
        cal = CalendarHelper.endOfDay(cal, false);
        item.setValidTo(cal.getTime());

        if (isBlank(item.getId()) || !repository.exists(item.getId())) {
            return create(tid, item);
        } else {
            return update(tid, item);
        }
    }

    @Override
    public void delete(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Deleting leaflet {} for tenant {}", id, tid);

        if (!repository.exists(id)) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
        }

        repository.delete(id);
    }

    @Override
    public void deleteAll(@NotNull String tid) {

        LOGGER.debug("Deleting all leaflets for tenant {}", tid);

        repository.deleteByTid(tid);
    }

    @Override
    public Page<Leaflet> search(@NotNull String tid, @NotNull String query, View view, PageRequest request) {

        LOGGER.debug("Searching all leaflets for query {} and request {} and tenant {}", query, request, tid);

        Specifications<LeafletEntity> specifications = where(hasTenant(tid));

        if (isNotBlank(query)) {
            specifications = specifications.and(likesQuery(query));
        }

        Page<LeafletEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public long count(@NotNull String tid) {

        LOGGER.debug("Counting all leaflets for tenant {}", tid);

        return repository.countByTid(tid);
    }

    @Override
    public Leaflet enable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Enabling leaflet #{} for tenant {}", id, tid);

        LeafletEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found");
        }

        entity.setEnabled(true);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Leaflet disable(@NotNull String tid, @NotNull String id) throws EntityNotFoundException {

        LOGGER.debug("Disabling leaflet #{} for tenant {}", id, tid);

        LeafletEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
        }

        entity.setEnabled(false);

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public List<Leaflet> readValid(@NotNull String tid, View view) {

        LOGGER.debug("Reading valid leaflets for tenant {}", tid);

        Specifications<LeafletEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<LeafletEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public List<Leaflet> readValidByStore(@NotNull String tid, @NotNull String storeId, View view) {

        LOGGER.debug("Reading valid leaflets for tenant {} and store {}", tid, storeId);

        Specifications<LeafletEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(hasStore(storeId));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<LeafletEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public List<Leaflet> readValidByCategory(@NotNull String tid, @NotNull String category, View view) {

        LOGGER.debug("Reading valid leaflets for tenant {} and category {}", tid, category);

        Specifications<LeafletEntity> specifications = where(hasTenant(tid));
        specifications = specifications.and(hasCategory(category));
        specifications = specifications.and(isEnabled());
        specifications = specifications.and(isValid());

        Sort sort = new Sort(Sort.Direction.DESC, "updated");

        List<LeafletEntity> entities = repository.findAll(where(specifications), sort);

        return fromEntities(view, entities);
    }

    @Override
    public Page<Leaflet> search(String tid, String query, Boolean isEnabled, Boolean isValid, Date validFrom, Date validTo,
                                View view, PageRequest request) {

        LOGGER.debug("Searching all leaflets for query {} and request {} and tenant {}", query, request, tid);

        Specifications<LeafletEntity> specifications = where(null);

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

        Page<LeafletEntity> entities = repository.findAll(where(specifications), request);

        return new PageImpl<>(fromEntities(view, entities.getContent()), request, entities.getTotalElements());
    }

    @Override
    public void deleteExpired() {

        LOGGER.debug("Deleting all expired leaflets");

        repository.deleteByValidToLessThanEqual(new Date());
    }

    @Override
    public void deleteExpiredAndImages() {

        LOGGER.debug("Deleting all expired leaflets and images");

        List<LeafletEntity> expired = repository.findAll(where(isExpired()));

        for (LeafletEntity item : expired) {

            // Leaflet cover
            if (StringUtils.isNotBlank(item.getCoverImage())) {
                try {
                    String resourcesFolder = resourcesResolver.getCmsLeafletsPath(item.getTid()).toString();
                    storageService.delete(item.getTid(), resourcesFolder, item.getCoverImage());
                    LOGGER.trace("Deleting cover image {}", item.getCoverImage());

                } catch (IOException e) {
                    LOGGER.warn("Error deleting cover image {}: {}", item.getCoverImage(), ExceptionUtils.getMessage(e));
                }
            }

            // Leaflet images
            if (!CollectionUtils.isEmpty(item.getImages())) {

                for (String image : item.getImages()) {

                    try {
                        String resourcesFolder = resourcesResolver.getCmsLeafletsPath(item.getTid()).toString();
                        storageService.delete(item.getTid(), resourcesFolder, image);
                        LOGGER.trace("Deleting leaflet image {}", image);

                    } catch (IOException e) {
                        LOGGER.warn("Error deleting leaflet image {}: {}", image, ExceptionUtils.getMessage(e));
                    }

                }
            }
            LOGGER.trace("All images deleted for leaflet {} and tenant {}", item.getId(), item.getTid());
        }

        deleteExpired();
    }

    /**
     * Stores the cover image
     */
    @Override
    public Leaflet update(@NotNull String tid, @NotNull Leaflet item, MultipartFile file) throws EntityNotFoundException, IOException {

        try {
            String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            if (isNotBlank(filename)) {
                item.setCoverImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing multipart image {} for leaflet #{}: {}", file.getOriginalFilename(), item.getId(),
                    ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    /**
     * Stores the cover image
     */
    @Override
    public Leaflet update(@NotNull String tid, @NotNull Leaflet item, File file) throws EntityNotFoundException, IOException {

        try {
            String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            if (isNotBlank(filename)) {
                item.setCoverImage(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing image {} for leaflet #{}: {}", file.getName(), item.getId(), ExceptionUtils.getMessage(e));
        }

        return upsert(tid, item);
    }

    @Override
    public Leaflet updatePageImage(@NotNull String tid, @NotNull Leaflet item, File file) throws IOException {

        try {
            String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();

            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            if (isNotBlank(filename) && item.getImages() != null && !item.getImages().contains(filename)) {
                item.getImages().add(filename);
            }

        } catch (IOException e) {
            LOGGER.warn("Error storing page image {} for leaflet #{}: {}", file.getName(), item.getId(),
                    ExceptionUtils.getMessage(e));
        }

        return update(tid, item);
    }

    @Override
    public Leaflet updateWithImages(@NotNull String tid, @NotNull Leaflet item, @NotNull List<MultipartFile> files)
            throws EntityNotFoundException,
            IOException {

        LOGGER.debug("Storing {} images for leaflet {} and tenant {}", files.size(), item.getId(), tid);

        String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();

        if (item.getImages() == null) {
            item.setImages(new ArrayList<String>());
        }

        for (MultipartFile file : files) {
            String filename = storageService.store(file, tid, resourcesFolder,
                    new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));
            if (StringUtils.isNotBlank(filename)) {
                item.getImages().add(filename);
            }
        }

        return update(tid, item);
    }

    @Override
    public Leaflet updateRemovingImage(@NotNull String tid, @NotNull Leaflet item, @NotNull int imageIndex)
            throws EntityNotFoundException, IOException, IndexOutOfBoundsException {

        LOGGER.debug("Removing image at index {} from leaflet {} and tenant {}", imageIndex, item.getId(), tid);

        if (item.getImages() == null) {
            item.setImages(new ArrayList<String>());
        }

        item.getImages().remove(imageIndex);

        return update(tid, item);
    }

    @Override
    public List<Store> readStores(@NotNull String tid, @NotNull String id) {

        LOGGER.debug("Reading stores for leaflet #{} and tenant", id, tid);

        LeafletEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
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
    public Leaflet updateStores(@NotNull String tid, @NotNull String id, @NotNull List<Store> stores) {

        LOGGER.debug("Adding {} stores to leaflet #{} and tenant {}", stores.size(), id, tid);

        LeafletEntity entity = repository.findOne(id);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet #" + id + " not found for tenant " + tid);
        }

        entity.setStores(new ArrayList<String>());

        for (Store store : stores) {
            if (isNotBlank(store.getId()) && !entity.getStores().contains(store.getId())) {
                entity.getStores().add(store.getId());
                LOGGER.trace("Store {} added to leaflet {} for tenant {}", store, id, tid);
            }
        }

        entity = repository.save(entity);

        return fromEntity(View.FULL, entity);
    }

    @Override
    public Leaflet createImagesFromPdf(@NotNull String tid, @NotNull Leaflet item)
            throws RestClientException, IOException, InterruptedException {

        LOGGER.debug("Create images for leaflet {} from PDF {}", item.getId(), item.getPdf());

        // Download PDF from URL:

        String localPdfFilePath = downloadLeafletPDF(tid, item.getId(), item.getPdf());

        // Render PDF pages and save images:

        int pageCount = createLeafletImagesFromPdfFile(tid, item.getId(), localPdfFilePath);

        // Update leaflet cover and images:

        item.setCoverImage(String.format("LEAFLET_%s_%s_001.jpg", tid, item.getId()));

        List<String> images = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            images.add(String.format("LEAFLET_%s_%s_%03d.jpg", tid, item.getId(), i + 1));
        }
        item.setImages(images);

        item = update(tid, item);

        return item;
    }

    @Override
    @Async
    public Leaflet createImagesFromPdf(@NotNull String tid, @NotNull Leaflet item, MultipartFile pdf)
            throws RestClientException, IOException, InterruptedException {

        LOGGER.debug("Create images for leaflet {} from PDF {}", item.getId(), pdf);

        String javaIoTmpdir = System.getProperty("java.io.tmpdir");
        Path tempPdfPath = Paths.get(javaIoTmpdir, pdf.getOriginalFilename());
        pdf.transferTo(tempPdfPath.toFile());
        String localPdfFilePath = tempPdfPath.toString();

        // Render PDF pages and save images:

        int pageCount = createLeafletImagesFromPdfFile(tid, item.getId(), localPdfFilePath);

        // Update leaflet cover and images:

        item.setCoverImage(String.format("LEAFLET_%s_%s_001.jpg", tid, item.getId()));

        List<String> images = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            images.add(String.format("LEAFLET_%s_%s_%03d.jpg", tid, item.getId(), i + 1));
        }
        item.setImages(images);

        item = update(tid, item);

        return item;
    }

    private String downloadLeafletPDF(String tid, String leafletId, String url) throws IOException {

        LOGGER.debug("Downloading PDF {}", url);

        RestTemplate template = new RestTemplate();
        byte[] bytes = template.getForObject(url, byte[].class);
        String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();
        return storageService.store(bytes, tid, String.format("LEAFLET_%s_%s.pdf", tid, leafletId), resourcesFolder);
    }

    private int createLeafletImagesFromPdfFile(String tid, String leafletId, String file) throws IOException {

        LOGGER.debug("Creating images for leaflet {} from PDF file {}", leafletId, file);

        PDDocument document = PDDocument.load(new File(file));
        LOGGER.trace("PDF document {}", document);

        PDFRenderer pdfRenderer = new PDFRenderer(document);
        String javaIoTmpdir = System.getProperty("java.io.tmpdir");

        int pageCounter = 0;
        for (PDPage page : document.getPages()) {

            LOGGER.trace("Rendering page #{}: {}", pageCounter + 1, page);

            BufferedImage bim = pdfRenderer.renderImageWithDPI(pageCounter, 100, ImageType.RGB);
            LOGGER.trace("Rendered page image is: {}", bim);

            Path tempPath = Paths.get(javaIoTmpdir, String.format("LEAFLET_%s_%s_%03d.jpg", tid, leafletId, pageCounter + 1));
            LOGGER.trace("Writing rendered page image in file {}", tempPath);
            FileOutputStream output = new FileOutputStream(tempPath.toFile());
            try {
                ImageIOUtil.writeImage(bim, "jpg", output, 100, 0.60f);
            } finally {
                output.close();
            }

            String resourcesFolder = resourcesResolver.getCmsLeafletsPath(tid).toString();

            storageService.store(tempPath.toFile(), tid, resourcesFolder, new ImageSize(IMAGE_WIDTH_IN_PIXEL, IMAGE_HEIGHT_IN_PIXEL));

            pageCounter++;
        }

        document.close();

        return pageCounter;
    }

    @Override
    public Leaflet readByCode(String tid, String code, View view) throws EntityNotFoundException {

        LOGGER.debug("Reading category by code {} for tenant {}", code, tid);

        LeafletEntity entity = repository.findOneByTidAndCode(tid, code);
        if (entity == null) {
            throw new EntityNotFoundException("Leaflet by code " + code + " not found");
        }

        return fromEntity(view, entity);
    }
}
