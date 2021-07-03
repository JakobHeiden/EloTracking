package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.ChallengeModel;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChallengeDao extends MongoRepository<ChallengeModel, String> {// TODO mongodb sinnvoll aufteilen...?
    List<ChallengeModel> findAllByAcceptorId(String acceptorId);
    List<ChallengeModel> findAllByChallengerId(String challengerId);
}
