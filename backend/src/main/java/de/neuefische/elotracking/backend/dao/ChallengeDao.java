package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChallengeDao extends MongoRepository<ChallengeModel, String> {
}
