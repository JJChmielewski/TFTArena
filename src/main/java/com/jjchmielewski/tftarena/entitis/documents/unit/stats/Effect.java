package com.jjchmielewski.tftarena.entitis.documents.unit.stats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Effect {

    private int maxUnits;

    private int minUnits;

    private int style;

    private Map<String, Double> variables;

    @Override
    public String toString() {
        return "Effect{" +
                "maxUnits=" + maxUnits +
                ", minUnits=" + minUnits +
                ", style=" + style +
                ", variables=" + variables +
                '}';
    }
}
