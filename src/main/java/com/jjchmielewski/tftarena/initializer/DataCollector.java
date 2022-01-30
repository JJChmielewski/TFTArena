package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.documents.Summoner;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.repository.GameRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataCollector extends Thread{

    private final String apiKey;

    private final int playerPages = 10;
    private final int matcherPerPlayer = 10;

    private final GameRepository gameRepository;

    private final String urlDiamond;

    private final String urlSummoner;

    private final String urlSummonerMatches;

    private final String urlMatchDetails;

    private final boolean saveGames;

    private List<Game> gatheredGames;

    public DataCollector(GameRepository gameRepository,String apiKey, String urlDiamond, String urlSummoner, String urlSummonerMatches, String urlMatchDetails, boolean saveGames) {
        this.gameRepository = gameRepository;
        this.urlDiamond = urlDiamond;
        this.urlSummoner = urlSummoner;
        this.urlSummonerMatches = urlSummonerMatches;
        this.urlMatchDetails = urlMatchDetails;
        this.apiKey=apiKey;
        this.saveGames = saveGames;
        this.gatheredGames = new ArrayList<>();
    }

    public void collectData() throws InterruptedException {

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Riot-Token", this.apiKey);
        final HttpEntity<String> request = new HttpEntity<>(headers);

        List<Summoner> diamondSummoners = new ArrayList<>();

        for(int i = 1; i<= playerPages; i++){
            ResponseEntity<Summoner[]> responseDiamond = restTemplate.exchange(urlDiamond +i,HttpMethod.GET,request,Summoner[].class);

            if(responseDiamond.getBody() != null)
                diamondSummoners.addAll(Arrays.asList(responseDiamond.getBody()));

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println("Collected diamond summoners in thread: "+this.getId());

        for(int i=0; i<diamondSummoners.size(); i++){
            diamondSummoners.set(i,restTemplate.exchange(urlSummoner +diamondSummoners.get(i).getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody());

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println("Collected summoner details in thread: "+this.getId());

        List<String> matchListDiamond = new ArrayList<>();

        for (Summoner diamondSummoner : diamondSummoners) {
            String[] matchesDiamond = restTemplate.exchange(urlSummonerMatches + diamondSummoner.getPuuid() + "/ids?count="+ matcherPerPlayer, HttpMethod.GET, request, String[].class).getBody();

            if (matchesDiamond != null)
                matchListDiamond.addAll(Arrays.asList(matchesDiamond));

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println("Collected match list in thread: "+this.getId());

        matchListDiamond = new ArrayList<>(new HashSet<>(matchListDiamond));


        System.out.println("Starting to download matches in thread: "+this.getId());

        for(String match : matchListDiamond){
            try{
                TimeUnit.MILLISECONDS.sleep(1300);

                Game temp = restTemplate.exchange(urlMatchDetails +match, HttpMethod.GET,request,Game.class).getBody();

                if(temp!=null){
                    if(saveGames)
                        gameRepository.save(temp);

                    gatheredGames.add(temp);

                }


            }catch (Exception e){
                e.printStackTrace();
            }
        }

        System.out.println("Data collection successful in thread: "+this.getId());

    }

    public List<Game> getGatheredGames() {
        return gatheredGames;
    }

    public void run(){
        try{
            this.collectData();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
