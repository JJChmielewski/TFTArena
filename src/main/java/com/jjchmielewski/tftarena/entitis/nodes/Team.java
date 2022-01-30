package com.jjchmielewski.tftarena.entitis.nodes;


import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamUnitRelationship;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
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

    @Relationship(type = "TeamUnitRelationship", direction = Relationship.Direction.OUTGOING)
    private List<TeamUnitRelationship> units;

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

    public void addUnit(TeamUnitRelationship teamUnitRelationship){
        if(this.units == null)
            this.units = new ArrayList<>();

        this.units.add(teamUnitRelationship);
    }

    @Override
    public String toString() {
        return "Team{" +
                "name='" + name + '\'' +
                ", enemyTeams=" + enemyTeams +
                '}';
    }


}
