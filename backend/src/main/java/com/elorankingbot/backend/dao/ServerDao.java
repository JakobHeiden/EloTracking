package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServerDao extends MongoRepository<Server, Long> {
}
