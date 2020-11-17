package de.neuefische.elotracking.backend.dao;

import de.neuefische.elotracking.backend.model.Dummy;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface MongoDbDao extends PagingAndSortingRepository<Dummy, String> {
    public List<Dummy> findAll();
}
