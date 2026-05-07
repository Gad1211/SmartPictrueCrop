package com.aipichandler.core;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public final class ModelConfigLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_CONFIG_FILE = "modelConfig.json";

    private ModelConfigLoader() {
    }

    public static Path resolveDefaultConfigPath() {
        return resolveAppDirectory().resolve(DEFAULT_CONFIG_FILE);
    }

    public static List<ModelOption> load(Path configPath) throws IOException {
        Path appDir = resolveAppDirectory();
        Path configDir = configPath.getParent() == null ? appDir : configPath.getParent();
        ConfigFile config;
        if (Files.exists(configPath)) {
            config = MAPPER.readValue(configPath.toFile(), ConfigFile.class);
        } else {
            try (InputStream stream = ModelConfigLoader.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE)) {
                if (stream == null) {
                    throw new IOException("模型配置文件不存在: " + configPath);
                }
                config = MAPPER.readValue(stream, ConfigFile.class);
            }
        }
        if (config.models == null || config.models.isEmpty()) {
            throw new IOException("模型配置文件中 models 不能为空。");
        }

        List<ModelOption> result = new ArrayList<>();
        for (ModelItem item : config.models) {
            if (item == null) {
                continue;
            }
            String name = trim(item.name);
            String modelUrl = resolveModelUrl(trim(item.modelUrl), appDir, configDir);
            String engine = trim(item.engine);
            if (name.isEmpty() || modelUrl.isEmpty() || engine.isEmpty()) {
                throw new IOException("模型配置项缺少 name/modelUrl/engine: " + item);
            }
            result.add(new ModelOption(name, modelUrl, engine));
        }
        if (result.isEmpty()) {
            throw new IOException("模型配置文件中没有有效模型。");
        }
        return result;
    }

    private static String resolveModelUrl(String rawModelUrl, Path appDir, Path configDir) {
        if (rawModelUrl.isEmpty()) {
            return rawModelUrl;
        }
        String expanded = rawModelUrl
                .replace("${appDir}", appDir.toString())
                .replace("${configDir}", configDir.toString());
        if (expanded.startsWith("http://") || expanded.startsWith("https://")) {
            return expanded;
        }
        Path modelPath = Paths.get(expanded);
        if (!modelPath.isAbsolute()) {
            modelPath = configDir.resolve(modelPath);
        }
        return modelPath.normalize().toString();
    }

    private static Path resolveAppDirectory() {
        try {
            Path location = Paths.get(ModelConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.getParent();
            }
            return location;
        } catch (URISyntaxException | IllegalArgumentException e) {
            return Paths.get(System.getProperty("user.dir"));
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class ModelOption {
        private final String name;
        private final String modelUrl;
        private final String engine;

        public ModelOption(String name, String modelUrl, String engine) {
            this.name = name;
            this.modelUrl = modelUrl;
            this.engine = engine;
        }

        public String name() {
            return name;
        }

        public String modelUrl() {
            return modelUrl;
        }

        public String engine() {
            return engine;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final class ConfigFile {
        public List<ModelItem> models;
    }

    static final class ModelItem {
        public String name;
        public String modelUrl;
        public String engine;

        @Override
        public String toString() {
            return "ModelItem{name='" + name + "', modelUrl='" + modelUrl + "', engine='" + engine + "'}";
        }
    }
}
