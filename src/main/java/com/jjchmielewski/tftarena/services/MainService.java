package com.jjchmielewski.tftarena.services;

import com.jjchmielewski.tftarena.entitis.documents.TeamComp;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MainService {


    private final GameRepository gameRepository;

    public static double[][][] matrix;
    public static List<String> teamNames;


    @Autowired
    public MainService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<Map.Entry<String, Double>> getData(String[] checkedTeams){

       int indexes[] = new int[checkedTeams.length];

       for(int i=0;i< checkedTeams.length;i++){

           if(teamNames.contains(checkedTeams[i]))
               indexes[i] = teamNames.indexOf(checkedTeams[i]);
           else
               indexes[i] = teamNames.indexOf("No team");
       }

        //System.out.println(teams);

        HashMap<String, Double> strength = new HashMap<>();

        for(int i=0;i<checkedTeams.length;i++){
            double tempStrength=0.0;

            for(int j=0;j<checkedTeams.length;j++){
                tempStrength+=matrix[indexes[i]][indexes[j]][0];
            }

            strength.put(checkedTeams[i],tempStrength);
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(strength.entrySet());

        sorted.sort(new Comparator<>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (o1.getValue() > o2.getValue())
                    return -1;
                else
                if(o1.getValue().equals(o2.getValue())) {
                    return 0;
                }
                else
                    return 1;
            }
        });

        return sorted;
    }

    public void checkAlgorithm(){

        List<Game> games = gameRepository.findAll();

        int gamesNumber = 10;
        double[] method1 = new double[gamesNumber+1];
        double[] method2 = new double[gamesNumber+1];

        for(int g=0;g<gamesNumber;g++){


            TeamComp[] teamComps = games.get(g).getInfo().getParticipants();
            String[] teams = new String[teamComps.length];
            for(int i=0;i< teamComps.length;i++){
                teams[teamComps[i].getPlacement()-1] = teamComps[i].getTeamName();

            }
            if(teams[0] == null || teams[1] == null || teams[2] == null || teams[3] == null || teams[4] == null || teams[5] == null || teams[6] == null || teams[7] == null){
                System.out.println("Error in: " + games.get(g).getId());
                continue;
            }


            List<Map.Entry<String, Double>> data = getData(teams);

            if(g==2 || g==9)
                System.out.println(data);

            String[] guessedTeams = new String[data.size()];
            for(int i=0;i<data.size();i++){
                guessedTeams[i] = data.get(i).getKey();
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

        System.out.println(Arrays.toString(method1));
        System.out.println(Arrays.toString(method2));


        System.out.println("M1 Avg error: "+method1[method1.length-1]);
        System.out.println("M2 Avg error: "+method2[method2.length-1]);
    }


}
