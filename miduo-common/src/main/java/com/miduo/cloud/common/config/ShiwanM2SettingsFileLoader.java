package com.miduo.cloud.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 后端读取石湾 2 号机系统设置 JSON 文件（与前端 shiwan-m2-settings.json 同结构）。
 */
public final class ShiwanM2SettingsFileLoader {

    private static final String DEFAULT_FILE_NAME = "shiwan-m2-settings.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 默认路径：user.dir / shiwan-m2-settings.json
     */
    public static Path getDefaultPath() {
        return Paths.get(System.getProperty("user.dir", "."), DEFAULT_FILE_NAME);
    }

    /**
     * 从默认路径加载；文件不存在或解析失败返回 null。
     */
    public static ShiwanM2SettingsDto load() {
        return load(getDefaultPath());
    }

    /**
     * 从指定路径加载；文件不存在或解析失败返回 null。
     */
    public static ShiwanM2SettingsDto load(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return null;
        }
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            return MAPPER.readValue(json, ShiwanM2SettingsDto.class);
        } catch (IOException e) {
            return null;
        }
    }
}
