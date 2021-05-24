package de.neuefische.elotracking.backend.configuration;

import org.springframework.boot.json.BasicJsonParser;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

@Component
public class CommandAbbreviationsLoader {

    public static Map<String, String> getAbbreviations() {
        String mappingsAsJson = "{}";
        try {
            File resource = new ClassPathResource("command-abbreviations.json").getFile();
            mappingsAsJson = new String(Files.readAllBytes(resource.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> mappingsStringToObject = new BasicJsonParser().parseMap(mappingsAsJson);
        Map<String, String> abbreviations = new HashMap<>();
        for (Map.Entry<String, Object> mapping : mappingsStringToObject.entrySet()) {
            abbreviations.put(mapping.getKey(), (String) mapping.getValue());
        }
        return abbreviations;
    }
}
