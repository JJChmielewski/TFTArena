package com.jjchmielewski.tftarena.entitis.documents.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.entitis.documents.unit.stats.Effect;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;

@Document(collation = "Trait")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Trait {

    //game data fields
    @Id
    private String id;
    private int num_units;
    private int style;
    private int tier_current;
    private int tier_total;

    //shared

    private String name;

    //community dragon fields
    private String apiName;

    private Effect[] effects;

    public Trait() {
    }

    @Override
    public String toString() {
        return "Trait{" +
                "id='" + id + '\'' +
                ", num_units=" + num_units +
                ", style=" + style +
                ", tier_current=" + tier_current +
                ", tier_total=" + tier_total +
                ", name='" + name + '\'' +
                ", apiName='" + apiName + '\'' +
                ", effects=" + Arrays.toString(effects) +
                '}';
    }
}
