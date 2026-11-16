package com.dairyfarm.controller;

import com.dairyfarm.service.PhotoService;
import java.io.IOException;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/animals/photos")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class PhotoUploadController {
    private final PhotoService photos;
    public PhotoUploadController(PhotoService photos) { this.photos = photos; }
    @PostMapping
    public Map<String, String> upload(@RequestPart("file") MultipartFile file) throws IOException {
        return Map.of("url", photos.store(file));
    }
}
