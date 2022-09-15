package com.jjchmielewski.tftarena.responses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseItem implements Comparable<ResponseItem>{

    private String name;
    private int id;
    private int[] from;
    private int timesPlayed;

    public ResponseItem(String name, int id, int[] from, int timesPlayed) {
        this.name = name;
        this.id = id;
        this.from = from;
        this.timesPlayed = timesPlayed;
    }

    public ResponseItem(int id) {
        this.id = id;
    }

    @Override
    public int compareTo(ResponseItem o) {
        return Integer.compare(this.timesPlayed, o.getTimesPlayed());
    }
}
