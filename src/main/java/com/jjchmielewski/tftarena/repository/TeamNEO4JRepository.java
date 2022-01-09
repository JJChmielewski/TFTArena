package com.jjchmielewski.tftarena.repository;


import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamNEO4JRepository extends Neo4jRepository<Team, String> {

    public Team findByName(String name);

    @Query("MATCH p=(t:Team)-[r:TeamRelationship]->(t2:Team) where (t.name in $enemyTeams) and (t2.name in $enemyTeams)  return collect(p)")
    public List<Team> findStrength( @Param("enemyTeams") String[] enemyTeams);
}
