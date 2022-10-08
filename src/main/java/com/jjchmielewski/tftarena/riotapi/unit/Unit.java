package com.jjchmielewski.tftarena.riotapi.unit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.metatft.MetaUnit;
import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Unit implements Comparable<Unit>{

    private String id;

    private String character_id;
    private int[] items;
    private int tier;
    private int offensiveComponentCount;

    public Unit() {
    }

    public Unit(MetaUnit metaUnit) {
        this.character_id = metaUnit.getUnit();
        this.tier = metaUnit.getTier();
        this.items = metaUnit.getItem_ids();
    }

    public void countOffensiveComponents(){

        if (items == null) {
            return;
        }

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

    public int getItemComponentsCount() {
        int componentCount = 0;

        if (items != null) {
            for (Integer item : items) {
                if (item < 1 || item > 99) {
                    continue;
                }
                componentCount++;

                if (item > 9) {
                    componentCount++;
                }
            }
        }
        return componentCount;
    }

    public String getFullUnitName() {
        return this.character_id+"_"+this.tier;
    }

    @Override
    public String toString() {
        return "Unit{" +
                "id='" + id + '\'' +
                ", character_id='" + character_id + '\'' +
                ", items=" + Arrays.toString(items) +
                ", tier=" + tier +
                ", offensiveComponentCount=" + offensiveComponentCount +
                '}';
    }

    @Override
    public int compareTo(Unit o) {
        if(this.offensiveComponentCount == o.offensiveComponentCount)
            return Integer.compare(this.tier, o.getTier());

        return Integer.compare(this.offensiveComponentCount, o.getOffensiveComponentCount());
    }
}
