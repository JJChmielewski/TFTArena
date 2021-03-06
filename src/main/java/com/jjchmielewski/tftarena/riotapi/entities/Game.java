package com.jjchmielewski.tftarena.riotapi.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.jjchmielewski.tftarena.riotapi.GameInfo;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class Game {

    @Id
    private String id;

    private GameInfo info;

    public Game() {
    }

    @Override
    public String toString() {
        return "Game{" +
                "id='" + id + '\'' +
                ", info=" + info +
                '}';
    }
}
