package com.jjchmielewski.tftarena.matrixbuilder;

import com.jjchmielewski.tftarena.communitydragon.CommunityDragonHandler;
import com.jjchmielewski.tftarena.metatft.MetaMatchData;
import com.jjchmielewski.tftarena.metatft.MetaTftDataCollector;
import com.jjchmielewski.tftarena.riotapi.Team;
import com.jjchmielewski.tftarena.riotapi.entities.Game;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.service.MainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class MatrixBuilder implements Runnable{

    private final String apiKey;

    private final GameRepository gameRepository;

    private final boolean buildGraph;

    private final boolean saveGames;

    private final boolean collectData;

    private final long setBeginning;

    private final MainService mainService;

    private final CommunityDragonHandler communityDragonHandler;


    @Autowired
    public MatrixBuilder(GameRepository gameRepository,
                         @Value("${tftarena.buildGraph}") boolean buildGraph,
                         @Value("${tftarena.saveGames}") boolean saveGames,
                         @Value("${tftarena.collectData}") boolean collectData,
                         @Value("${tftarena.setBeginning}") long setBeginning, MainService mainService, CommunityDragonHandler communityDragonHandler) {

        this.gameRepository = gameRepository;
        this.communityDragonHandler = communityDragonHandler;
        this.apiKey=System.getenv("RIOT_KEY");
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

        communityDragonHandler.readCommunityDragon();
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

                buildMatrices(gatheredGames);

            }catch (Exception e){
                e.printStackTrace();
            }
        }

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

    public void buildMatrices(List<Game> games){

        List<String> teamNames = new ArrayList<>();
        List<String> unitNames = new ArrayList<>();
        List<Integer> itemIndexes = new ArrayList<>();
        double[][][] strengthMatrix;
        double[][][] teamUnitMatrix;
        int[][][] itemMatrix;
        double[][] unitMatrix;

        System.out.println("Starting");

        //matrix building
        for(Game game : games) {

            Team[] teams = game.getInfo().getParticipants();

            if(teams.length != 8){
                System.out.println(game.getId());
                continue;
            }

            for (Team team : teams) {
                if (!teamNames.contains(team.getTeamName())) {
                    teamNames.add(team.getTeamName());
                }

                for(Unit unit: team.getUnits()){
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
        teamUnitMatrix = new double[teamNames.size()][unitNames.size()][4];

        //times played
        itemMatrix = new int[unitNames.size()][itemIndexes.size()][1];

        //sum of place, times played
        unitMatrix = new double[unitNames.size()][2];

        System.out.println(unitNames.size());

        //fill the matrix
        for(Game game:games){
            Team[] teams = game.getInfo().getParticipants();
            for (Team team : teams) {

                int teamIndex = teamNames.indexOf(team.getTeamName());

                for (Team enemyTeam : teams) {
                    int enemyTeamIndex = teamNames.indexOf(enemyTeam.getTeamName());


                    strengthMatrix[teamIndex][enemyTeamIndex][0] += enemyTeam.getPlacement() - team.getPlacement();
                    strengthMatrix[teamIndex][enemyTeamIndex][1]++;

                }

                //analysing units in team
                List<String> teamUnits = new ArrayList<>();

                //preparing units, sorting out items
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

                        teamUnitMatrix[teamIndex][unitIndex][0] += team.getPlacement();
                        teamUnitMatrix[teamIndex][unitIndex][2]++;

                        unitMatrix[unitIndex][0] += team.getPlacement();
                        unitMatrix[unitIndex][1]++;
                    }
                    else {
                        teamUnitMatrix[teamIndex][unitIndex][1] += team.getPlacement();
                        teamUnitMatrix[teamIndex][unitIndex][3]++;
                    }
                }


                strengthMatrix[teamIndex][teamIndex][2]++;
            }
        }

        for(int i=0; i<strengthMatrix.length;i++){
            for(int j=0;j<strengthMatrix.length;j++){

                if(strengthMatrix[i][j][1] != 0)
                    strengthMatrix[i][j][0] /=strengthMatrix[i][j][1];
                else
                    strengthMatrix[i][j][0] = 0;
            }
        }

        mainService.setMatrixData(strengthMatrix, teamNames,teamUnitMatrix,itemMatrix,unitNames,itemIndexes,games.size(), unitMatrix);

        System.out.println("Matrices built");
    }
}
