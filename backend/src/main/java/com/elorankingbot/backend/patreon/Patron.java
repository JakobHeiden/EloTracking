package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.logging.UseToStringForLogging;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "patron")
public class Patron {

    @Id
    @EqualsAndHashCode.Include
    private long userId;

    public Patron(long userId) {
        this.userId = userId;
    }
}
