package com.jjchmielewski.tftarena.metatft;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.exceptions.CsvBeanIntrospectionException;
import com.opencsv.exceptions.CsvChainedException;
import com.opencsv.exceptions.CsvFieldAssignmentException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

public class MetaTftDataCollector {

    public static MetaMatchData[] getMetaTftMatchData() {

        String metaTftFile = "metaTft_test_data.csv";

        try {
            List<MetaMatchData> matchData = new CsvToBeanBuilder<MetaMatchData>(new FileReader(metaTftFile))
                    .withMappingStrategy(new MetaTftMappingStrategy()).withSeparator(';').withSkipLines(1).build().parse();

            matchData.forEach(System.out::println);

            return matchData.toArray(matchData.toArray(new MetaMatchData[0]));
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }

        return null;
    }

    static class MetaTftMappingStrategy extends ColumnPositionMappingStrategy {

        public MetaTftMappingStrategy() {
            this.setType(MetaMatchData.class);
        }

        @Override
        public Object populateNewBean(String[] line) throws CsvBeanIntrospectionException, CsvFieldAssignmentException, CsvChainedException {

            MetaMatchData metaMatchData = new MetaMatchData();
            ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            for (int i=0; i<line.length; i++) {
                line[i] = line[i].replace('\'', '\"');
            }

            try {
                metaMatchData.setOpponent_augments(objectMapper.readValue(line[0], String[].class));
                metaMatchData.setPlayer_augments(objectMapper.readValue(line[1], String[].class));
                metaMatchData.setOpponent_units(objectMapper.readValue(line[2], MetaUnit[].class));
                metaMatchData.setPlayer_units(objectMapper.readValue(line[3], MetaUnit[].class));
                metaMatchData.setPlayer_full_traits(objectMapper.readValue(line[4], String[].class));
                metaMatchData.setOpponent_full_traits(objectMapper.readValue(line[5], String[].class));
                metaMatchData.setWin(objectMapper.readValue(line[6], Double.class));
                metaMatchData.setOpponent_health_lost(objectMapper.readValue(line[7], Double.class));
                metaMatchData.setPlayer_health_lost(objectMapper.readValue(line[8], Double.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            return metaMatchData;
        }
    }

}
