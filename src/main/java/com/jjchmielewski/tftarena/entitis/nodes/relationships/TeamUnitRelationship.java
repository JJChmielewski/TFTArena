package com.jjchmielewski.tftarena.entitis.nodes.relationships;

import com.jjchmielewski.tftarena.entitis.nodes.UnitNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
public class TeamUnitRelationship {

    @RelationshipId
    private Long id;

    @Property
    private double weight;

    @Property
    private double percentagePlayed;

    @TargetNode
    private UnitNode unit;

    public TeamUnitRelationship() {
    }

    public TeamUnitRelationship(double weight, double percentagePlayed, UnitNode unit) {
        this.weight = weight;
        this.percentagePlayed = percentagePlayed;
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "TeamUnitRelationship{" +
                "id=" + id +
                ", weight=" + weight +
                ", percentagePlayed=" + percentagePlayed +
                ", unit=" + unit +
                '}';
    }
}
