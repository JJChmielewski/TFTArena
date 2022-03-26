package com.jjchmielewski.tftarena.matrixbuilder.communitydragon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jjchmielewski.tftarena.entitis.documents.unit.Trait;
import com.jjchmielewski.tftarena.entitis.documents.unit.Unit;
import com.jjchmielewski.tftarena.service.MainService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Component
public class CommunityDragonHandler {

    @Value("${tftarena.community-dragon-url}")
    private String communityDragonURL;

    @Value("${tftarena.current-set}")
    private int currentSet;

    private final MainService mainService;

    @Autowired
    public CommunityDragonHandler(MainService mainService) {
        this.mainService = mainService;
    }


    public void readCommunityDragon(){

        try {

            BufferedInputStream inputStream = new BufferedInputStream(new URL(communityDragonURL).openStream());

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String jsonText = new String(), line;

            while((line = reader.readLine()) != null){
                jsonText +=line;
            }

            JSONObject jsonObject = new JSONObject(jsonText);

            JSONObject sets = (JSONObject) jsonObject.get("sets");

            JSONObject currentSet = (JSONObject) sets.get("" + this.currentSet);

            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            Trait[] traits = objectMapper.readValue(currentSet.get("traits").toString(), Trait[].class);

            Unit[] units = objectMapper.readValue(currentSet.get("champions").toString(), Unit[].class);

            mainService.setTraits(traits);
            mainService.setUnits(units);

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
