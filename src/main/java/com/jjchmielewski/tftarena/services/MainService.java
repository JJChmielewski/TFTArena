package com.jjchmielewski.tftarena.services;

import com.jjchmielewski.tftarena.entitis.documents.Team;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MainService {


    private final GameRepository gameRepository;

    public static double[][][] matrix;
    public static List<String> teamNames;


    @Autowired
    public MainService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<Pair<String, Double>> getData(String[] checkedTeams){

       int[] indexes = new int[checkedTeams.length];

       for(int i=0;i< checkedTeams.length;i++){

           if(teamNames.contains(checkedTeams[i]))
               indexes[i] = teamNames.indexOf(checkedTeams[i]);
           else
               indexes[i] = teamNames.indexOf("No team");
       }

        //System.out.println(teams);

        List<Pair<String, Double>> strength = new ArrayList<>();

        for(int i=0;i<checkedTeams.length;i++){
            double tempStrength=0.0;

            for(int j=0;j<checkedTeams.length;j++){
                tempStrength+=matrix[indexes[i]][indexes[j]][0];
            }

            Pair<String, Double> temp = Pair.of(checkedTeams[i], tempStrength);

            strength.add(temp);
        }

        strength.sort(new Comparator<>() {
            @Override
            public int compare(Pair<String,Double> o1, Pair<String,Double> o2) {
                if (o1.getSecond() > o2.getSecond())
                    return -1;
                else
                if(o1.getSecond().equals(o2.getSecond())) {
                    return 0;
                }
                else
                    return 1;
            }
        });

        return strength;
    }

    public void checkAlgorithm(){

        List<Game> games = gameRepository.findAll();

        int gamesNumber = games.size();
        double[] method1 = new double[gamesNumber+1];
        double[] method2 = new double[gamesNumber+1];

        for(int g=0;g<gamesNumber;g++){

            Team[] teamComps = games.get(g).getInfo().getParticipants();
            String[] teams = new String[teamComps.length];

            for (Team team : teamComps) {
                teams[team.getPlacement() - 1] = team.getTeamName();
            }

            if(teams[0] == null || teams[1] == null || teams[2] == null || teams[3] == null || teams[4] == null || teams[5] == null || teams[6] == null || teams[7] == null){
                System.out.println("Error in: " + games.get(g).getId());
                continue;
            }

            List<Pair<String, Double>> data = getData(teams);

            String[] guessedTeams = new String[data.size()];
            for(int i=0;i<data.size();i++){
                guessedTeams[i] = data.get(i).getFirst();
            }

            //method 1
            for(int i=0;i<guessedTeams.length;i++){

                for(int j=0;j<teams.length;j++){
                    if(guessedTeams[i].equals(teams[j])){
                        method1[g] += Math.abs(teams.length - j -guessedTeams.length + i);
                    }
                }

            }
            method1[method1.length-1] += method1[g];
            method1[g] /= 32.0;

            //method 2
            for(int i=0;i<guessedTeams.length;i++){

                for(int j=0;j<teams.length;j++){
                    if(guessedTeams[i].equals(teams[j])){
                        method2[g] += Math.abs(j - i);

                    }
                }

            }
            method2[method2.length-1] += method2[g];
            method2[g] /= 32.0;

        }


        method1[method1.length-1] /= 32 * (method1.length-1);
        method2[method2.length-1] /= 32 * (method2.length-1);


        System.out.println("M1 Avg error: "+method1[method1.length-1]);
        System.out.println("M2 Avg error: "+method2[method2.length-1]);
    }


}
