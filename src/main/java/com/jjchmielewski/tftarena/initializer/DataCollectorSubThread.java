package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.Game;
import com.jjchmielewski.tftarena.repository.GameRepository;
import jdk.jfr.Category;
import lombok.Getter;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
public class DataCollectorSubThread extends Thread{


    private GameRepository gameRepository;

    private final List<String> games;
    private final String url;
    private final HttpEntity<String> request;

    private List<Game> collectedGames;

    public DataCollectorSubThread(List<String> games, String url, HttpEntity<String> request, GameRepository gameRepository) {
        this.games = games;
        this.url = url;
        this.request = request;
        this.gameRepository = gameRepository;
    }

    @SneakyThrows
    public void run(){

        System.out.println("Subthread starts running");

        RestTemplate restTemplate = new RestTemplate();
        this.collectedGames = new ArrayList<>();

        for(String game : games){
            try{
                TimeUnit.MILLISECONDS.sleep(1300);

                Game temp = restTemplate.exchange(url+game, HttpMethod.GET,request,Game.class).getBody();

                if(temp!=null)
                    gameRepository.save(temp);

            }catch (Exception e){
                e.printStackTrace();
                continue;
            }
        }

        System.out.println("Subthread data collection successful");

    }

}
