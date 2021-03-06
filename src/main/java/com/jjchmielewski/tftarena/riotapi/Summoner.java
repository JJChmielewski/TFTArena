package com.jjchmielewski.tftarena.riotapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Summoner {

    private String leagueId;
    private String summonerId;
    private String puuid;
    private String summonerName;
    private String tier;
    private String rank;
    private String leaguePoints;


    public Summoner() {
    }

    @Override
    public String toString() {
        return "Summoner{" +
                "leagueId='" + leagueId + '\'' +
                ", summonerId='" + summonerId + '\'' +
                ", puuid='" + puuid + '\'' +
                ", tier='" + tier + '\'' +
                ", rank='" + rank + '\'' +
                ", leaguePoints='" + leaguePoints + '\'' +
                '}';
    }
}
