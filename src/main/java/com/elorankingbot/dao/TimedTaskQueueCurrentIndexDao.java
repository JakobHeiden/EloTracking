package com.elorankingbot.dao;

import com.elorankingbot.model.CurrentIndex;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimedTaskQueueCurrentIndexDao  extends MongoRepository<CurrentIndex, Integer> {
}
