package com.miduo.cloud.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 1 号机 T_Code 同步游标持久化（上次已同步的最大 SerialNo）。
 * 文件：user.dir / shiwan-m2-m1-sync-cursor.json
 */
public final class M1SyncCursorStore {

    private static final String CURSOR_FILE_NAME = "shiwan-m2-m1-sync-cursor.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Path getCursorPath() {
        return Paths.get(System.getProperty("user.dir", "."), CURSOR_FILE_NAME);
    }

    public static Long loadLastSyncedSerialNo() {
        Path path = getCursorPath();
        if (!Files.isRegularFile(path)) {
            return null;
        }
        try {
            String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            CursorDto dto = MAPPER.readValue(json, CursorDto.class);
            return dto != null ? dto.getLastSyncedM1SerialNo() : null;
        } catch (IOException e) {
            return null;
        }
    }

    public static void saveLastSyncedSerialNo(Long serialNo) {
        if (serialNo == null) return;
        Path path = getCursorPath();
        try {
            CursorDto dto = new CursorDto();
            dto.setLastSyncedM1SerialNo(serialNo);
            String json = MAPPER.writeValueAsString(dto);
            Files.write(path, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ignored) {
            // best effort
        }
    }

    @SuppressWarnings("unused")
    public static class CursorDto {
        private Long lastSyncedM1SerialNo;
        public Long getLastSyncedM1SerialNo() { return lastSyncedM1SerialNo; }
        public void setLastSyncedM1SerialNo(Long lastSyncedM1SerialNo) { this.lastSyncedM1SerialNo = lastSyncedM1SerialNo; }
    }
}
