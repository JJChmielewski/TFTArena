package com.jjchmielewski.tftarena.entitis;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Arrays;

@JsonIgnoreProperties(ignoreUnknown = true)
@Document(collation = "GameInfo")
@Getter
@Setter
public class GameInfo {

    @Id
    private String id;

    private TeamComp[] participants;

    public GameInfo() {
    }

    @Override
    public String toString() {
        return "GameInfo{" +
                "participants=" + Arrays.toString(participants) +
                '}';
    }
}
