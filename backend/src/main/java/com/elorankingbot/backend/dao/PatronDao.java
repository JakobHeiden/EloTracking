package com.elorankingbot.backend.patreon;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatronDao extends MongoRepository<Patron, Long> {

}
