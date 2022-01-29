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
public class GraphBuilder implements Runnable{

    private final String apiKey;

    private final TeamRepository teamRepository;

    private final GameRepository gameRepository;

    private final boolean buildGraph;

    private final boolean saveGames;

    private final boolean collectData;


    @Autowired
    public GraphBuilder(TeamRepository teamRepository, GameRepository gameRepository,
                        @Value("${tftarena.riot-games.api-key}") String apiKey,
                        @Value("${tftarena.buildGraph}") boolean buildGraph,
                        @Value("${tftarena.saveGames}") boolean saveGames,
                        @Value("${tftarena.collectData}") boolean collectData) {

        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.apiKey=apiKey;
        this.buildGraph = buildGraph;
        this.saveGames = saveGames;
        this.collectData = collectData;
    }


    @PostConstruct
    public void init() {

        Thread graphBuilderThread = new Thread(this);

        graphBuilderThread.start();

    }

    @Override
    public void run() {

        if(buildGraph) {
            try{
                List<Game> gatheredGames;

                if(collectData)
                    gatheredGames = collectData();
                else
                    gatheredGames = gameRepository.findAll();

                buildTest(gatheredGames);

            }catch (Exception e){
                e.printStackTrace();
            }
        }

        getData();
    }

    public List<Game> collectData() throws InterruptedException {
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


        DataCollector dataCollectorEU = new DataCollector(this.gameRepository,apiKey,urlDiamondEU,urlSummonersEU,urlSummonerMatchesEU,urlMatchDetailsEU,saveGames);
        DataCollector dataCollectorUS = new DataCollector(this.gameRepository,apiKey,urlDiamondUS,urlSummonersUS,urlSummonerMatchesUS,urlMatchDetailsUS,saveGames);
        DataCollector dataCollectorAsia = new DataCollector(this.gameRepository,apiKey,urlDiamondAsia,urlSummonersAsia,urlSummonerMatchesAsia,urlMatchDetailsAsia,saveGames);

        if(saveGames)
            gameRepository.deleteAll();

        dataCollectorEU.start();
        dataCollectorUS.start();
        dataCollectorAsia.start();

        dataCollectorEU.join();
        dataCollectorUS.join();
        dataCollectorAsia.join();

        List<Game> gatheredGames = new ArrayList<>();

        gatheredGames.addAll(dataCollectorEU.getGatheredGames());
        gatheredGames.addAll(dataCollectorUS.getGatheredGames());
        gatheredGames.addAll(dataCollectorAsia.getGatheredGames());


        return gatheredGames;
    }

    public void buildTest(List<Game> games){

        List<String> teamNames = new ArrayList<>();
        double[][][] strengthMatrix;


        //matrix building
        for(Game game : games) {

            TeamComp[] teams = game.getInfo().getParticipants();

            if(teams.length != 8){
                System.out.println(game.getId());
                continue;
            }

            for (TeamComp teamComp : teams) {
                if (!teamNames.contains(teamComp.getTeamName())) {
                    teamNames.add(teamComp.getTeamName());
                }
            }
        }

        strengthMatrix = new double[teamNames.size()][teamNames.size()][3];


        //fill the matrix
        for(Game game:games){
            TeamComp[] teams = game.getInfo().getParticipants();
            for (TeamComp team : teams) {

                int teamIndex = teamNames.indexOf(team.getTeamName());

                for (TeamComp enemyTeam : teams) {
                    int enemyTeamIndex = teamNames.indexOf(enemyTeam.getTeamName());


                    strengthMatrix[teamIndex][enemyTeamIndex][0] += enemyTeam.getPlacement() - team.getPlacement();
                    strengthMatrix[teamIndex][enemyTeamIndex][1]++;

                }
                strengthMatrix[teamIndex][teamIndex][2]++;
            }
        }



        //remove irrelevant teams
        for(int i=0;i< teamNames.size();i++){

            double minPercent = 0.15;

            if(strengthMatrix[i][i][2] < games.size()/100.0 * minPercent || teamNames.get(i).equals("No team")){
                for(int j=0;j<strengthMatrix.length;j++){

                    if (strengthMatrix[j] != null) {
                        strengthMatrix[j][i] = null;
                    }

                }
                strengthMatrix[i] = null;
            }

        }


        //display matrix
        /*
        for (int i = 0; i < strengthMatrix.length; i++) {
            for (int j = 0; j < strengthMatrix.length; j++) {
                if (strengthMatrix[i] != null) {
                    System.out.print(Arrays.toString(strengthMatrix[i][j]) + " ");
                }
            }
            System.out.print("\n");
        }

         */

        System.out.println("Matrix finished");

        Team[] teams = new Team[strengthMatrix.length];

        //build nodes and relationships
        for (int i=0;i<strengthMatrix.length;i++) {
            if(strengthMatrix[i] == null)
                continue;

            teams[i] = new Team(teamNames.get(i));
        }

        List<Team> validTeams = new ArrayList<>();

        for (int i = 0; i < teamNames.size(); i++) {

            if(strengthMatrix[i] == null)
                continue;

            for (int j = 0; j < teams.length; j++) {

                if(strengthMatrix[i][j] == null)
                    continue;

                TeamRelationship teamRelationship = new TeamRelationship(teams[j]);
                double strength;

                if (strengthMatrix[i][j][1] != 0)
                    strength = strengthMatrix[i][j][0] / strengthMatrix[i][j][1];
                else
                    strength = 0;

                teamRelationship.setStrength(strength);
                teams[i].addEnemyTeams(teamRelationship);
            }

            validTeams.add(teams[i]);
        }

        System.out.println(validTeams.size());

        teamRepository.deleteAll();

        System.out.println("Delete done");

        teamRepository.saveGraph(validTeams);

        System.out.println("Save done");

    }

    public void getData(){

        List<Team> teams = teamRepository.getMatchInfo( new String[]{"Bodyguard_1_Kaisa_2", "Innovator_4_Ezreal_3", "Assassin_2_Akali_2", "Syndicate_3_Akali_1", "Chemtech_3_Viktor_1", "Bruiser_2_KogMaw_2", "Sniper_3_Jhin_1", "Yordle_1_Orianna_1"});

        //System.out.println(teams);

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

        System.out.println(sorted);
    }


}
