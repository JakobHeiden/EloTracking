package com.elorankingbot.dao;

import com.elorankingbot.model.Server;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ServerDao extends MongoRepository<Server, Long> {
}
