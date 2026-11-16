package com.dairyfarm.service;

import com.dairyfarm.dto.PhaseTwoDtos;
import com.dairyfarm.exception.NotFoundException;
import com.dairyfarm.model.Farm;
import com.dairyfarm.model.HealthAttachment;
import com.dairyfarm.model.HealthRecord;
import com.dairyfarm.repository.FarmRepository;
import com.dairyfarm.repository.HealthAttachmentRepository;
import com.dairyfarm.repository.HealthRecordRepository;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HealthAttachmentService {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 25_000_000L;
    private final HealthRecordRepository healthRecords;
    private final HealthAttachmentRepository attachments;
    private final FarmRepository farms;
    private final CurrentUserService currentUser;
    private final Path directory;

    public HealthAttachmentService(HealthRecordRepository healthRecords, HealthAttachmentRepository attachments,
                                   FarmRepository farms, CurrentUserService currentUser,
                                   @Value("${app.upload-dir}") String uploadDirectory) {
        this.healthRecords = healthRecords; this.attachments = attachments; this.farms = farms;
        this.currentUser = currentUser;
        this.directory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("health");
    }

    @Transactional
    public PhaseTwoDtos.AttachmentResponse upload(UUID recordUuid, MultipartFile file) throws IOException {
        UUID farmUuid = currentUser.farmUuid();
        HealthRecord record = healthRecords.findByUuidAndFarm_Uuid(recordUuid, farmUuid)
                .orElseThrow(() -> new NotFoundException("Medical record not found."));
        if (file.isEmpty()) throw new IllegalArgumentException("Select a non-empty attachment.");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Attachments must be 10 MB or smaller.");
        byte[] content = file.getBytes();
        String mediaType = detectType(content);
        String originalName = Path.of(file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename())
                .getFileName().toString();
        if (originalName.isBlank() || originalName.length() > 255) throw new IllegalArgumentException("Attachment filename is invalid.");
        String storedName = UUID.randomUUID().toString();
        Files.createDirectories(directory);
        Path target = directory.resolve(storedName).normalize();
        if (!target.getParent().equals(directory)) throw new IllegalArgumentException("Invalid attachment path.");
        Files.write(target, content);
        try {
            HealthAttachment attachment = new HealthAttachment();
            attachment.setFarm(farms.findById(farmUuid).orElseThrow(() -> new NotFoundException("Farm not found.")));
            attachment.setHealthRecord(record);
            attachment.setOriginalName(originalName);
            attachment.setStoredName(storedName);
            attachment.setMediaType(mediaType);
            attachment.setFileSize(content.length);
            return response(attachments.save(attachment));
        } catch (RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PhaseTwoDtos.AttachmentResponse> list(UUID recordUuid) {
        UUID farmUuid = currentUser.farmUuid();
        if (!healthRecords.existsByUuidAndFarm_Uuid(recordUuid, farmUuid))
            throw new NotFoundException("Medical record not found.");
        return attachments.findAllByHealthRecord_UuidAndFarm_UuidOrderByCreatedAtAsc(recordUuid, farmUuid)
                .stream().map(HealthAttachmentService::response).toList();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(UUID recordUuid, UUID attachmentUuid) throws IOException {
        HealthAttachment attachment = attachments.findByUuidAndHealthRecord_UuidAndFarm_Uuid(
                attachmentUuid, recordUuid, currentUser.farmUuid())
                .orElseThrow(() -> new NotFoundException("Attachment not found."));
        Path target = directory.resolve(attachment.getStoredName()).normalize();
        if (!target.getParent().equals(directory) || !Files.isRegularFile(target))
            throw new NotFoundException("Attachment file not found.");
        Resource resource = new ByteArrayResource(Files.readAllBytes(target));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getMediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment()
                                .filename(attachment.getOriginalName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .contentLength(attachment.getFileSize())
                .body(resource);
    }

    private String detectType(byte[] content) throws IOException {
        if (content.length >= 5 && content[0] == '%' && content[1] == 'P' && content[2] == 'D'
                && content[3] == 'F' && content[4] == '-') return MediaType.APPLICATION_PDF_VALUE;
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) throw new IllegalArgumentException("Only JPEG, PNG, and PDF attachments are supported.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Only JPEG, PNG, and PDF attachments are supported.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(java.util.Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png"))
                    throw new IllegalArgumentException("Only JPEG, PNG, and PDF attachments are supported.");
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS) throw new IllegalArgumentException("Image dimensions are too large.");
                return format.equals("png") ? "image/png" : "image/jpeg";
            } finally { reader.dispose(); }
        }
    }

    public static PhaseTwoDtos.AttachmentResponse response(HealthAttachment a) {
        return new PhaseTwoDtos.AttachmentResponse(a.getUuid(), a.getOriginalName(), a.getMediaType(),
                a.getFileSize(), "/api/health/records/" + a.getHealthRecord().getUuid() + "/attachments/" + a.getUuid());
    }
}
