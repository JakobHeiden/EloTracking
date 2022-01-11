package com.elorankingbot.backend.dao;

import com.elorankingbot.backend.model.TimeSlot;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TimeSlotDao extends MongoRepository<TimeSlot, Integer> {
}
