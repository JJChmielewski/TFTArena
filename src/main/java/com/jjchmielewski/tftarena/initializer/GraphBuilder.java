package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.Game;
import com.jjchmielewski.tftarena.entitis.TeamComp;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.repository.TeamNEO4JRepository;
import com.jjchmielewski.tftarena.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class GraphBuilder {

    private final TeamRepository teamRepository;

    private final GameRepository dataCollector;

    private final GameRepository gameRepository;

    private final TeamNEO4JRepository teamNEO4JRepository;

    @Autowired
    public GraphBuilder(TeamRepository teamRepository, GameRepository dataCollector, GameRepository gameRepository, TeamNEO4JRepository teamNEO4JRepository) {
        this.teamRepository = teamRepository;
        this.dataCollector = dataCollector;
        this.gameRepository = gameRepository;
        this.teamNEO4JRepository = teamNEO4JRepository;
    }


    @PostConstruct
    public void init() {

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

            for (TeamComp teamComp : teams) {
                if (!teamNames.contains(teamComp.getTeamName())) {
                    teamNames.add(teamComp.getTeamName());
                    strengthList2D.add(new ArrayList<>());
                    for (int x = 0; x < strengthList2D.size(); x++) {
                        for (int y = strengthList2D.get(x).size(); y < strengthList2D.size(); y++) {
                            strengthList2D.get(x).add(new double[3]);
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
                strengthList2D.get(teamIndex).get(teamIndex)[2]++;
            }
        }

        for(int i=0;i< teamNames.size();i++){

            double minPercent = 0.15;

            if(strengthList2D.get(i).get(i)[2] < games.size()/100.0 * minPercent || teamNames.get(i).equals("No team")){
                for(List<double[]> l : strengthList2D){
                    l.remove(i);
                }
                strengthList2D.remove(i);
                teamNames.remove(i);
                i--;
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

        teamRepository.saveGraph(nodeTeams);

        System.out.println("Save done");

    }

    public void getData(){

        //System.out.println(teamNEO4JRepository.findByName("Chemtech5").getEnemyTeams().get(60).getEnemyTeam());

        List<Team> teams = teamNEO4JRepository.findStrength( new String[]{"Set6_Bodyguard_1_TFT6_Kaisa_2", "Set6_Innovator_4_TFT6_Ezreal_3", "Set6_Assassin_2_TFT6_Akali_2", "Set6_Syndicate_3_TFT6_Akali_1", "Set6_Chemtech_3_TFT6_Viktor_1", "Set6_Bruiser_2_TFT6_KogMaw_2", "Set6_Sniper_3_TFT6_Jhin_1", "Set6_Yordle_1_TFT6_Orianna_1"});

        System.out.println(teams);

        HashMap<String, Double> strength = new HashMap<>();

        for(Team t : teams){
            double tempStrength=0.0;
            for(TeamRelationship r : t.getEnemyTeams())
                tempStrength+=r.getStrength();
            strength.put(t.getName(),tempStrength);
        }

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(strength.entrySet());

        sorted.sort(new Comparator<>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                if (o1.getValue() < o2.getValue())
                    return -1;
                else
                    if(o1.getValue().equals(o2.getValue())) {
                        return 0;
                    }
                    else
                        return 1;
            }
        });

        System.out.println(sorted);
    }
}
