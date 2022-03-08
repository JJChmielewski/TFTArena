package com.jjchmielewski.tftarena.graphBuilder;

import com.jjchmielewski.tftarena.entitis.documents.TeamComp;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.entitis.documents.unit.Unit;
import com.jjchmielewski.tftarena.entitis.nodes.Item;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.UnitNode;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamUnitRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.UnitItemRelationship;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.repository.TeamRepository;
import com.jjchmielewski.tftarena.services.MainService;
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

    private final long setBeginning;

    private final MainService mainService;


    @Autowired
    public GraphBuilder(TeamRepository teamRepository, GameRepository gameRepository,
                        @Value("${tftarena.riot-games.api-key}") String apiKey,
                        @Value("${tftarena.buildGraph}") boolean buildGraph,
                        @Value("${tftarena.saveGames}") boolean saveGames,
                        @Value("${tftarena.collectData}") boolean collectData,
                        @Value("${tftarena.setBeginning}") long setBeginning, MainService mainService) {

        this.teamRepository = teamRepository;
        this.gameRepository = gameRepository;
        this.apiKey=apiKey;
        this.buildGraph = buildGraph;
        this.saveGames = saveGames;
        this.collectData = collectData;
        this.setBeginning=setBeginning;
        this.mainService = mainService;
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

        mainService.checkAlgorithm();
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


        DataCollector dataCollectorEU = new DataCollector(this.gameRepository,apiKey,urlDiamondEU,urlSummonersEU,urlSummonerMatchesEU,urlMatchDetailsEU,saveGames, setBeginning);
        DataCollector dataCollectorUS = new DataCollector(this.gameRepository,apiKey,urlDiamondUS,urlSummonersUS,urlSummonerMatchesUS,urlMatchDetailsUS,saveGames, setBeginning);
        DataCollector dataCollectorAsia = new DataCollector(this.gameRepository,apiKey,urlDiamondAsia,urlSummonersAsia,urlSummonerMatchesAsia,urlMatchDetailsAsia,saveGames, setBeginning);

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
        List<String> unitNames = new ArrayList<>();
        List<Integer> itemIndexes = new ArrayList<>();
        double[][][] strengthMatrix;
        double[][][] unitMatrix;
        int[][][] itemMatrix;

        System.out.println("Starting");

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

                for(Unit unit: teamComp.getUnits()){
                    if(!unitNames.contains(unit.getCharacter_id()+"_"+unit.getTier())){
                        unitNames.add(unit.getCharacter_id() + "_" + unit.getTier());
                    }

                    for(int item: unit.getItems()){
                        if(!itemIndexes.contains(item))
                            itemIndexes.add(item);
                    }

                }
            }
        }

        //placement diff, times vs this team, times played over all (not 0 on the diagonal)
        strengthMatrix = new double[teamNames.size()][teamNames.size()][3];

        //sum place when in, sum place when not, times played, times not played
        unitMatrix = new double[teamNames.size()][unitNames.size()][4];

        //times played, times not played
        itemMatrix = new int[unitNames.size()][itemIndexes.size()][1];

        System.out.println(unitNames.size());

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

                //analysing units in team
                List<String> teamUnits = new ArrayList<>();

                //preping units, sorting out items
                for(Unit unit : team.getUnits()) {
                    String teamName = unit.getCharacter_id() + "_" + unit.getTier();
                    teamUnits.add(teamName);

                    //item matrix
                    for(int item : unit.getItems()){

                        if(itemIndexes.contains(item)){
                            itemMatrix[unitNames.indexOf(teamName)][itemIndexes.indexOf(item)][0]++;
                        }
                    }
                }

                for(int unitIndex=0;unitIndex<unitNames.size();unitIndex++){

                    if(teamUnits.contains(unitNames.get(unitIndex))){

                        unitMatrix[teamIndex][unitIndex][0] += team.getPlacement();
                        unitMatrix[teamIndex][unitIndex][2]++;
                    }
                    else {
                        unitMatrix[teamIndex][unitIndex][1] += team.getPlacement();
                        unitMatrix[teamIndex][unitIndex][3]++;
                    }
                }


                strengthMatrix[teamIndex][teamIndex][2]++;
            }
        }


        //remove irrelevant teams
        /*
        for(int i=0;i< teamNames.size();i++){

            double minPercent = 0;

            if(strengthMatrix[i][i][2] <= games.size()/100.0 * minPercent || teamNames.get(i).equals("No team")){
                for(int j=0;j<strengthMatrix.length;j++){

                    if (strengthMatrix[j] != null) {
                        strengthMatrix[j][i] = null;
                    }

                }
                strengthMatrix[i] = null;
            }
        }*/


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
        UnitNode[] unitNodes = new UnitNode[unitNames.size()];
        Item[] items = new Item[itemIndexes.size()];

        //build nodes
        for (int i=0;i<strengthMatrix.length;i++) {

            teams[i] = new Team(teamNames.get(i));
        }
        for(int j=0;j<unitNodes.length;j++){

            unitNodes[j] = new UnitNode(unitNames.get(j));

        }
        for(int k=0;k<items.length;k++){
            items[k] = new Item(itemIndexes.get(k));
        }


        List<Team> validTeams = new ArrayList<>();

        //build relationships
        for (int i = 0; i < teamNames.size(); i++) {

            if(strengthMatrix[i] == null)
                continue;

            //team-team relationship
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

            //team-unit relationship
            for(int j=0; j < unitNodes.length;j++){

                if(unitMatrix[i][j][2] < 1)
                    continue;

                double weight, percentagePlayed;

                if(unitMatrix[i][j][3]!=0){
                    weight = (unitMatrix[i][j][1] / unitMatrix[i][j][3]) - (unitMatrix[i][j][0] / unitMatrix[i][j][2]);
                }
                else {
                    weight = 8 - (unitMatrix[i][j][0] / unitMatrix[i][j][2]);
                }

                percentagePlayed = unitMatrix[i][j][2]/(unitMatrix[i][j][2]+unitMatrix[i][j][3]);

                TeamUnitRelationship teamUnitRelationship = new TeamUnitRelationship( weight, percentagePlayed, unitNodes[j]);

                teams[i].addUnit(teamUnitRelationship);
            }

            validTeams.add(teams[i]);
        }
        //unit-item relationship
        for(int i=0; i< unitNodes.length;i++){
            for(int j=0;j< items.length;j++){
                if(itemMatrix[i][j][0] < 1)
                    continue;

                unitNodes[i].addItem(new UnitItemRelationship(itemMatrix[i][j][0], items[j]));
            }
        }

        System.out.println(validTeams.size());
        System.out.println(unitNodes.length);
        System.out.println(items.length);

        teamRepository.deleteAll();

        System.out.println("Delete done");

        teamRepository.saveGraph(validTeams, unitNodes, items);

        System.out.println("Save done");

    }


}
