package com.jjchmielewski.tftarena.communitydragon;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            StringBuilder jsonText = new StringBuilder();
            String line;

            while((line = reader.readLine()) != null){
                jsonText.append(line);
            }

            JSONObject jsonObject = new JSONObject(jsonText.toString());

            JSONObject sets = (JSONObject) jsonObject.get("sets");

            JSONObject currentSet = (JSONObject) sets.get("" + this.currentSet);

            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            CDTrait[] traits = objectMapper.readValue(currentSet.get("traits").toString(), CDTrait[].class);

            CDUnit[] units = objectMapper.readValue(currentSet.get("champions").toString(), CDUnit[].class);

            CDItem[] items = objectMapper.readValue(jsonObject.get("items").toString(), CDItem[].class);

            mainService.setTraits(traits);
            mainService.setUnits(units);
            mainService.setItems(items);

        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
