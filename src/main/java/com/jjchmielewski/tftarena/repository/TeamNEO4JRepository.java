package com.jjchmielewski.tftarena.repository;


import com.jjchmielewski.tftarena.entitis.nodes.Team;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeamNEO4JRepository extends Neo4jRepository<Team, String> {

    @Query("create (n:Team{name: $name})")
    void saveNodes(@Param("name") String name);

    @Query("Match (t1:Team), (t2:Team) where t1.name = $name1 and t2.name=$name2 create (t1)-[r:TeamRelationship{ strength: $strength}]->(t2) ")
    void saveRelationships(@Param("name1") String name1, @Param("name2") String name2, @Param("strength") double strength);

    @Query("MATCH p=(t:Team)-[r:TeamRelationship]->(t2:Team) where (t.name in $teams) and (t2.name in $teams)  return collect(p)")
    List<Team> findStrength( @Param("teams") String[] teams);
}
