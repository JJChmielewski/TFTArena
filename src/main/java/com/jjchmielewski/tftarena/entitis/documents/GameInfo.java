package com.jjchmielewski.tftarena.entitis.documents;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.entitis.documents.TeamComp;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigInteger;
import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collation = "GameInfo")
@Getter
@Setter
public class GameInfo {

    @Id
    private String id;

    private long game_datetime;

    private TeamComp[] participants;

    public GameInfo() {
    }

}