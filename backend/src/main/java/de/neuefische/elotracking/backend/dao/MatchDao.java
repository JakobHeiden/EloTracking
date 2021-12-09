package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchDao extends MongoRepository<Match, String> {
}
