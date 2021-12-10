package de.neuefische.elotracking.backend.configuration;

import de.neuefische.elotracking.backend.configuration.CommandAbbreviationsLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
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
            String out = mappings.get(in);
            log.trace(String.format("map %s to %s", in, out));
            return out;
        } else {
            return in;
        }
    }
}
