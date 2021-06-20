package de.neuefische.elotracking.backend.parser;

import de.neuefische.elotracking.backend.configuration.CommandAbbreviationsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommandAbbreviationMapper {

    @Autowired
    CommandAbbreviationsLoader loader;
    Map<String, String> mappings;

    public CommandAbbreviationMapper() {
        mappings = loader.getAbbreviations();
    }

    public String mapIfApplicable(String in) {
        if (mappings.containsKey(in)) {
            return mappings.get(in);
        } else {
            return in;
        }
    }
}
