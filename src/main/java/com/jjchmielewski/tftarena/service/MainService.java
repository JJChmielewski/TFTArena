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
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class MainService {


    private final GameRepository gameRepository;

    private final MetaTftDataCollector metaTftDataCollector;

    //team, enemy, 0 strength, 1 times played vs enemy, 2 total times played
    private double[][][] matrix;

    //team, unit, 0 sum place when in, 1 sum place when not, 2 times played, 3 times not played
    private double[][][] teamUnitMatrix;

    //unit, item, 0 times played, 1 sum of places
    private double[][][] itemOnUnitMatrix;

    //sum of places, times played
    private double[][] unitMatrix;

    //strength, times played
    private double[][] itemMatrix;

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

    private double teamWeight = 1;
    private double unitWeight = 0.5;
    private double itemWeight = 0.5;

    @Value("${tftarena.current-set}")
    private int currentSet;


    @Autowired
    public MainService(GameRepository gameRepository, MetaTftDataCollector metaTftDataCollector) {
        this.gameRepository = gameRepository;
        this.metaTftDataCollector = metaTftDataCollector;
    }

    public ResponseTeam[] predictMatch(ResponseTeam[] teams, double teamWeight, double unitWeight, double itemWeight) {
        Map<ResponseTeam, Integer> indexMap = new HashMap<>();
        double[] teamsStrength = new double[teams.length];
        double[] unitStrength = new double[teams.length];
        double[] itemStrength = new double[teams.length];
        int items = 0;
        int units = 0;

        int index = 0;
        for (ResponseTeam team : teams) {
            indexMap.put(team, index);
            index++;

            units = team.getUnits().size();

            for (ResponseUnit unit : team.getUnits()) {
                if (unit.getItems() != null) {
                    items += unit.getItems().size();
                }
            }
        }

        ResponseTeam[] teamsArray = predictMatchBasedOnTeams(Arrays.copyOf(teams,teams.length));
        for (ResponseTeam team : teamsArray) {
            teamsStrength[indexMap.get(team)] = team.getValue();
        }

        ResponseTeam[] unitsArray = predictMatchBasedOnUnits(Arrays.copyOf(teams,teams.length));
        for (ResponseTeam team : unitsArray) {
            unitStrength[indexMap.get(team)] = team.getValue();
        }
        ResponseTeam[] itemsArray = predictMatchBasedOnItems(Arrays.copyOf(teams,teams.length));
        for (ResponseTeam team : itemsArray) {
            itemStrength[indexMap.get(team)] = team.getValue();
        }



        for (int i = 0; i < teams.length; i++){
            teams[i].setValue(teamsStrength[i] * teamWeight + (itemWeight * itemStrength[i])/items  + (unitStrength[i] * unitWeight)/units);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] predictMatchBasedOnTeams(ResponseTeam[] teams){

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

    public ResponseTeam[] predictMatchBasedOnUnitsInTeam(ResponseTeam[] teams) {
        for (ResponseTeam team : teams) {
            double teamStrength = 0;
            team.setValue(0);
            if (team.getTeamName().equals("No team") || !teamNames.contains(team.getTeamName())) {
                continue;
            }

            for (ResponseUnit unit : team.getUnits()) {
                if (!unitNames.contains(unit.getApiNameWithTier())) {
                    continue;
                }

                teamStrength += teamUnitMatrix[teamNames.indexOf(team.getTeamName())][unitNames.indexOf(unit.getApiNameWithTier())][0];
            }

            team.setValue(teamStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] predictMatchBasedOnUnits(ResponseTeam[] teams) {

        for (ResponseTeam team : teams) {
            double unitStrength = 0;
            for (ResponseUnit unit : team.getUnits()) {
                if (!unitNames.contains(unit.getApiNameWithTier())) {
                    continue;
                }
                unitStrength += unitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][0] / unitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][1];
            }
            team.setValue(unitStrength);
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;
    }

    public ResponseTeam[] predictMatchBasedOnItemsOnUnits(ResponseTeam[] teams) {

        for (ResponseTeam team : teams) {
            double strength = 0;

            if (team.getUnits() != null) {
                for (ResponseUnit unit : team.getUnits()) {
                    double unitStrength = unitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][0];

                    if (unit.getItems() != null) {
                        for (ResponseItem item : unit.getItems()) {
                            if (!itemIDs.contains(item.getId())) {
                                continue;
                            }
                            unitStrength *= itemOnUnitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][itemIDs.indexOf(item.getId())][0];
                        }
                    }
                    strength += unitStrength;
                }
                team.setValue(strength);
            }
        }

        Arrays.sort(teams, Collections.reverseOrder());

        return teams;

    }

    public ResponseTeam[] predictMatchBasedOnItems(ResponseTeam[] teams) {
        for (ResponseTeam team : teams) {
            double itemStrength = 0;
            for (ResponseUnit unit : team.getUnits()) {
                for (ResponseItem item : unit.getItems()) {
                    if (!itemIDs.contains(item.getId())) {
                        continue;
                    }
                    itemStrength += itemOnUnitMatrix[unitNames.indexOf(unit.getApiNameWithTier())][itemIDs.indexOf(item.getId())][0];
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

        for(int i = 0; i<this.itemOnUnitMatrix[unitIndex].length; i++){

            CDItem temp = this.items.get(itemIDs.get(i));

            if(temp == null)
                continue;

            items.add(new ResponseItem(temp.name(), temp.id(),temp.from(), this.itemOnUnitMatrix[unitIndex][i][0]));

        }

        items.sort(Collections.reverseOrder());

        return items.subList(0,limit);

    }

    public void checkAlgorithm() {
        checkAlgorithm(gameRepository.findAll());
    }

    public void checkAlgorithm(List<Game> games){

        double gamesNumber = games.size();
        double[][] method1 = new double[8][games.size()+1];
        double method1Worked = 0;
        double[][] itemsOnly = new double[8][games.size()+1];
        double[][] unitsOnly = new double[8][games.size()+1];
        int[] maxValues = new int[]{0,2,4,8,12,18,24,32};

        for(int g=0;g<gamesNumber;g++){

            Team[] teamComps = games.get(g).getInfo().getParticipants();
            ResponseTeam[] teams = new ResponseTeam[teamComps.length];

            for(int i=0;i<teamComps.length;i++) {
                teams[i] = new ResponseTeam(teamComps[i]);
            }

            ResponseTeam[] method1Array = predictMatchBasedOnTeams(teams);
            boolean isMethod1Valid = false;
            for (int i = 0 ; i < method1Array.length; i++) {
                method1[method1Array.length - 1][g] += Math.abs(method1Array[i].getPlacement() - i - 1);
                for (ResponseTeam team : method1Array) {
                    if (team.getValue() != 0){
                        isMethod1Valid = true;
                    }
                }
            }
            if (!isMethod1Valid) {
                method1[method1Array.length - 1][g] = 0;
            } else {
                method1Worked++;
            }
            method1[method1Array.length-1][method1[method1Array.length-1].length-1] += method1[method1Array.length-1][g];
            method1[method1Array.length-1][g] /= maxValues[method1Array.length-1];


            ResponseTeam[] unitMethodArray = predictMatchBasedOnUnits(teams);
            boolean isUnitMethodValid = false;
            for (int i = 0; i < unitMethodArray.length; i++) {
                unitsOnly[unitMethodArray.length-1][g] += Math.abs(unitMethodArray[i].getPlacement() - i - 1);
                for (ResponseTeam team : unitMethodArray) {
                    if (team.getValue() != 0) {
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
                    if (team.getValue() != 0) {
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
            System.out.println(i+" Avg error: "+(method1[i][method1[i].length-1] /( maxValues[i] * method1Worked)) + " with valid: " + method1Worked/gamesNumber);
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

        List<Game> metaMatchData = new ArrayList<>();

        try {
            CSVParser csvParser = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(true).build();
            CSVReader csvReader = new CSVReaderBuilder(new FileReader("D:/MetaTFT/test_data_extracted.csv")).withSkipLines(1).withCSVParser(csvParser).build();
            String[] nextLine;

            while ((nextLine = csvReader.readNext()) != null) {
                MetaMatchData temp = new MetaMatchData(nextLine);
                metaMatchData.add(convertMetaTftToRiotApiGame(temp));
            }

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }

        checkAlgorithm(metaMatchData);
    }

    public void checkMetaTrainedAlgorithm(String datasource) {
        List<Game> testGames = new ArrayList<>();

        try {
            CSVParser csvParser = new CSVParserBuilder().withSeparator(';').withIgnoreQuotations(true).build();
            CSVReader csvReader = new CSVReaderBuilder(new FileReader(datasource)).withSkipLines(1).withCSVParser(csvParser).build();
            String[] nextLine;

            while ((nextLine = csvReader.readNext()) != null) {
                MetaMatchData metaMatchData = new MetaMatchData(nextLine);
                testGames.add(convertMetaTftToRiotApiGame(metaMatchData));
            }
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (CsvValidationException csvValidationException) {
            csvValidationException.printStackTrace();
        }
        checkMetaTrainedAlgorithm(testGames);
    }

    public void checkMetaTrainedAlgorithm() {
        checkMetaTrainedAlgorithm(gameRepository.findAll());
    }

    public void checkMetaTrainedAlgorithm(List<Game> games) {

        double allMethodSum = 0, allMethodWorked = 0;
        double allMethodExclusiveSum = 0, allMethodExclusiveWorked = 0;
        double teamMethodSum = 0, teamMethodWorked = 0;
        double teamMethodExclusiveSum = 0, teamMethodExclusiveWorked = 0;
        double unitInTeamMethodSum = 0, unitInTeamMethodWorked = 0;
        double itemUnitSum = 0, itemUnitWorked = 0;
        double unitMethodSum = 0, unitMethodWorked = 0;
        double itemMethodSum = 0, itemMethodWorked = 0;
        int numberOfTeams = 0;
        int[] maxValues = new int[]{0,1,2,4,6,9,12,16};

        for (Game game : games) {

            numberOfTeams = game.getInfo().getParticipants().length;
            ResponseTeam[] teams = new ResponseTeam[numberOfTeams];
            for (int i =0 ; i < numberOfTeams; i++) {
                teams[i] = new ResponseTeam(game.getInfo().getParticipants()[i]);
            }

            if (Arrays.stream(teams).anyMatch(team -> team.getPlacement() < 1 || team.getPlacement() > 8)) {
                continue;
            }

            ResponseTeam[] allMethodArray = predictMatch(Arrays.copyOf(teams, teams.length), teamWeight, unitWeight, itemWeight);

            for (int team = 0; team < allMethodArray.length; team++) {
                allMethodSum += Math.abs(team + 1 - allMethodArray[team].getPlacement());
                allMethodWorked++;
            }

            for (int team = 0; team < allMethodArray.length; team++) {
                if (Arrays.stream(allMethodArray).anyMatch(temp -> temp.getValue() == 0 || !teamNames.contains(temp.getTeamName()) || teamNames.equals("No team"))) {
                    break;
                }
                allMethodExclusiveSum += Math.abs(team + 1 - allMethodArray[team].getPlacement());
                allMethodExclusiveWorked++;
            }

            ResponseTeam[] teamMethodArray = predictMatchBasedOnTeams(Arrays.copyOf(teams, teams.length));

            for (int team = 0; team < teamMethodArray.length; team++) {
                teamMethodSum += Math.abs(team + 1 - teamMethodArray[team].getPlacement());
                teamMethodWorked++;
            }

            for (int team = 0; team < teamMethodArray.length; team++) {
                if (Arrays.stream(teamMethodArray).anyMatch(temp -> temp.getValue() == 0 || !teamNames.contains(temp.getTeamName()) || teamNames.equals("No team"))) {
                    break;
                }
                teamMethodExclusiveSum += Math.abs(team + 1 - teamMethodArray[team].getPlacement());
                teamMethodExclusiveWorked++;
            }

            teams = predictMatchBasedOnItemsOnUnits(teams);

            for (int team = 0; team < teams.length; team++) {
                itemUnitSum += Math.abs(team + 1 - teamMethodArray[team].getPlacement());
                itemUnitWorked++;
            }

            ResponseTeam[] unitInTeamArray = predictMatchBasedOnUnitsInTeam(Arrays.copyOf(teams, teams.length));

            for (int team = 0; team < unitInTeamArray.length; team ++) {
                if (Arrays.stream(unitInTeamArray).anyMatch(temp -> temp.getValue() == 0 || !teamNames.contains(temp.getTeamName()))) {
                    break;
                }

                unitInTeamMethodSum += Math.abs(team + 1 - unitInTeamArray[team].getPlacement());
                unitInTeamMethodWorked++;
            }

            ResponseTeam[] itemsArray = predictMatchBasedOnItems(Arrays.copyOf(teams, teams.length));

            for (int team = 0; team < itemsArray.length; team ++) {
                itemMethodSum += Math.abs(team + 1 - itemsArray[team].getPlacement());
                itemMethodWorked++;
            }

            ResponseTeam[] unitsArray = predictMatchBasedOnUnits(Arrays.copyOf(teams, teams.length));

            for (int team = 0; team < unitsArray.length; team++) {
                unitMethodSum += Math.abs(team + 1 - unitsArray[team].getPlacement());
                unitMethodWorked++;
            }
        }

        System.out.println("All method  avg error: " + allMethodSum/(allMethodWorked * maxValues[numberOfTeams - 1])+ " with teams detected: " + allMethodWorked/(games.size() * numberOfTeams));
        System.out.println("All method exclusive avg error: " + allMethodExclusiveSum/(allMethodExclusiveWorked * maxValues[numberOfTeams - 1]) + " with teams detected: " + allMethodExclusiveWorked/(games.size() * numberOfTeams));
        System.out.println("Team method exclusive avg error: " + teamMethodExclusiveSum/(teamMethodExclusiveWorked * maxValues[numberOfTeams - 1]) + " with teams detected: " + teamMethodExclusiveWorked/(games.size() * numberOfTeams));
        System.out.println("Team method avg error: " + teamMethodSum/(teamMethodWorked * maxValues[numberOfTeams - 1]) + " with teams detected: " + teamMethodWorked/(games.size() * numberOfTeams));
        System.out.println("Unit in team method avg error: " + unitInTeamMethodSum/(unitInTeamMethodWorked * maxValues[numberOfTeams - 1]) + " with teams detected: " + unitInTeamMethodWorked/(games.size() * numberOfTeams));
        System.out.println("Unit method avg error: " + itemMethodSum/(itemMethodWorked * maxValues[numberOfTeams - 1]));
        System.out.println("Item method avg error: " + unitMethodSum/(unitMethodWorked * maxValues[numberOfTeams - 1]));
        System.out.println("Item on unit method avg error: " + itemUnitSum/(itemUnitWorked * maxValues[numberOfTeams - 1]));
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
                              double[][][] teamUnitMatrix, double[][][] itemMatrix,
                              List<String> unitNames, List<Integer> itemIndexes,
                              int totalGames, double[][] unitMatrix){

        this.matrix = matrix;
        this.teamNames = teamNames;
        this.teamUnitMatrix = teamUnitMatrix;
        this.itemOnUnitMatrix = itemMatrix;
        this.unitNames = unitNames;
        this.itemIDs = itemIndexes;
        this.totalGames= totalGames;
        this.unitMatrix = unitMatrix;
    }

    public void setMatrixData(double[][][] teamMatrix, List<String> teamNames,
                              double[][][] unitInTeamMatrix, double[][][] itemOnUnitMatrix,
                              List<String> unitNames, List<Integer> itemIndexes,
                              double[][] unitMatrix, double[][] itemMatrix) {
        this.matrix = teamMatrix;
        this.teamNames = teamNames;
        this.teamUnitMatrix = unitInTeamMatrix;
        this.itemOnUnitMatrix = itemOnUnitMatrix;
        this.unitNames = unitNames;
        this.itemIDs = itemIndexes;
        this.unitMatrix = unitMatrix;
        this.itemMatrix = itemMatrix;
    }

    private Team convertMetaTftToRiotApiTeam(MetaUnit[] playerUnits, String[] playerTraits, double healthLost) {

        Map<String, Integer> newTeamTraits = new HashMap<>();

        if (playerTraits == null || playerTraits == null) {
            return new Team();
        }

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
        resultTeam.setPlacement(healthLost >= 0 ? 1 : 2);
        resultTeam.setHealthLost(healthLost);

        return resultTeam;
    }

    public Game convertMetaTftToRiotApiGame(MetaMatchData matchData) {
        Team player, opponent;
        player = convertMetaTftToRiotApiTeam(matchData.getPlayer_units(), matchData.getPlayer_full_traits(), matchData.getPlayer_health_lost());
        opponent = convertMetaTftToRiotApiTeam(matchData.getOpponent_units(), matchData.getOpponent_full_traits(), matchData.getOpponent_health_lost());

        GameInfo gameInfo = new GameInfo();
        gameInfo.setParticipants(new Team[]{player, opponent});
        Game game = new Game();
        game.setInfo(gameInfo);
        return game;
    }

    private void attunePredictMatchWeights(List<Game> games) {

        double lowestTeamWeight = 0, lowestItemWeight = 0, lowestUnitWeight = 0, lowestError = 1;

        for (double teamWeight = 0; teamWeight <= 1; teamWeight += 0.1) {
            for (double unitWeight = 0; unitWeight <= 1; unitWeight += 0.1) {
                for (double itemWeight = 0; itemWeight <= 1; itemWeight += 0.1) {
                    double allMethodSum = 0, allMethodWorked = 0;

                    for (Game game : games) {

                        ResponseTeam[] teams = new ResponseTeam[game.getInfo().getParticipants().length];

                        for (int i = 0 ; i < game.getInfo().getParticipants().length; i++) {
                            teams[i] = new ResponseTeam(game.getInfo().getParticipants()[i]);
                        }

                        if (Arrays.stream(teams).anyMatch(team -> team.getValue() < 1 || team.getPlacement() > 8)) {
                            continue;
                        }

                        ResponseTeam[] allMethodArray = predictMatch(Arrays.copyOf(teams, teams.length), teamWeight, unitWeight, itemWeight);

                        for (int team = 0; team < allMethodArray.length; team++) {
                            allMethodSum += Math.abs(team + 1 - allMethodArray[team].getPlacement());
                            allMethodWorked++;
                        }
                    }

                    if (allMethodSum/allMethodWorked < lowestError) {
                        lowestError = allMethodSum/allMethodWorked;
                        lowestTeamWeight = teamWeight;
                        lowestUnitWeight = unitWeight;
                        lowestItemWeight = itemWeight;
                    }
                }
            }
        }
        System.out.println(String.format("Lowest error: %s with team weight: %s unit weight: %s and item weight: %s", lowestError, lowestTeamWeight, lowestUnitWeight, lowestItemWeight));
        this.teamWeight = lowestTeamWeight;
        this.unitWeight = lowestUnitWeight;
        this.itemWeight = lowestItemWeight;
    }
}
