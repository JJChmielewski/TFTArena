package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.UnitNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UnitNEO4JRepository extends Neo4jRepository<UnitNode, String> {

    @Query("create (n:UnitNode{name: $name})")
    void saveUnit(@Param("name") String name);


}
