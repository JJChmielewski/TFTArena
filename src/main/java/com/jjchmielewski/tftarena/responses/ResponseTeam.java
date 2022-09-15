package com.jjchmielewski.tftarena.responses;

import com.jjchmielewski.tftarena.riotapi.Team;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResponseTeam implements Comparable<ResponseTeam>{

    private String teamName;

    private List<ResponseUnit> units;

    private int placement;

    private double value;

    public ResponseTeam(String teamName, double value) {
        this.teamName = teamName;
        this.value = value;
    }

    public ResponseTeam(String teamName, List<ResponseUnit> units) {
        this.teamName = teamName;
        this.units = units;
    }

    public ResponseTeam(Team team) {
        teamName = team.getTeamName();
        units = new ArrayList<>();
        placement = team.getPlacement();
        for (Unit unit : team.getUnits()) {
            units.add(new ResponseUnit(unit));
        }
    }

    @Override
    public int compareTo(ResponseTeam o) {
        return Double.compare(this.value, o.getValue());
    }
}
