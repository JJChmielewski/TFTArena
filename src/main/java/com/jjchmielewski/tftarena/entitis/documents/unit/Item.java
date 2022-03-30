package com.jjchmielewski.tftarena.entitis.documents.unit;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class Item {

    private String name;
    private int id;
    private int[] from;

    public Item() {
    }

    @Override
    public String toString() {
        return "Item{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", from=" + Arrays.toString(from) +
                '}';
    }
}
