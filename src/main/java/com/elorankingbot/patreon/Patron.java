package com.elorankingbot.patreon;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "patron")
public class Patron {

    @Id
    @EqualsAndHashCode.Include
    private long userId;
    private String accessToken;
    private int pledgeInCents;

    public Patron(long userId, String accessToken) {
        this.userId = userId;
        this.accessToken = accessToken;
    }
}
