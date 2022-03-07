package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.Item;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemNeo4JRepository extends Neo4jRepository<Item, Integer> {

    @Query("create (i:Item{ itemId: $itemId})")
    void saveItem(@Param("itemId") int itemId);
}
