package com.jjchmielewski.tftarena.initializer;

import com.jjchmielewski.tftarena.entitis.documents.TeamComp;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class GraphBuilder {

    private final String apiKey;

    private final TeamRepository teamRepository;

    private final GameRepository gameRepository;


    @Autowired
    public GraphBuilder(TeamRepository teamRepository, GameRepository gameRepository, @Value("${tftarena.riot-games.api-key}") String apiKey) {
        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.apiKey=apiKey;
    }


    @PostConstruct
    public void init() {

        collectData();
        //buildTest();
        //getData();
    }

    public void collectData(){
        String urlDiamondEU  = "https://euw1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";
        String urlSummonersEU = "https://euw1.api.riotgames.com/tft/summoner/v1/summoners/";
        String urlSummonerMatchesEU = "https://europe.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        String urlMatchDetailsEU = "https://europe.api.riotgames.com/tft/match/v1/matches/";

        String urlDiamondUS  = "https://na1.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";
        String urlSummonersUS = "https://na1.api.riotgames.com/tft/summoner/v1/summoners/";
        String urlSummonerMatchesUS = "https://americas.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        String urlMatchDetailsUS = "https://americas.api.riotgames.com/tft/match/v1/matches/";

        String urlDiamondAsia  = "https://kr.api.riotgames.com/tft/league/v1/entries/DIAMOND/I?page=";
        String urlSummonersAsia = "https://kr.api.riotgames.com/tft/summoner/v1/summoners/";
        String urlSummonerMatchesAsia = "https://asia.api.riotgames.com/tft/match/v1/matches/by-puuid/";
        String urlMatchDetailsAsia = "https://asia.api.riotgames.com/tft/match/v1/matches/";


        DataCollector dataCollectorEU = new DataCollector(this.gameRepository,apiKey,urlDiamondEU,urlSummonersEU,urlSummonerMatchesEU,urlMatchDetailsEU);
        DataCollector dataCollectorUS = new DataCollector(this.gameRepository,apiKey,urlDiamondUS,urlSummonersUS,urlSummonerMatchesUS,urlMatchDetailsUS);
        DataCollector dataCollectorAsia = new DataCollector(this.gameRepository,apiKey,urlDiamondAsia,urlSummonersAsia,urlSummonerMatchesAsia,urlMatchDetailsAsia);

        gameRepository.deleteAll();

        dataCollectorEU.start();
        dataCollectorUS.start();
        dataCollectorAsia.start();
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

        teamRepository.deleteAll();

        System.out.println("Delete done");

        teamRepository.saveGraph(nodeTeams);

        System.out.println("Save done");

    }

    public void getData(){

        List<Team> teams = teamRepository.getMatchInfo( new String[]{"Bodyguard_1_Kaisa_2", "Innovator_4_Ezreal_3", "Assassin_2_Akali_2", "Syndicate_3_Akali_1", "Chemtech_3_Viktor_1", "Bruiser_2_KogMaw_2", "Sniper_3_Jhin_1", "Yordle_1_Orianna_1"});

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
