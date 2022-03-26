package com.jjchmielewski.tftarena.entitis.documents.unit;

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
    private int offensiveComponentCount;

    //community dragon fields

    private String apiName;

    private String[] traits;

    public Unit() {
    }

    public void countOffensiveComponents(){

        offensiveComponentCount = 0;

        int[] offensiveComponents = new int[]{1,2,3,4,9};

        for(int i : items){
            for(int c : offensiveComponents){
                if(i/10 == c)
                    offensiveComponentCount++;
                if(i%10 == c)
                    offensiveComponentCount++;
            }
        }
    }

    @Override
    public String toString() {
        return "Unit{" +
                "id='" + id + '\'' +
                ", character_id='" + character_id + '\'' +
                ", items=" + Arrays.toString(items) +
                ", tier=" + tier +
                ", offensiveComponentCount=" + offensiveComponentCount +
                ", apiName='" + apiName + '\'' +
                ", traits=" + Arrays.toString(traits) +
                '}';
    }
}
