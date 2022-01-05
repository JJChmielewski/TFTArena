package com.jjchmielewski.tftarena.entitis.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collation = "Trait")
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Trait {

    @Id
    private String id;

    private String name;
    private int num_units;
    private int style;
    private int tier_current;
    private int tier_total;

    public Trait() {
    }

    @Override
    public String toString() {
        return "Trait{" +
                "name='" + name + '\'' +
                ", num_units=" + num_units +
                ", style=" + style +
                ", tier_current=" + tier_current +
                ", tier_total=" + tier_total +
                '}';
    }
}
