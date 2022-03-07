package com.jjchmielewski.tftarena.entitis.nodes;

import com.jjchmielewski.tftarena.entitis.nodes.relationships.UnitItemRelationship;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node
@Getter
@Setter
public class UnitNode {

    @Id
    private String name;

    @Relationship(type = "UnitItemRelationship", direction = Relationship.Direction.OUTGOING)
    private List<UnitItemRelationship> items;

    public UnitNode(String name) {
        this.name = name;
        this.items = new ArrayList<>();
    }

    public void addItem(UnitItemRelationship item){
        if(items==null)
            items=new ArrayList<>();

        items.add(item);
    }

    @Override
    public String toString() {
        return "UnitNode{" +
                "name='" + name + '\'' +
                '}';
    }
}
