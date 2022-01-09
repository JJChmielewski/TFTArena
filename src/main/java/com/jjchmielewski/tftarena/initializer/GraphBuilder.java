package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.Game;
import com.jjchmielewski.tftarena.entitis.TeamComp;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.repository.TeamNEO4JRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class GraphBuilder {

    @Autowired
    private GameRepository dataCollector;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private TeamNEO4JRepository teamNEO4JRepository;


    @PostConstruct
    public void init() throws InterruptedException {

        //dataCollector.start();
        //buildTest();
        getData();
    }

    public void buildTest(){

        List<String> teamNames = new ArrayList<>();
        List<List<double[]>> strengthList2D = new ArrayList<>();
        List<Team> nodeTeams = new ArrayList<>();

        System.out.println("lol");

        List<Game> games = gameRepository.findAll();


        for(Game game : games) {


            TeamComp[] teams = game.getInfo().getParticipants();

            if(teams.length != 8){
                System.out.println(game.getId());
                continue;
            }

            for (int i = 0; i < teams.length; i++) {
                if (!teamNames.contains(teams[i].getTeamName())) {
                    teamNames.add(teams[i].getTeamName());
                    strengthList2D.add(new ArrayList<>());
                    for (int x = 0; x < strengthList2D.size(); x++) {
                        for (int y = strengthList2D.get(x).size(); y < strengthList2D.size(); y++) {
                            strengthList2D.get(x).add(new double[2]);
                        }
                    }
                }
            }

            for (TeamComp team : teams) {

                int teamIndex = teamNames.indexOf(team.getTeamName());

                for (TeamComp enemyTeam : teams) {
                    int enemyTeamIndex = teamNames.indexOf(enemyTeam.getTeamName());


                    strengthList2D.get(teamIndex).get(enemyTeamIndex)[0] += team.getPlacement() - enemyTeam.getPlacement();
                    strengthList2D.get(teamIndex).get(enemyTeamIndex)[1]++;

                }
            }
        }

        for (int i = 0; i < strengthList2D.size(); i++) {
            for (int j = 0; j < strengthList2D.size(); j++) {
                System.out.print(Arrays.toString(strengthList2D.get(i).get(j)) + " ");
            }
            System.out.print("\n");
        }


        for (String s : teamNames) {
            nodeTeams.add(new Team(s));
        }


        for (int i = 0; i < nodeTeams.size(); i++) {

            for (int j = 0; j < nodeTeams.size(); j++) {
                TeamRelationship teamRelationship = new TeamRelationship(nodeTeams.get(j));
                double strength;

                if (strengthList2D.get(i).get(j)[1] != 0)
                    strength = strengthList2D.get(i).get(j)[0] / strengthList2D.get(i).get(j)[1];
                else
                    strength = 0;

                teamRelationship.setStrength(strength);
                nodeTeams.get(i).addEnemyTeams(teamRelationship);
            }
        }

        System.out.println(nodeTeams.size());

        System.out.println(nodeTeams.size() == strengthList2D.size());

        teamNEO4JRepository.deleteAll();

        System.out.println("Delete done");

        teamNEO4JRepository.save(nodeTeams.get(0));

        System.out.println("Save done");

    }

    public void getData(){

        //System.out.println(teamNEO4JRepository.findByName("Chemtech5").getEnemyTeams().get(60).getEnemyTeam());

        List<Team> teams = teamNEO4JRepository.findStrength( new String[]{"Socialite3", "Chemtech7", "Twinshot4", "Scrap5", "Socialite3", "Scrap6", "Socialite2", "Protector4"});

        System.out.println(teams);

        HashMap<String, Double> strength = new HashMap<>();

        for(Team t : teams){
            double tempStrength=0.0;
            for(TeamRelationship r : t.getEnemyTeams())
                tempStrength+=r.getStrength();
            strength.put(t.getName(),tempStrength);
        }

        System.out.println(strength);
    }
}
