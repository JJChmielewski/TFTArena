package com.jjchmielewski.tftarena.matrixbuilder;

import com.jjchmielewski.tftarena.riotapi.Summoner;
import com.jjchmielewski.tftarena.riotapi.entities.Game;
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

    private final int playerPages;
    private final int matcherPerPlayer;

    private final GameRepository gameRepository;

    private final String urlDiamond;

    private final String urlSummoner;

    private final String urlSummonerMatches;

    private final String urlMatchDetails;

    private final boolean saveGames;

    private final long setBeginning;

    private final List<Game> gatheredGames;

    public DataCollector(GameRepository gameRepository,String apiKey, String urlDiamond, String urlSummoner, String urlSummonerMatches, String urlMatchDetails, boolean saveGames, long setBeginning) {
        this.gameRepository = gameRepository;
        this.urlDiamond = urlDiamond;
        this.urlSummoner = urlSummoner;
        this.urlSummonerMatches = urlSummonerMatches;
        this.urlMatchDetails = urlMatchDetails;
        this.apiKey=apiKey;
        this.saveGames = saveGames;
        this.gatheredGames = new ArrayList<>();
        this.setBeginning = setBeginning;

        //default values
        this.playerPages=10;
        this.matcherPerPlayer=10;
    }

    public void collectData() throws InterruptedException {

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.add("X-Riot-Token", this.apiKey);
        final HttpEntity<String> request = new HttpEntity<>(headers);

        List<Summoner> diamondSummoners = new ArrayList<>();

        for(int i = 1; i<= playerPages; i++){

            try{
                ResponseEntity<Summoner[]> responseDiamond = restTemplate.exchange(urlDiamond +i,HttpMethod.GET,request,Summoner[].class);

                if(responseDiamond.getBody() != null)
                    diamondSummoners.addAll(Arrays.asList(responseDiamond.getBody()));
            }catch (Exception e){
                e.printStackTrace();
            }

            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println("Collected diamond summoners in thread: "+this.getId());

        for(int i=0; i<diamondSummoners.size(); i++){
            try{
                Summoner temp = restTemplate.exchange(urlSummoner +diamondSummoners.get(i).getSummonerId(),HttpMethod.GET,request,Summoner.class).getBody();

                if(temp!=null)
                    diamondSummoners.set(i,temp);
                else {
                    diamondSummoners.remove(i);
                    i--;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            TimeUnit.MILLISECONDS.sleep(1300);
        }

        System.out.println("Collected summoner details in thread: "+this.getId());

        List<String> matchListDiamond = new ArrayList<>();

        for (Summoner diamondSummoner : diamondSummoners) {
            try{
                String[] matchesDiamond = restTemplate.exchange(urlSummonerMatches + diamondSummoner.getPuuid() + "/ids?count="+ matcherPerPlayer, HttpMethod.GET, request, String[].class).getBody();

                if (matchesDiamond != null)
                    matchListDiamond.addAll(Arrays.asList(matchesDiamond));
            }catch (Exception e){
                e.printStackTrace();
            }

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
                    if(this.setBeginning < temp.getInfo().getGame_datetime()){
                        if(saveGames)
                            gameRepository.save(temp);

                        gatheredGames.add(temp);
                    }
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
