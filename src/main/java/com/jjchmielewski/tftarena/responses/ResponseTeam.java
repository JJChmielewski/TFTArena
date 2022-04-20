package com.jjchmielewski.tftarena.responses;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ResponseTeam implements Comparable<ResponseTeam>{

    private String teamName;

    private List<ResponseUnit> units;

    private double value;

    public ResponseTeam(String teamName, double value) {
        this.teamName = teamName;
        this.value = value;
    }

    public ResponseTeam(String teamName, List<ResponseUnit> units) {
        this.teamName = teamName;
        this.units = units;
    }

    @Override
    public int compareTo(ResponseTeam o) {
        return Double.compare(this.value, o.value);
    }
}
