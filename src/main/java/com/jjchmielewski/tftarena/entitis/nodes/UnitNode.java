package com.jjchmielewski.tftarena.entitis.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Getter
@Setter
public class UnitNode {

    @Id
    private String name;


    public UnitNode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "UnitNode{" +
                "name='" + name + '\'' +
                '}';
    }
}
