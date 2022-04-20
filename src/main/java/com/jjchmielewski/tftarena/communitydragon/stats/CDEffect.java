package com.jjchmielewski.tftarena.communitydragon.stats;

import java.util.Map;

public record CDEffect(int maxUnits, int minUnits, int style, Map<String, Double> variables){}
