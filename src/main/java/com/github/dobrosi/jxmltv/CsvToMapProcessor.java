package com.github.dobrosi.jxmltv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CsvToMapProcessor {
    public static Map<String, Map<String, String>> getMap() {
        String directoryPath = "mapper";
        Map<String, Map<String, String>> result = processCsvFiles(directoryPath);
        result.forEach((fileName, fileContentMap) -> {
            System.out.println("File: " + fileName);
            fileContentMap.forEach((key, value) ->
                                       System.out.println("  " + key + " -> " + value));
        });
        return result;
    }

    private static Map<String, Map<String, String>> processCsvFiles(String directoryPath) {
        Map<String, Map<String, String>> csvDataMap = new HashMap<>();
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
                Map<String, String> fileContentMap = processCsvFile(file);
                csvDataMap.put(fileNameWithoutExtension, fileContentMap);
            }
        }

        return csvDataMap;
    }

    private static Map<String, String> processCsvFile(File file) {
        Map<String, String> contentMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",", -1);
                if (parts.length >= 2 && !parts[0].isBlank()) {
                    contentMap.put(parts[0].trim(), parts[1].trim());
                }
            }
        } catch (IOException e) {
            log.error("Hiba történt a fájl feldolgozása közben: {}", file.getName(), e);
        }

        return contentMap;
    }
}