package com.jjchmielewski.tftarena.entitis.nodes;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node
@Getter
@Setter
public class Item {

    @Id
    private int itemId;

    public Item(int itemId) {
        this.itemId = itemId;
    }

    @Override
    public String toString() {
        return "Item{" +
                "itemId=" + itemId +
                '}';
    }
}
