package de.neuefische.elotracking.backend.parser;

import de.neuefische.elotracking.backend.configuration.CommandAbbreviationsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CommandAbbreviatonMapper {

    @Autowired
    CommandAbbreviationsLoader loader;
    Map<String, String> mappings;

    public CommandAbbreviatonMapper() {
        mappings = loader.getAbbreviations();
    }

    public String mapIfApplicable(String in) {
        System.out.println(in);
        if (mappings.containsKey(in)) {
            return mappings.get(in);
        } else {
            return in;
        }
    }
}
