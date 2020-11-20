package de.neuefische.elotracking.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

//just a placeholder to put something in the DB TODO remove later on
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "dummy")
public class Dummy {
    @Id
    private String data;
}
