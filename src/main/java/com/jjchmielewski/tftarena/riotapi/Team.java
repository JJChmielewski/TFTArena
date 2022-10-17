package com.jjchmielewski.tftarena.riotapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.riotapi.unit.Trait;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.Collections;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collation = "TeamComp")
@Getter
@Setter
public class Team {

    @Id
    private String id;

    private int level;
    private int placement;

    private Trait[] traits;
    private Unit[] units;

    @Transient
    private double healthLost = 0;

    @Transient
    private boolean isMetaConverted = false;

    public Team() {
    }

    public String getTeamName(){

        if(units == null || traits ==null)
            return "No team";

        Arrays.sort(this.traits, Collections.reverseOrder());

        for(Unit u : this.units)
            u.countOffensiveComponents();


        Arrays.sort(this.units, Collections.reverseOrder());


        String teamName ="";
        if(this.units.length > 5 && this.traits.length > 0){
            teamName += this.traits[0].getName().split("_")[1] + "_" + this.traits[0].getStyle() +"_"+ this.units[0].getCharacter_id().split("_")[1] +"_"+this.units[0].getTier();
        }
        else{
            teamName = "No team";
        }

        return teamName;
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
