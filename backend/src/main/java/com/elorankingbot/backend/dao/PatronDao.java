package com.elorankingbot.backend.patreon;

import com.elorankingbot.backend.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface PatronDao extends MongoRepository<Patron, Long> {

}
