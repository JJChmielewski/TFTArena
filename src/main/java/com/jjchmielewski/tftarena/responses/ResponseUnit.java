package com.jjchmielewski.tftarena.responses;

import com.jjchmielewski.tftarena.metatft.MetaUnit;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResponseUnit implements Comparable<ResponseUnit>{

    private String apiNameWithTier;

    private double value;

    private List<ResponseItem> items;

    public ResponseUnit(String apiNameWithTier, double value) {
        this.apiNameWithTier = apiNameWithTier;
        this.value = value;
    }

    public ResponseUnit(Unit unit) {
        apiNameWithTier = unit.getCharacter_id() +"_"+ unit.getTier();
        items = new ArrayList<>();
        if (unit.getItems() == null) {
            return;
        }

        for (int item : unit.getItems()) {
            items.add(new ResponseItem(item));
        }
    }

    public ResponseUnit(MetaUnit metaUnit) {
        apiNameWithTier = metaUnit.getUnit()+"_"+metaUnit.getTier();
        items = new ArrayList<>();

        for (int item : metaUnit.getItemIDs()) {
            items.add(new ResponseItem(item));
        }
    }

    @Override
    public int compareTo(ResponseUnit o) {
        return Double.compare(this.value, o.getValue());
    }
}
