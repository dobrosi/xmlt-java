package com.github.dobrosi.jxmltv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class JxmltvApplication {
	public static final String CHANNEL_PREFIX = "<channel id=\"";
	public static final String DISPLAY_NAME_PREFIX = "<display-name>";
	public static final String ICON_PREFIX = "<icon src=\"";
	public static final String DISPLAY_NAME_POSTFIX = "</display-name>";
	public static final String CHANNEL_POSTFIX = "</channel>";
	public static final String PROGRAMME_PREFIX = "<programme";
	public static final String DIGI = "digi";
	private String theKey;
	private boolean finishedChannelParsing;
	private String pChannelId;
	private String pImgUrl;
	private String pChannelLine;
	private Map<String, String> ids;
	private Map<String, CsvToMapProcessor.Channel> channelMap;

	public static void main(String[] args) {
		SpringApplication.run(JxmltvApplication.class, args);
	}

	public JxmltvApplication() {
		theKey = DIGI;
		start();
		val map = CsvToMapProcessor.getMap();
		for (val key : map.keySet()) {
			theKey = key;
			channelMap = map.get(key);
			start();
		}
	}

	private void start() {
		var filePath = "channels.xml";
		var outputFilePath = "channels-" + theKey + ".xml";
		ids = new HashMap<>();
		finishedChannelParsing = false;

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
		if (!finishedChannelParsing) {
			if (line.contains(CHANNEL_PREFIX)) {
				pChannelId = getChannelId(line);
				pChannelLine = line;
				return;
			} else if (line.contains(DISPLAY_NAME_PREFIX)) {
				if (pChannelLine != null) {
					String displayName = getWord(line, DISPLAY_NAME_PREFIX, DISPLAY_NAME_POSTFIX);
					ids.put(pChannelId, mapId(displayName));
					write(writer, pChannelLine);
					line = line.replace(displayName, mapName(displayName));
					pChannelLine = null;
					pImgUrl = mapImgUrl(displayName);
				} else if (isNotDigi()) {
					return;
				}
			} else if (line.contains(ICON_PREFIX)) {
				if (isNotDigi()) {
					return;
				}
			} else if (line.contains(CHANNEL_POSTFIX)) {
				if (pImgUrl != null) {
					write(writer, "  <icon src=\"" + pImgUrl + "\"/>");
				}
				pChannelId = null;
				pImgUrl = null;
			} else if (line.contains(PROGRAMME_PREFIX)) {
				finishedChannelParsing = true;
			}
		}
		write(writer, line);
	}

	private boolean isNotDigi() {
		return !theKey.equals(DIGI);
	}

	private String mapValue(String displayName, Function<CsvToMapProcessor.Channel, String> getter) {
		return mapValue(displayName, displayName, getter);
	}

	private String mapValue(String displayName, String defaultValue, Function<CsvToMapProcessor.Channel, String> getter) {
		CsvToMapProcessor.Channel channel;
		return channelMap == null || (channel = getChannel(displayName)) == null ? defaultValue : getter.apply(channel);
	}

	private String mapImgUrl(String displayName) {
		return mapValue(displayName, null, CsvToMapProcessor.Channel::getImgUrl);
	}

	private String mapId(String displayName) {
		return mapValue(displayName, CsvToMapProcessor.Channel::getId);
	}

	private String mapName(String displayName) {
		return mapValue(displayName, CsvToMapProcessor.Channel::getName);
	}

	private CsvToMapProcessor.Channel getChannel(final String displayName) {
		return channelMap.get(displayName);
	}

	private void write(BufferedWriter writer, String line) throws IOException {
		String replacedLine = line;
		for (String id : ids.keySet()) {
			replacedLine = replacedLine.replace(id, ids.get(id));
		}
		writer.write(replacedLine);
		writer.newLine();
	}

	private String getChannelId(String line) {
		return getWord(line, CHANNEL_PREFIX, "\"");
	}

	private String getWord(String line, String d1, String d2) {
		return line.split(d1)[1].split(d2)[0];
	}
}
