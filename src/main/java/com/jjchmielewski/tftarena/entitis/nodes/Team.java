package com.jjchmielewski.tftarena.entitis.nodes;


import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("Team")
@Getter
@Setter
public class Team {

    @Id
    private String name;

    @Relationship(type = "TeamRelationship", direction = Relationship.Direction.OUTGOING)
    private List<TeamRelationship> enemyTeams;

    public Team() {
    }

    public Team(String name) {
        this.name = name;
    }

    public void addEnemyTeams(TeamRelationship team){

        if(this.enemyTeams == null)
            this.enemyTeams=new ArrayList<>();

        enemyTeams.add(team);
    }

    @Override
    public String toString() {
        return "Team{" +
                "name='" + name + '\'' +
                ", enemyTeams=" + enemyTeams +
                '}';
    }


}
