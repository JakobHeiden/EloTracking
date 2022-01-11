package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.CurrentIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimedTaskQueueCurrentIndexDao  extends MongoRepository<CurrentIndex, Integer> {
}
