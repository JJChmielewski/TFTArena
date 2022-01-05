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

        final String urlDiamondEU = "https://euw1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=1";
        final String urlDiamondUS = "https://na1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=1";
        final String urlDiamondAsia = "https://kr.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=1";

        final String urlSummonerEU = "https://euw1.api.riotgames.com/tft/summoner/v1/summoners/";
        final String urlSummonerAsia = "https://kr.api.riotgames.com/tft/summoner/v1/summoners/";
        final String urlSummonerUS = "https://na1.api.riotgames.com/tft/summoner/v1/summoners/";

        final String urlSummonerMatchUS = "https://americas.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        final String urlSummonerMatchAsia = "https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        final String urlSummonerMatchEU = "https://europe.api.riotgames.com/tft/match/v1/matches/by-puuid/";


        Summoner[] diamondSummonersEU;
        Summoner[] diamondSummonersUS;
        Summoner[] diamondSummonersAsia;

        ResponseEntity<Summoner[]> responseDiamondEU = restTemplate.exchange(urlDiamondEU,HttpMethod.GET,request,Summoner[].class);
        ResponseEntity<Summoner[]> responseDiamondUS = restTemplate.exchange(urlDiamondUS,HttpMethod.GET,request,Summoner[].class);
        ResponseEntity<Summoner[]> responseDiamondAsia = restTemplate.exchange(urlDiamondAsia, HttpMethod.GET,request,Summoner[].class);

        diamondSummonersEU = responseDiamondEU.getBody();
        diamondSummonersUS = responseDiamondUS.getBody();
        diamondSummonersAsia = responseDiamondAsia.getBody();


        for(int i=0; i<diamondSummonersUS.length; i++){
            diamondSummonersEU[i] = restTemplate.exchange(urlSummonerEU+diamondSummonersEU[i].getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody();
            diamondSummonersUS[i] = restTemplate.exchange(urlSummonerUS+diamondSummonersUS[i].getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody();
            diamondSummonersAsia[i] = restTemplate.exchange(urlSummonerAsia+diamondSummonersAsia[i].getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody();

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println(Arrays.toString(diamondSummonersEU));
        System.out.println(Arrays.toString(diamondSummonersUS));
        System.out.println(Arrays.toString(diamondSummonersAsia));

        List<String> matchListDiamondUS = new ArrayList<>();
        List<String> matchListDiamondAsia = new ArrayList<>();
        List<String> matchListDiamondEU = new ArrayList<>();

        for(int i=0; i<diamondSummonersUS.length;i++){
            String[] matchesDiamondEU = restTemplate.exchange(urlSummonerMatchEU+diamondSummonersEU[i].getPuuid()+"/ids?count=100",HttpMethod.GET,request,String[].class).getBody();
            String[] matchesDiamondUS = restTemplate.exchange(urlSummonerMatchUS+diamondSummonersUS[i].getPuuid()+"/ids?count=100",HttpMethod.GET,request,String[].class).getBody();
            String[] matchesDiamondAsia = restTemplate.exchange(urlSummonerMatchAsia+diamondSummonersAsia[i].getPuuid()+"/ids?count=100",HttpMethod.GET,request,String[].class).getBody();

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

        DataCollectorSubThread subThreadUS = new DataCollectorSubThread(matchListDiamondUS, "https://americas.api.riotgames.com/tft/match/v1/matches/", request);
        DataCollectorSubThread subThreadAsia = new DataCollectorSubThread(matchListDiamondAsia, "https://asia.api.riotgames.com/tft/match/v1/matches/",request);
        DataCollectorSubThread subThreadEU = new DataCollectorSubThread(matchListDiamondEU, "https://europe.api.riotgames.com/tft/match/v1/matches/",request);

        subThreadUS.start();
        subThreadAsia.start();
        subThreadEU.start();

        subThreadUS.join();
        subThreadAsia.join();
        subThreadEU.join();

        gameRepository.saveAll(subThreadUS.getCollectedGames());

        gameRepository.saveAll(subThreadAsia.getCollectedGames());

        gameRepository.saveAll(subThreadEU.getCollectedGames());

        System.out.println("Data collection successful");

    }

    @SneakyThrows
    public void run(){

        this.collectData();
    }

}
