package com.jjchmielewski.tftarena.riotapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.riotapi.unit.Trait;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;
import java.util.Comparator;

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

    public Team() {
    }

    public String getTeamName(){

        if(units == null || traits ==null)
            return "No team";

        Arrays.sort(this.traits, new Comparator<>() {
            @Override
            public int compare(Trait o1, Trait o2) {
                if(o1.getNum_units() > o2.getNum_units())
                    return -1;
                else{
                    if(o1.getNum_units() == o2.getNum_units()){
                        if(o1.getStyle() > o2.getStyle()){
                            return -1;
                        }
                        else
                            return 0;
                    }
                    else
                        return 1;
                }
            }
        });

        for(Unit u : this.units)
            u.countOffensiveComponents();



        Arrays.sort(this.units, new Comparator<>() {
            @Override
            public int compare(Unit o1, Unit o2) {
                if(o1.getOffensiveComponentCount() > o2.getOffensiveComponentCount())
                    return -1;
                else{
                    if(o1.getOffensiveComponentCount() == o2.getOffensiveComponentCount()){
                        if(o1.getTier() > o2.getTier()){
                            return -1;
                        }
                        else
                            return 0;
                    }
                    else
                        return 1;
                }
            }
        });


        String teamName ="";
        if(this.units.length > 5 && this.traits.length > 0){
            teamName += this.traits[0].getName() + "_" + this.traits[0].getStyle() +"_"+ this.units[0].getCharacter_id()+"_"+this.units[0].getTier();
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
