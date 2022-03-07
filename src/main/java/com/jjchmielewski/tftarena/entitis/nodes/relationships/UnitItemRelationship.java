package com.jjchmielewski.tftarena.entitis.nodes.relationships;

import com.jjchmielewski.tftarena.entitis.nodes.Item;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
public class UnitItemRelationship {

    @RelationshipId
    private Long id;

    @Property
    private int timesPlayed;

    @TargetNode
    private Item item;

    public UnitItemRelationship(int timesPlayed, Item item) {
        this.timesPlayed = timesPlayed;
        this.item = item;
    }

    @Override
    public String toString() {
        return "UnitItemRelationship{" +
                "id=" + id +
                ", timesPlayed=" + timesPlayed +
                ", item=" + item +
                '}';
    }
}
