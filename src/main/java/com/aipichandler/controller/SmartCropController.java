package com.aipichandler.controller;

import com.aipichandler.core.AiSubjectCropper;
import com.aipichandler.core.ProcessingConfig;
import com.aipichandler.model.CropAspectRatio;
import com.aipichandler.model.CropResult;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Example Spring Boot controller for local smart crop API.
 */
@RestController
public class SmartCropController {

    private final AiSubjectCropper cropper;

    public SmartCropController() {
        String defaultModel = Path.of("models", "yolo11n.onnx").toAbsolutePath().toString();
        String modelPath = System.getenv().getOrDefault("AIPICHANDLER_YOLO_MODEL", defaultModel);
        ProcessingConfig config = new ProcessingConfig(
                0.15,
                "output",
                0.9f,
                "YOLO11n ONNX",
                modelPath,
                "ONNXRuntime",
                ""
        );
        this.cropper = new AiSubjectCropper(config, text -> {
        });
    }

    @PostMapping(path = "/api/crop/smart", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> smartCrop(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "ratio", defaultValue = "1:1") String ratio
    ) throws IOException {
        BufferedImage source = ImageIO.read(file.getInputStream());
        if (source == null) {
            return ResponseEntity.badRequest().body("Invalid image data".getBytes());
        }
        BufferedImage rgb = AiSubjectCropper.toRgbImage(source);
        CropResult result = cropper.cropMainSubject(rgb, CropAspectRatio.fromText(ratio));
        byte[] bytes = encodeJpeg(result.croppedImage());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"smart-crop.jpg\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(bytes);
    }

    private byte[] encodeJpeg(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
