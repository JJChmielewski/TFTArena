package com.jjchmielewski.tftarena.entitis.nodes.relationships;

import com.jjchmielewski.tftarena.entitis.nodes.Team;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.*;

@RelationshipProperties
@Getter
@Setter
public class TeamRelationship {

    @RelationshipId
    private Long id;

    @Property
    private double strength;

    @TargetNode
    private Team enemyTeam;

    public TeamRelationship() {
    }

    public TeamRelationship(Team enemyTeam) {
        this.enemyTeam = enemyTeam;
    }

    @Override
    public String toString() {
        return "TeamRelationship{" +
                "id=" + id +
                ", strength=" + strength +
                ", enemyTeam=" + enemyTeam.getName()+
                '}';
    }
}
