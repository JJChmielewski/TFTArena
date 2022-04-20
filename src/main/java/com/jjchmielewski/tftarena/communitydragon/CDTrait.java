package com.jjchmielewski.tftarena.communitydragon;

import com.jjchmielewski.tftarena.communitydragon.stats.CDEffect;

public record CDTrait(String name, String apiName, CDEffect[] effects) {}
