package com.jjchmielewski.tftarena.metatft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public MetaMatchData(String[] csvData) {
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        if (csvData.length != 9) {
            return;
        }

        try {
            this.opponent_augments = objectMapper.readValue(csvData[0], String[].class);
            this.player_augments = objectMapper.readValue(csvData[1], String[].class);
            this.opponent_units = objectMapper.readValue(csvData[2], MetaUnit[].class);
            this.player_units = objectMapper.readValue(csvData[3], MetaUnit[].class);
            this.player_full_traits = objectMapper.readValue(csvData[4], String[].class);
            this.opponent_full_traits = objectMapper.readValue(csvData[5], String[].class);
            this.win = Double.parseDouble(csvData[6]);
            if (csvData[7].isBlank()) {
                this.opponent_health_lost = 0;
            } else {
                this.opponent_health_lost = Double.parseDouble(csvData[7]);
            }
            this.player_health_lost = Double.parseDouble(csvData[8]);
        } catch (JsonProcessingException jsonProcessingException) {
            jsonProcessingException.printStackTrace();
        }
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
