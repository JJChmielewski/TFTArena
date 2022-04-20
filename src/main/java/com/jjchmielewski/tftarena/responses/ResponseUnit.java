package com.jjchmielewski.tftarena.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResponseUnit implements Comparable<ResponseUnit>{

    private String apiName;

    private double value;

    private List<ResponseItem> items;

    public ResponseUnit(String apiName, double value) {
        this.apiName = apiName;
        this.value = value;
    }

    @Override
    public int compareTo(ResponseUnit o) {
        return Double.compare(this.value, o.getValue());
    }
}
