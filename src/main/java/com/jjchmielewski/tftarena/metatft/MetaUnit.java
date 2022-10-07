package com.jjchmielewski.tftarena.metatft;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class MetaUnit {

    private String unit;
    private String[] items;
    private int tier;
    private String loc;
    private int cost;
    private String name;
    private int[] item_ids;

    public MetaUnit() {
    }

    @Override
    public String toString() {
        return "MetaUnits{" +
                "unit='" + unit + '\'' +
                ", items=" + Arrays.toString(items) +
                ", tier=" + tier +
                ", loc='" + loc + '\'' +
                ", cost=" + cost +
                ", name='" + name + '\'' +
                ", itemIDs=" + Arrays.toString(item_ids) +
                '}';
    }
}
