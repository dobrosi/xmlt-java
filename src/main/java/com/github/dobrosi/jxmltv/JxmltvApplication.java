package com.github.dobrosi.jxmltv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class JxmltvApplication {

	private String pChannelId;

	private String pLine;

	private Map<String, String> ids;

	private Map<String, String> channelNameMap;

	public static void main(String[] args) {
		SpringApplication.run(JxmltvApplication.class, args);
	}

	public JxmltvApplication() {
		start("digi");
		val map = CsvToMapProcessor.getMap();
		for (val key : map.keySet()) {
			channelNameMap = map.get(key);
			start(key);
		}
	}

	private void start(String key) {
		var filePath = "channels.xml";
		var outputFilePath = "channels-" + key + ".xml";
		ids = new HashMap<>();

		try {
			try(var writer = Files.newBufferedWriter(Path.of(outputFilePath))) {
				Files.lines(Path.of(filePath))
					.forEach(line -> {
                        try {
                            parse(writer, line);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
			}
		} catch (IOException e) {
            log.error("Hiba történt a fájl olvasása közben: {}", e.getMessage());
		}
	}

	private void parse(BufferedWriter writer, String line) throws IOException {
		if (pLine != null && line.contains("<display-name>")) {
			String channelName = getWord(line, "<display-name>", "</display-name>");
			ids.put(pChannelId, mapName(channelName));
			write(writer, pLine);
			line = line.replace(channelName, mapName(channelName));
			pLine =	null;
			pChannelId = null;
		} else if (line.startsWith("<channel id=\"")) {
			pChannelId = getWord(line, "<channel id=\"", "\"");
			pLine = line;
			return;
		}
		write(writer, line);
	}

	private String mapName(String channelName) {
		return channelNameMap == null ? channelName : channelNameMap.getOrDefault(channelName, channelName);
	}

	private void write(BufferedWriter writer, String line) throws IOException {
		String replacedLine = line;
		for (String id : ids.keySet()) {
			replacedLine = replacedLine.replace(id, ids.get(id));
		}
		writer.write(replacedLine);
		writer.newLine();
	}

	private String getWord(String line, String d1, String d2) {
		return line.split(d1)[1].split(d2)[0];
	}
}
