package com.jjchmielewski.tftarena.entitis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.entitis.unit.Trait;
import com.jjchmielewski.tftarena.entitis.unit.Unit;
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
public class TeamComp {

    @Id
    private String id;

    private int level;
    private int placement;

    private Trait[] traits;
    private Unit[] units;

    public TeamComp() {
    }

    public String getTeamName(){

        Arrays.sort(this.traits, new Comparator<Trait>() {
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
                            return 1;
                    }
                    else
                        return 1;
                }
            }
        });

        String teamName = new String();

        if(this.traits.length > 1){
            String[] unitNameSplit = this.traits[0].getName().split("_");

            if(unitNameSplit.length < 2){
                teamName = "No team";
            }
            else {

                teamName += unitNameSplit[1] + this.traits[0].getNum_units();
            }
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
