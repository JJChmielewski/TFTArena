package com.jjchmielewski.tftarena.service;

import com.jjchmielewski.tftarena.communitydragon.CDItem;
import com.jjchmielewski.tftarena.communitydragon.CDTrait;
import com.jjchmielewski.tftarena.communitydragon.CDUnit;
import com.jjchmielewski.tftarena.communitydragon.stats.CDEffect;
import com.jjchmielewski.tftarena.metatft.MetaMatchData;
import com.jjchmielewski.tftarena.metatft.MetaTftDataCollector;
import com.jjchmielewski.tftarena.metatft.MetaUnit;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.responses.ResponseItem;
import com.jjchmielewski.tftarena.responses.ResponseTeam;
import com.jjchmielewski.tftarena.responses.ResponseUnit;
import com.jjchmielewski.tftarena.riotapi.GameInfo;
import com.jjchmielewski.tftarena.riotapi.Team;
import com.jjchmielewski.tftarena.riotapi.entities.Game;
import com.jjchmielewski.tftarena.riotapi.unit.Trait;
import com.jjchmielewski.tftarena.riotapi.unit.Unit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MainService {


    private final GameRepository gameRepository;

    //team, enemy, 0 strength, 1 times played vs enemy, 2 total times played
    private double[][][] matrix;

    //team, unit, 0 sum place when in, 1 sum place when not, 2 times played, 3 times not played
    private double[][][] teamUnitMatrix;

    //unit, item, 0 times played, 1 sum of places
    private int[][][] itemMatrix;

    //sum of places, times played
    private double[][] unitMatrix;

    //indexes of matrices
    private List<String> teamNames;

    private List<String> unitNames;

    private List<Integer> itemIDs;

    private int totalGames;

    //key name
    private Map<String, CDTrait> traits;

    //key apiName
    private Map<String, CDUnit> units;

    //key id
    private Map<Integer, CDItem> items;

    @Value("${tftarena.current-set}")
    private int currentSet;


    @Autowired
    public MainService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public ResponseTeam[] predictMatch(ResponseTeam[] teams){

        String[] teamNames = new String[teams.length];
        for (int i = 0; i < teams.length; i++) {
            teamNames[i] = teams[i].getTeamName();
        }

        int[] indexes = getTeamsIndexes(teamNames);

        for(int i=0;i<teams.length;i++){
            double tempStrength=0.0;

            for(int j=0;j<teams.length;j++){
                tempStrength+=matrix[indexes[i]][indexes[j]][0];
            }
            teams[i].setValue(tempStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] predictMatchBasedOnUnits(ResponseTeam[] teams) {

        for (ResponseTeam team : teams) {
            double unitStrength = 0;
            for (ResponseUnit unit : team.getUnits()) {
                unitStrength += unitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][0] / unitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][1];
            }
            team.setValue(unitStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] predictMatchBasedOnItems(ResponseTeam[] teams) {
        for (ResponseTeam team : teams) {
            double itemStrength = 0;
            for (ResponseUnit unit : team.getUnits()) {
                for (ResponseItem item : unit.getItems()) {
                    itemStrength += itemMatrix[unitNames.indexOf(unit.getApiNameWithTier())][itemIDs.indexOf(item.getId())][0];
                }
            }
            team.setValue(itemStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] findBestTeams(String[] enemyTeams, double minPercentagePlayed){

        int[] enemyIndexes = getTeamsIndexes(enemyTeams);

        ResponseTeam[] teams = new ResponseTeam[matrix.length];

        for(int i=0;i<matrix.length;i++){

            double teamStrength = 0;

            if(matrix[i][i][2]/totalGames < minPercentagePlayed){

                teamStrength = -7;

                teams[i] = new ResponseTeam(teamNames.get(i), teamStrength);
                continue;
            }

            for (int enemyIndex : enemyIndexes) {

                teamStrength += matrix[i][enemyIndex][0];

            }

            teams[i] = new ResponseTeam(teamNames.get(i), teamStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;

    }

    public ResponseTeam[] findBestTeams(String[] enemyTeams, double minPercentagePlayed, int limit){

        if(limit < 0)
            return null;

        ResponseTeam[] teams = this.findBestTeams(enemyTeams, minPercentagePlayed);

        return Arrays.copyOf(teams, limit);

    }

    public ResponseTeam buildTeam(String teamName, int level, double minPercentagePlayed){

        //1 trait, 2 style, 4 unit, 5 star
        String[] teamNameSplit = teamName.split("_");

        int teamIndex = teamNames.indexOf(teamName);
        CDEffect teamEffect = getTeamEffect(teamNameSplit[1], Integer.parseInt(teamNameSplit[2]));

        List<ResponseUnit> teamUnits = new ArrayList<>();

        List<ResponseUnit> validUnits = new ArrayList<>();
        List<ResponseUnit> unitsWithTrait = new ArrayList<>();
        ResponseUnit mainUnit = null;

        for(int u = 0; u< teamUnitMatrix[teamIndex].length; u++){

            if(teamUnitMatrix[teamIndex][u][2]/(teamUnitMatrix[teamIndex][u][2] + teamUnitMatrix[teamIndex][u][3]) >= minPercentagePlayed){

                double[] unitData = teamUnitMatrix[teamIndex][u];

                double value;

                String unitName = unitNames.get(u);

                if(unitData[2] == 0)
                    continue;

                if(unitData[3] == 0)
                    value= 8 - unitData[0]/unitData[2];
                else
                    value= (unitData[1]/unitData[3]) - (unitData[0]/unitData[2]);

                ResponseUnit unit = new ResponseUnit(unitName, value);

                if(unitName.equals(teamNameSplit[3]+"_"+teamNameSplit[4]+"_"+teamNameSplit[5])) {
                    mainUnit = unit;
                    continue;
                }

                String[] unitNameSplit = unitName.split("_");

                for(String traitName : this.units.get(unitNameSplit[0]+"_"+unitNameSplit[1]).traits()){
                    if(teamNameSplit[1].equals(traitName)){
                        unitsWithTrait.add(unit);
                    }
                }

                validUnits.add(unit);
            }
        }

        validUnits.sort(Collections.reverseOrder());
        unitsWithTrait.sort(Collections.reverseOrder());

        int traitUnitsInTeam = 0;

        List<String> teamUnitNames = new ArrayList<>();

        for(int l=0;teamUnits.size()<level;l++){

            if(l-teamUnits.size() >= validUnits.size())
                break;

            String[] unitSplit;

            if(teamUnits.size() < teamEffect.minUnits() && l<unitsWithTrait.size())
                unitSplit = unitsWithTrait.get(l).getApiNameWithTier().split("_");
            else
                unitSplit = validUnits.get(l-teamUnits.size()).getApiNameWithTier().split("_");

            if(!teamUnitNames.contains(unitSplit[1])){

                if(teamUnits.size() < teamEffect.minUnits() && l < unitsWithTrait.size()){
                    teamUnitNames.add(unitSplit[1]);
                    teamUnits.add(unitsWithTrait.get(l));
                    traitUnitsInTeam++;
                    continue;
                }

                if(!teamUnits.contains(mainUnit) && mainUnit != null){
                    teamUnitNames.add(mainUnit.getApiNameWithTier().split("_")[1]);
                    teamUnits.add(mainUnit);
                    continue;
                }

                if(!teamUnits.contains(validUnits.get(l-teamUnits.size()))){

                    if(unitsWithTrait.contains(validUnits.get(l-teamUnits.size()))){

                        if(traitUnitsInTeam < teamEffect.maxUnits()){

                            traitUnitsInTeam++;
                            teamUnits.add(validUnits.get(l-teamUnits.size()));
                        }

                    }
                    else
                        teamUnits.add(validUnits.get(l-teamUnits.size()));

                }

                teamUnitNames.add(unitSplit[1]);
            }

        }

        return new ResponseTeam(teamName , teamUnits);
    }

    public List<ResponseItem> findBestItemsForUnit(String unitName, int limit){

        int unitIndex = this.unitNames.indexOf(unitName);

        List<ResponseItem> items = new ArrayList<>();

        for(int i=0;i<this.itemMatrix[unitIndex].length;i++){

            CDItem temp = this.items.get(itemIDs.get(i));

            if(temp == null)
                continue;

            items.add(new ResponseItem(temp.name(), temp.id(),temp.from(),this.itemMatrix[unitIndex][i][0]));

        }

        items.sort(Collections.reverseOrder());

        return items.subList(0,limit);

    }

    public void checkAlgorithm() {
        checkAlgorithm(gameRepository.findAll());
    }

    public void checkAlgorithm(List<Game> games){

        int gamesNumber = games.size();
        double[][] method1 = new double[8][gamesNumber+1];
        double[][] itemsOnly = new double[8][gamesNumber+1];
        double[][] unitsOnly = new double[8][gamesNumber+1];
        int[] maxValues = new int[]{0,2,4,8,12,18,24,32};

        for(int g=0;g<gamesNumber;g++){

            Team[] teamComps = games.get(g).getInfo().getParticipants();
            ResponseTeam[] teams = new ResponseTeam[teamComps.length];

            for(int i=0;i<teamComps.length;i++) {
                teams[i] = new ResponseTeam(teamComps[i]);
            }

            ResponseTeam[] method1Array = predictMatch(teams);
            boolean isMethod1Valid = false;
            for (int i = 0 ; i < method1Array.length; i++) {
                method1[method1Array.length - 1][g] += Math.abs(method1Array[i].getPlacement() - i - 1);
                for (ResponseTeam team : method1Array) {
                    if (team.getValue() > 0){
                        isMethod1Valid = true;
                    }
                }
            }
            if (!isMethod1Valid) {
                method1[method1Array.length - 1][g] = 0;
            }
            method1[method1Array.length-1][method1[method1Array.length-1].length-1] += method1[method1Array.length-1][g];
            method1[method1Array.length-1][g] /= maxValues[method1Array.length-1];


            ResponseTeam[] unitMethodArray = predictMatchBasedOnUnits(teams);
            boolean isUnitMethodValid = false;
            for (int i = 0; i < unitMethodArray.length; i++) {
                unitsOnly[unitMethodArray.length-1][g] += Math.abs(unitMethodArray[i].getPlacement() - i - 1);
                for (ResponseTeam team : unitMethodArray) {
                    if (team.getValue() > 0) {
                        isUnitMethodValid = true;
                    }
                }
            }
            if (!isUnitMethodValid) {
                unitsOnly[unitMethodArray.length-1][g] = 0;
            }
            unitsOnly[unitMethodArray.length-1][unitsOnly[unitMethodArray.length-1].length-1] += unitsOnly[unitMethodArray.length-1][g];
            unitsOnly[unitMethodArray.length-1][g] /= maxValues[unitMethodArray.length-1];


            ResponseTeam[] itemMethodArray = predictMatchBasedOnItems(teams);
            boolean isItemMethodValid = false;
            for (int i = 0; i < itemMethodArray.length; i++) {
                itemsOnly[itemMethodArray.length - 1][g] += Math.abs(itemMethodArray[i].getPlacement() - i - 1);
                for (ResponseTeam team : itemMethodArray) {
                    if (team.getValue() > 0) {
                        isItemMethodValid = true;
                    }
                }
            }
            if (!isItemMethodValid) {
                itemsOnly[itemMethodArray.length - 1][g] = 0;
            }
            itemsOnly[itemMethodArray.length-1][itemsOnly[itemMethodArray.length-1].length-1] += itemsOnly[itemMethodArray.length-1][g];
            itemsOnly[itemMethodArray.length-1][g] /= maxValues[itemMethodArray.length-1];
        }

        for(int i=1; i<8;i++){
            System.out.println(i+" Avg error: "+(method1[i][method1[i].length-1] /( maxValues[i] * (method1[i].length-1))));
        }

        System.out.println("\n");

        for(int i=1; i<8;i++){
            System.out.println(i+" Avg unit error: "+(unitsOnly[i][unitsOnly[i].length-1] /( maxValues[i] * (unitsOnly[i].length-1))));
        }

        System.out.println("\n");

        for(int i=1; i<8;i++){
            System.out.println(i+" Avg item error: "+(itemsOnly[i][itemsOnly[i].length-1] /( maxValues[i] * (itemsOnly[i].length-1))));
        }
    }

    public void checkAlgorithmWithMetaTftData() {

        MetaMatchData[] metaMatchData = MetaTftDataCollector.getMetaTftMatchData();
        List<Game> games = new ArrayList<>();

        for (MetaMatchData matchData : metaMatchData) {
            Team player, opponent;
            player = convertMetaTftToRiotApi(matchData.getPlayer_units(), matchData.getPlayer_full_traits(), matchData.getWin() == 1 ? 1 : 2);
            opponent = convertMetaTftToRiotApi(matchData.getOpponent_units(), matchData.getOpponent_full_traits(), matchData.getWin() == 0 ? 1 : 2);

            GameInfo gameInfo = new GameInfo();
            gameInfo.setParticipants(new Team[]{player, opponent});
            Game game = new Game();
            game.setInfo(gameInfo);
            games.add(game);
        }

        checkAlgorithm(games);
    }

    private int[] getTeamsIndexes(String[] teams){
        int[] indexes = new int[teams.length];

        for(int i=0;i< teams.length;i++){

            if(teamNames.contains(teams[i]))
                indexes[i] = teamNames.indexOf(teams[i]);
            else
                indexes[i] = teamNames.indexOf("No team");
        }

        return indexes;
    }

    private CDEffect getTeamEffect(String traitName, int style){

        for(CDEffect effect : this.traits.get(traitName).effects()){

            if(effect.style() == style)
                return effect;
        }

        throw new RuntimeException("No such effect");

    }

    public void setTraits(CDTrait[] traits) {

        this.traits = new HashMap<>();

        for (CDTrait trait : traits) {
            this.traits.put(trait.name(), trait);
        }
    }

    public void setUnits(CDUnit[] units) {
        this.units = new HashMap<>();

        for (CDUnit unit : units) {
            this.units.put(unit.apiName(), unit);
        }

    }

    public void setItems(CDItem[] items){

        this.items = new HashMap<>();

        for(CDItem item : items){

            if(item.id() <= 99 && item.id() > 0 && item.id()/10 <= item.id()%10)
                this.items.put(item.id(), item);

        }
    }

    public void setMatrixData(double[][][] matrix, List<String> teamNames,
                              double[][][] teamUnitMatrix, int[][][] itemMatrix,
                              List<String> unitNames, List<Integer> itemIndexes,
                              int totalGames, double[][] unitMatrix){

        this.matrix = matrix;
        this.teamNames = teamNames;
        this.teamUnitMatrix = teamUnitMatrix;
        this.itemMatrix = itemMatrix;
        this.unitNames = unitNames;
        this.itemIDs = itemIndexes;
        this.totalGames= totalGames;
        this.unitMatrix = unitMatrix;
    }

    private Team convertMetaTftToRiotApi(MetaUnit[] playerUnits, String[] playerTraits, int placement) {

        Map<String, Integer> newTeamTraits = new HashMap<>();

        for (MetaUnit unit : playerUnits) {
            CDUnit cdUnit = units.get(unit.getUnit());

            for (String trait : cdUnit.traits()) {
                if (newTeamTraits.containsKey(trait)) {
                    newTeamTraits.put(trait, newTeamTraits.get(trait)+1);
                } else {
                    newTeamTraits.put(trait, 0);
                }
            }
        }

        List<Trait> riotTraits = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : newTeamTraits.entrySet()) {
            Trait trait = new Trait();

            trait.setName("TFT"+currentSet+"_"+entry.getKey());
            trait.setNum_units(entry.getValue());

            for (String playerTrait : playerTraits) {
                if (playerTrait.split("_")[0].equals(entry.getKey())) {
                    trait.setStyle(Integer.parseInt(playerTrait.split("_")[1]));
                    break;
                }
            }

            if (trait.getStyle() > 0) {
                riotTraits.add(trait);
            }
        }
        Unit[] riotUnits = new Unit[playerUnits.length];

        for (int i=0; i< playerUnits.length; i++) {
            riotUnits[i] = new Unit(playerUnits[i]);
        }

        Team resultTeam = new Team();
        resultTeam.setUnits(riotUnits);
        resultTeam.setTraits(riotTraits.toArray(new Trait[0]));
        resultTeam.setPlacement(placement);

        return resultTeam;
    }
}
