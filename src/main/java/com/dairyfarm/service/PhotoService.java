package com.dairyfarm.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoService {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000L;
    private final Path directory;
    public PhotoService(@Value("${app.upload-dir}") String uploadDirectory) {
        directory = Path.of(uploadDirectory).toAbsolutePath().normalize().resolve("animals");
    }
    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Select an image file.");
        if (file.getSize() > MAX_BYTES) throw new IllegalArgumentException("Image must be 5 MB or smaller.");
        byte[] content = file.getBytes();
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) throw new IllegalArgumentException("The selected file is not a supported image.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("Only JPEG and PNG images are supported.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase();
                if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png"))
                    throw new IllegalArgumentException("Only JPEG and PNG images are supported.");
                long pixels = (long) reader.getWidth(0) * reader.getHeight(0);
                if (pixels <= 0 || pixels > MAX_PIXELS)
                    throw new IllegalArgumentException("Image dimensions are too large.");
                String extension = format.equals("png") ? ".png" : ".jpg";
                String filename = UUID.randomUUID() + extension;
                Files.createDirectories(directory);
                Files.write(directory.resolve(filename), content);
                return "/uploads/animals/" + filename;
            } finally {
                reader.dispose();
            }
        }
    }
}
