package com.aipichandler.core;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ImageBatchProcessor {

    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<String>(
            Arrays.asList("jpg", "jpeg", "png", "bmp", "webp", "gif")
    );

    static {
        ImageIO.scanForPlugins();
    }

    private final ProcessingConfig config;
    private final Consumer<String> logger;
    private final BooleanSupplier stopRequested;

    public ImageBatchProcessor(ProcessingConfig config, Consumer<String> logger) {
        this(config, logger, new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return false;
            }
        });
    }

    public ImageBatchProcessor(ProcessingConfig config, Consumer<String> logger, BooleanSupplier stopRequested) {
        this.config = config;
        this.logger = logger;
        this.stopRequested = stopRequested;
    }

    public void processFolder(Path inputFolder) throws IOException {
        if (!Files.isDirectory(inputFolder)) {
            throw new IllegalArgumentException("输入路径不是文件夹: " + inputFolder);
        }

        Path outputFolder = inputFolder.resolve(config.outputFolderName());
        Files.createDirectories(outputFolder);
        logger.accept("输出目录: " + outputFolder);

        try (AiSubjectCropper cropper = new AiSubjectCropper(config, logger);
             Stream<Path> files = Files.walk(inputFolder)) {

            Iterator<Path> iterator = files.filter(Files::isRegularFile)
                    .filter(path -> !path.startsWith(outputFolder))
                    .filter(this::isSupportedImage)
                    .iterator();
            while (iterator.hasNext()) {
                if (stopRequested.getAsBoolean() || Thread.currentThread().isInterrupted()) {
                    logger.accept("收到停止指令，已终止后续处理。");
                    break;
                }
                Path path = iterator.next();
                processSingleImage(path, inputFolder, outputFolder, cropper);
            }
        }
    }

    private void processSingleImage(Path file, Path inputFolder, Path outputFolder, AiSubjectCropper cropper) {
        try {
            BufferedImage raw = ImageIO.read(file.toFile());
            if (raw == null) {
                logger.accept("跳过无法读取图片: " + file);
                return;
            }

            BufferedImage jpgReady = AiSubjectCropper.toRgbImage(raw);
            double outputRatio = resolveOutputRatio(jpgReady);
            BufferedImage cropped = cropper.cropMainSubject(jpgReady, outputRatio);

            Path relative = inputFolder.relativize(file);
            Path targetDir = relative.getParent() == null
                    ? outputFolder
                    : outputFolder.resolve(relative.getParent());
            Files.createDirectories(targetDir);

            String targetName = toJpgName(file.getFileName().toString());
            Path target = targetDir.resolve(targetName);
            writeJpeg(cropped, target, config.jpegQuality());
            logger.accept("完成: " + file.getFileName() + " -> " + target);
        } catch (Exception e) {
            logger.accept("处理失败: " + file + "，原因: " + e.getMessage());
        }
    }

    private double resolveOutputRatio(BufferedImage image) {
        double base = config.outputAspectRatio();
        double upperFactor = config.aspectRatioUpperFactor();
        double lowerFactor = config.aspectRatioLowerFactor();
        double minRatio = base * Math.min(upperFactor, lowerFactor);
        double maxRatio = base * Math.max(upperFactor, lowerFactor);
        double sourceRatio = image.getWidth() / (double) image.getHeight();
        if (sourceRatio < minRatio) {
            return minRatio;
        }
        if (sourceRatio > maxRatio) {
            return maxRatio;
        }
        return sourceRatio;
    }

    private boolean isSupportedImage(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.contains(ext);
    }

    private String toJpgName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return fileName + ".jpg";
        }
        return fileName.substring(0, dot) + ".jpg";
    }

    private void writeJpeg(BufferedImage image, Path target, float quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("当前环境没有 JPG 写入器。");
        }

        ImageWriter writer = writers.next();
        ImageWriteParam params = writer.getDefaultWriteParam();
        if (params.canWriteCompressed()) {
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(quality);
        }

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
    }
}
