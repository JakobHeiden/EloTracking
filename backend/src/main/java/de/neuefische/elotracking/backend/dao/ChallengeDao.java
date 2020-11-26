package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Challenge;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChallengeDao extends MongoRepository<Challenge, String> {
}
