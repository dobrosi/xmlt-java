package com.github.dobrosi.jxmltv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class CsvToMapProcessor {
    @Data
    public static class Channel {
        private String originalId;
        private String id;
        private String name;
        private String imgUrl;

        public Channel(final String[] parts) {
            originalId = parts[0].trim();
            id = parts[1].trim();
            String word;
            name = parts.length > 2 && !(word = parts[2].trim()).isBlank() ? word : id;
            imgUrl = parts.length > 3 && !(word = parts[3].trim()).isBlank() ? word : null;
        }
    }

    public static Map<String, Map<String, Channel>> getMap() {
        String directoryPath = "mapper";
        val result = processCsvFiles(directoryPath);
        result.forEach((fileName, fileContentMap) -> {
            System.out.println("File: " + fileName);
            fileContentMap.forEach((key, value) ->
                                       System.out.println("  " + key + " -> " + value));
        });
        return result;
    }

    private static Map<String, Map<String, Channel>> processCsvFiles(String directoryPath) {
        Map<String, Map<String, Channel>> csvDataMap = new HashMap<>();
        File directory = new File(directoryPath);

        if (!directory.exists() || !directory.isDirectory()) {
            System.err.println("Az adott könyvtár nem létezik vagy nem könyvtár: " + directoryPath);
            return csvDataMap;
        }

        File[] files = directory.listFiles((dir, name) -> name.toLowerCase()
            .endsWith(".csv"));
        if (files != null) {
            for (File file : files) {
                String fileNameWithoutExtension = file.getName()
                    .replaceFirst("\\.[^.]+$", "");
                Map<String, Channel> fileContentMap = processCsvFile(file);
                csvDataMap.put(fileNameWithoutExtension, fileContentMap);
            }
        }

        return csvDataMap;
    }

    private static Map<String, Channel> processCsvFile(File file) {
        Map<String, Channel> contentMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 2 && !parts[0].isBlank()) {
                    contentMap.put(parts[0].trim(), new Channel(parts));
                }
            }
        } catch (IOException e) {
            log.error("Hiba történt a fájl feldolgozása közben: {}", file.getName(), e);
        }

        return contentMap;
    }
}