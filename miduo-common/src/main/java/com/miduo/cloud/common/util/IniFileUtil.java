package com.miduo.cloud.common.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * INI文件操作工具类
 * 提供读取、写入、删除INI文件节和键值对的功能
 */
public class IniFileUtil {

    /**
     * 读取所有节名称
     *
     * @param filePath INI文件路径
     * @return 节名称列表
     */
    public static List<String> readSections(String filePath) {
        List<String> sections = new ArrayList<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return sections;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[") && line.endsWith("]")) {
                    String section = line.substring(1, line.length() - 1);
                    sections.add(section);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取INI文件失败: " + e.getMessage(), e);
        }

        return sections;
    }

    /**
     * 读取指定节的所有键值对
     *
     * @param filePath INI文件路径
     * @param section  节名称
     * @return 键值对Map
     */
    public static Map<String, String> readSection(String filePath, String section) {
        Map<String, String> properties = new LinkedHashMap<>();
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return properties;
        }

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean inTargetSection = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // 检查是否是节标题
                if (line.startsWith("[") && line.endsWith("]")) {
                    String currentSection = line.substring(1, line.length() - 1);
                    inTargetSection = currentSection.equals(section);
                    continue;
                }

                // 如果在目标节内，读取键值对
                if (inTargetSection && line.contains("=")) {
                    int separatorIndex = line.indexOf("=");
                    String key = line.substring(0, separatorIndex).trim();
                    String value = line.substring(separatorIndex + 1).trim();
                    properties.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("读取INI文件失败: " + e.getMessage(), e);
        }

        return properties;
    }

    /**
     * 写入或更新指定节的键值对
     *
     * @param filePath   INI文件路径
     * @param section    节名称
     * @param properties 键值对Map
     */
    public static void writeSection(String filePath, String section, Map<String, String> properties) {
        Path path = Paths.get(filePath);
        List<String> lines = new ArrayList<>();

        // 如果文件存在，读取现有内容
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                boolean inTargetSection = false;
                boolean sectionFound = false;

                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();

                    // 检查是否是节标题
                    if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                        String currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);

                        // 如果之前在目标节内，现在进入新节，说明目标节结束
                        if (inTargetSection) {
                            // 写入新的键值对
                            for (Map.Entry<String, String> entry : properties.entrySet()) {
                                lines.add(entry.getKey() + "=" + entry.getValue());
                            }
                            lines.add(""); // 添加空行
                            inTargetSection = false;
                        }

                        if (currentSection.equals(section)) {
                            inTargetSection = true;
                            sectionFound = true;
                            lines.add(line);
                        } else {
                            lines.add(line);
                        }
                        continue;
                    }

                    // 如果在目标节内，跳过旧的键值对（将被新值替换）
                    if (inTargetSection) {
                        continue;
                    }

                    lines.add(line);
                }

                // 如果文件结束时还在目标节内，写入新的键值对
                if (inTargetSection) {
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        lines.add(entry.getKey() + "=" + entry.getValue());
                    }
                }

                // 如果节不存在，在文件末尾添加
                if (!sectionFound) {
                    if (!lines.isEmpty() && !lines.get(lines.size() - 1).isEmpty()) {
                        lines.add(""); // 添加空行分隔
                    }
                    lines.add("[" + section + "]");
                    for (Map.Entry<String, String> entry : properties.entrySet()) {
                        lines.add(entry.getKey() + "=" + entry.getValue());
                    }
                }

            } catch (IOException e) {
                throw new RuntimeException("读取INI文件失败: " + e.getMessage(), e);
            }
        } else {
            // 文件不存在，创建新文件
            lines.add("[" + section + "]");
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                lines.add(entry.getKey() + "=" + entry.getValue());
            }
        }

        // 写入文件
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("写入INI文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除指定节
     *
     * @param filePath INI文件路径
     * @param section  节名称
     */
    public static void deleteSection(String filePath, String section) {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            return;
        }

        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            boolean inTargetSection = false;

            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();

                // 检查是否是节标题
                if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                    String currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);

                    if (currentSection.equals(section)) {
                        inTargetSection = true;
                        continue; // 跳过目标节的标题行
                    } else {
                        inTargetSection = false;
                        lines.add(line);
                    }
                    continue;
                }

                // 如果不在目标节内，保留该行
                if (!inTargetSection) {
                    lines.add(line);
                }
            }

            // 移除末尾多余的空行
            while (!lines.isEmpty() && lines.get(lines.size() - 1).trim().isEmpty()) {
                lines.remove(lines.size() - 1);
            }

        } catch (IOException e) {
            throw new RuntimeException("读取INI文件失败: " + e.getMessage(), e);
        }

        // 写回文件
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("写入INI文件失败: " + e.getMessage(), e);
        }
    }

    /**
     * 读取指定节中某个键的值
     *
     * @param filePath INI文件路径
     * @param section  节名称
     * @param key      键名
     * @return 键值，不存在则返回null
     */
    public static String readValue(String filePath, String section, String key) {
        Map<String, String> properties = readSection(filePath, section);
        return properties.get(key);
    }

    /**
     * 写入指定节中某个键的值
     *
     * @param filePath INI文件路径
     * @param section  节名称
     * @param key      键名
     * @param value    键值
     */
    public static void writeValue(String filePath, String section, String key, String value) {
        Map<String, String> properties = readSection(filePath, section);
        properties.put(key, value);
        writeSection(filePath, section, properties);
    }

    /**
     * 删除指定节中某个键
     *
     * @param filePath INI文件路径
     * @param section  节名称
     * @param key      键名
     */
    public static void deleteValue(String filePath, String section, String key) {
        Map<String, String> properties = readSection(filePath, section);
        properties.remove(key);
        writeSection(filePath, section, properties);
    }
}

