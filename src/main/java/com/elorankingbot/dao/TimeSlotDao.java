package com.elorankingbot.dao;

import com.elorankingbot.model.TimeSlot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimeSlotDao extends MongoRepository<TimeSlot, Integer> {
}
