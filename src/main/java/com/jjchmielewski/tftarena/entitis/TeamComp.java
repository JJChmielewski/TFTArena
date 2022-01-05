package com.jjchmielewski.tftarena.entitis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.entitis.unit.Trait;
import com.jjchmielewski.tftarena.entitis.unit.Unit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collation = "TeamComp")
@Getter
@Setter
public class TeamComp {

    @Id
    private String id;

    private int level;
    private int placement;

    private Trait[] traits;
    private Unit[] units;

    public TeamComp() {
    }

    @Override
    public String toString() {
        return "TeamComp{" +
                "level=" + level +
                ", placement=" + placement +
                ", traits=" + Arrays.toString(traits) +
                ", units=" + Arrays.toString(units) +
                '}';
    }
}
