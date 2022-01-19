package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.Summoner;
import com.jjchmielewski.tftarena.repository.GameRepository;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class DataCollector extends Thread{

    @Value("${tftarena.riot-games.api-key}")
    private String apiKey;

    @Autowired
    private GameRepository gameRepository;

    public DataCollector() {    }

    public void collectData() throws InterruptedException {

        System.out.println(this.apiKey);

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Riot-Token", this.apiKey);
        final HttpEntity<String> request = new HttpEntity<>(headers);

        final String urlDiamondEU = "https://euw1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";
        final String urlDiamondUS = "https://na1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";
        final String urlDiamondAsia = "https://kr.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";

        final String urlSummonerEU = "https://euw1.api.riotgames.com/tft/summoner/v1/summoners/";
        final String urlSummonerAsia = "https://kr.api.riotgames.com/tft/summoner/v1/summoners/";
        final String urlSummonerUS = "https://na1.api.riotgames.com/tft/summoner/v1/summoners/";

        final String urlSummonerMatchUS = "https://americas.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        final String urlSummonerMatchAsia = "https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        final String urlSummonerMatchEU = "https://europe.api.riotgames.com/tft/match/v1/matches/by-puuid/";


        List<Summoner> diamondSummonersEU = new ArrayList<>();
        List<Summoner> diamondSummonersUS = new ArrayList<>();
        List<Summoner> diamondSummonersAsia = new ArrayList<>();

        for(int i=1;i<=100;i++){
            ResponseEntity<Summoner[]> responseDiamondEU = restTemplate.exchange(urlDiamondEU+i,HttpMethod.GET,request,Summoner[].class);
            ResponseEntity<Summoner[]> responseDiamondUS = restTemplate.exchange(urlDiamondUS+i,HttpMethod.GET,request,Summoner[].class);
            ResponseEntity<Summoner[]> responseDiamondAsia = restTemplate.exchange(urlDiamondAsia+i, HttpMethod.GET,request,Summoner[].class);

            if(responseDiamondEU.getBody() != null)
                diamondSummonersEU.addAll(Arrays.asList(responseDiamondEU.getBody()));
            if(responseDiamondUS.getBody() != null)
                diamondSummonersUS.addAll(Arrays.asList(responseDiamondUS.getBody()));
            if(responseDiamondAsia.getBody() != null)
                diamondSummonersAsia.addAll(Arrays.asList(responseDiamondAsia.getBody()));
        }


        for(int i=0; i<diamondSummonersUS.size(); i++){
            diamondSummonersEU.set(i,restTemplate.exchange(urlSummonerEU+diamondSummonersEU.get(i).getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody());
            diamondSummonersUS.set(i,restTemplate.exchange(urlSummonerUS+diamondSummonersUS.get(i).getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody());
            diamondSummonersAsia.set(i,restTemplate.exchange(urlSummonerAsia+diamondSummonersAsia.get(i).getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody());

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        List<String> matchListDiamondUS = new ArrayList<>();
        List<String> matchListDiamondAsia = new ArrayList<>();
        List<String> matchListDiamondEU = new ArrayList<>();

        for(int i=0; i<diamondSummonersUS.size();i++){
            String[] matchesDiamondEU = restTemplate.exchange(urlSummonerMatchEU+diamondSummonersEU.get(i).getPuuid()+"/ids?count=10",HttpMethod.GET,request,String[].class).getBody();
            String[] matchesDiamondUS = restTemplate.exchange(urlSummonerMatchUS+diamondSummonersUS.get(i).getPuuid()+"/ids?count=10",HttpMethod.GET,request,String[].class).getBody();
            String[] matchesDiamondAsia = restTemplate.exchange(urlSummonerMatchAsia+diamondSummonersAsia.get(i).getPuuid()+"/ids?count=10",HttpMethod.GET,request,String[].class).getBody();

            if(matchesDiamondUS!=null)
                matchListDiamondUS.addAll(Arrays.asList(matchesDiamondUS));
            if(matchesDiamondAsia!=null)
                matchListDiamondAsia.addAll(Arrays.asList(matchesDiamondAsia));
            if(matchesDiamondEU!=null)
                matchListDiamondEU.addAll(Arrays.asList(matchesDiamondEU));

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        matchListDiamondUS = new ArrayList<>(new HashSet<>(matchListDiamondUS));
        matchListDiamondAsia = new ArrayList<>(new HashSet<>(matchListDiamondAsia));
        matchListDiamondEU = new ArrayList<>(new HashSet<>(matchListDiamondEU));

        DataCollectorSubThread subThreadUS = new DataCollectorSubThread(matchListDiamondUS, "https://americas.api.riotgames.com/tft/match/v1/matches/", request, gameRepository);
        DataCollectorSubThread subThreadAsia = new DataCollectorSubThread(matchListDiamondAsia, "https://asia.api.riotgames.com/tft/match/v1/matches/",request, gameRepository);
        DataCollectorSubThread subThreadEU = new DataCollectorSubThread(matchListDiamondEU, "https://europe.api.riotgames.com/tft/match/v1/matches/",request, gameRepository);

        subThreadUS.start();
        subThreadAsia.start();
        subThreadEU.start();

        subThreadUS.join();
        subThreadAsia.join();
        subThreadEU.join();

        System.out.println("Data collection successful");

    }

    @SneakyThrows
    public void run(){

        this.collectData();
    }

}
