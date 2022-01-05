package com.jjchmielewski.tftarena.entitis.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;

@Document(collation = "Unit")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Unit {

    @Id
    private String id;

    private String character_id;
    private int[] items;
    private int tier;

    public Unit() {
    }

    @Override
    public String toString() {
        return "Unit{" +
                "id='" + id + '\'' +
                ", character_id='" + character_id + '\'' +
                ", items=" + Arrays.toString(items) +
                ", tier=" + tier +
                '}';
    }
}
