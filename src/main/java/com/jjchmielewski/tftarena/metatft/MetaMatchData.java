package com.jjchmielewski.tftarena.metatft;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class MetaMatchData {

    private String[] opponent_augments;
    private String[] player_augments;
    private MetaUnit[] opponent_units;
    private MetaUnit[] player_units;
    private String[] player_full_traits;
    private String[] opponent_full_traits;
    private double win;
    private double opponent_health_lost;
    private double player_health_lost;

    public MetaMatchData() {
    }

    @Override
    public String toString() {
        return "MetaMatchData{" +
                "opponent_augments=" + Arrays.toString(opponent_augments) +
                ", player_augments=" + Arrays.toString(player_augments) +
                ", opponent_units=" + Arrays.toString(opponent_units) +
                ", player_units=" + Arrays.toString(player_units) +
                ", player_full_traits=" + Arrays.toString(player_full_traits) +
                ", opponent_full_traits=" + Arrays.toString(opponent_full_traits) +
                ", win=" + win +
                ", opponent_health_lost=" + opponent_health_lost +
                ", player_health_lost=" + player_health_lost +
                '}';
    }
}
