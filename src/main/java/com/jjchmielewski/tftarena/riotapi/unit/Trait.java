package com.jjchmielewski.tftarena.riotapi.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Trait {

    private String id;
    private int num_units;
    private int style;
    private int tier_current;
    private int tier_total;
    private String name;

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
                '}';
    }
}
