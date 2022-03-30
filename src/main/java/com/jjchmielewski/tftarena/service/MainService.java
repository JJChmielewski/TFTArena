package com.jjchmielewski.tftarena.service;

import com.jjchmielewski.tftarena.entitis.documents.Team;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.entitis.documents.unit.Item;
import com.jjchmielewski.tftarena.entitis.documents.unit.Trait;
import com.jjchmielewski.tftarena.entitis.documents.unit.Unit;
import com.jjchmielewski.tftarena.entitis.documents.unit.stats.Effect;
import com.jjchmielewski.tftarena.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class MainService {


    private final GameRepository gameRepository;

    //team, enemy, 0 strength, 1 times played vs enemy, 2 total times played
    protected static double[][][] matrix;

    //team, unit, 0 sum place when in, 1 sum place when not, 2 times played, 3 times not played
    protected static double[][][] unitMatrix;

    //unit, item, 0 times played
    protected static int[][][] itemMatrix;

    //indexes of matrices
    protected static List<String> teamNames;

    protected static List<String> unitNames;

    protected static List<Integer> itemIndexes;

    protected static int totalGames;

    private Trait[] traits;

    private Unit[] units;

    private Item[] items;


    @Autowired
    public MainService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Pair<String, Double>[] predictMatch(String[] teams){

        int[] indexes = getTeamsIndexes(teams);

       Pair<String, Double>[] strength = new Pair[teams.length];

        for(int i=0;i<teams.length;i++){
            double tempStrength=0.0;

            for(int j=0;j<teams.length;j++){
                tempStrength+=matrix[indexes[i]][indexes[j]][0];
            }

            Pair<String, Double> temp = Pair.of(teams[i], tempStrength);

            strength[i] = temp;
        }

        strength = this.sortPairsDesc(strength);

        return strength;
    }

    public Pair<String,Double>[] findBestTeams(String[] enemyTeams, double minPercentagePlayed){

        int[] enemyIndexes = getTeamsIndexes(enemyTeams);

        Pair<String,Double>[] teams = new Pair[matrix.length];

        for(int i=0;i<matrix.length;i++){

            double teamStrength = 0;

            if(matrix[i][i][2]/totalGames < minPercentagePlayed){
                teamStrength = -7;
                Pair<String, Double> team = Pair.of(teamNames.get(i), teamStrength);

                teams[i] = team;
                continue;
            }

            for(int e=0;e<enemyIndexes.length;e++){

                teamStrength+=matrix[i][enemyIndexes[e]][0];

            }

            Pair<String, Double> team = Pair.of(teamNames.get(i), teamStrength);

            teams[i] = team;
        }

        teams = this.sortPairsDesc(teams);

        return teams;

    }

    public Pair<String,Double>[] findBestTeams(String[] enemyTeams, double minPercentagePlayed, int limit){

        if(limit < 0)
            return null;

        Pair<String,Double>[] teams = this.findBestTeams(enemyTeams, minPercentagePlayed);

        Pair<String,Double>[] returned = new Pair[limit];

        if(teams == null)
            return null;

        for(int i=0; i < limit; i++){
            returned[i] = teams[i];
        }

        return returned;
    }

    public List<Pair<String, Double>> buildTeam(String teamName,int level, double minPercentagePlayed){

        //1 trait, 2 style, 4 unit, 5 star
        String[] teamNameSplit = teamName.split("_");
        int teamIndex = teamNames.indexOf(teamName);
        Effect teamEffect = getTeamEffect(teamNameSplit[0]+"_"+teamNameSplit[1], Integer.parseInt(teamNameSplit[2]));

        List<Pair<String, Double>> team = new ArrayList<>();

        List<Pair<String, Double>> validUnits = new ArrayList<>();
        List<Pair<String, Double>> unitsWithTrait = new ArrayList<>();
        Pair<String, Double> mainUnit = null;

        for(int u=0;u<unitMatrix[teamIndex].length;u++){

            if(unitMatrix[teamIndex][u][2]/(unitMatrix[teamIndex][u][2] + unitMatrix[teamIndex][u][3]) >= minPercentagePlayed){

                double[] unitData = unitMatrix[teamIndex][u];

                double value;

                if(unitData[2] == 0)
                    continue;

                if(unitData[3] == 0)
                    value=8 - unitData[0]/unitData[2];
                else
                    value=(unitData[1]/unitData[3]) - (unitData[0]/unitData[2]);

                Pair<String, Double> unit = Pair.of(unitNames.get(u), value);

                if(unit.getFirst().equals(teamNameSplit[3]+"_"+teamNameSplit[4]+"_"+teamNameSplit[5]))
                    mainUnit = unit;

                String[] unitNameSplit = unit.getFirst().split("_");

                for(int i=0; i<units.length;i++){

                    if(units[i].getApiName().equals(unitNameSplit[0]+"_"+unitNameSplit[1])){

                        for(int t=0;t<units[i].getTraits().length;t++){
                            if(teamNameSplit[1].equals(units[i].getTraits()[t])){
                                unitsWithTrait.add(unit);
                            }
                        }

                    }

                }
                validUnits.add(unit);
            }
        }

        validUnits = Arrays.asList(sortPairsDesc(validUnits.toArray(new Pair[0])));
        unitsWithTrait = Arrays.asList(sortPairsDesc(unitsWithTrait.toArray(new Pair[0])));
        int traitUnitsInTeam = 0;
        List<String> teamUnitNames = new ArrayList<>();

        for(int l=0;team.size()<level;l++){

            if(l-team.size() >= validUnits.size())
                break;

            String[] unitSplit;

            if(team.size() < teamEffect.getMinUnits() && l<unitsWithTrait.size())
                unitSplit = unitsWithTrait.get(l).getFirst().split("_");
            else
                unitSplit = validUnits.get(l-team.size()).getFirst().split("_");

            if(!teamUnitNames.contains(unitSplit[1])){

                if(team.size() < teamEffect.getMinUnits() && l<unitsWithTrait.size()){
                    teamUnitNames.add(unitSplit[1]);
                    team.add(unitsWithTrait.get(l));
                    traitUnitsInTeam++;
                    continue;
                }

                if(!team.contains(mainUnit)){
                    teamUnitNames.add(mainUnit.getFirst().split("_")[1]);
                    team.add(mainUnit);
                    continue;
                }

                if(!team.contains(validUnits.get(l-team.size()))){

                    if(unitsWithTrait.contains(validUnits.get(l-team.size()))){

                        if(traitUnitsInTeam < teamEffect.getMaxUnits()){

                            traitUnitsInTeam++;
                            team.add(validUnits.get(l-team.size()));
                        }

                    }
                    else
                        team.add(validUnits.get(l-team.size()));

                }

                teamUnitNames.add(unitSplit[1]);
            }

        }

        return team;
    }

    public List<Pair<String, Double>> findBestItemsForUnit(String unitName, int limit){

        int unitIndex = this.unitNames.indexOf(unitName);

        Pair<String, Double>[] items = new Pair[this.itemMatrix[unitIndex].length];

        for(int i=0;i<this.itemMatrix[unitIndex].length;i++){

            String itemName="";

            for(Item item : this.items){
                if(item.getId() == this.itemIndexes.get(i)) {
                    itemName = item.getName();
                    break;
                }
            }

            items[i] = Pair.of(itemName, (double) this.itemMatrix[unitIndex][i][0]);

        }

        items = sortPairsDesc(items);

        List<Pair<String,Double>> returnList = new ArrayList<>();

        for(int i=0; i<limit;i++){

            returnList.add(items[i]);
        }

        return returnList;

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

            Pair<String, Double>[] data = predictMatch(teams);

            String[] guessedTeams = new String[data.length];
            for(int i=0;i<data.length;i++){
                guessedTeams[i] = data[i].getFirst();
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

    private Pair<String,Double>[] sortPairsDesc(Pair<String,Double>[] pairs){

        if(pairs == null)
            return  null;

        Arrays.sort(pairs, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return o2.getSecond().compareTo(o1.getSecond());
            }
        });

        return pairs;
    }

    private Pair<String,Double>[] sortPairsAsc(Pair<String,Double>[] pairs){

        if(pairs == null)
            return null;

        Arrays.sort(pairs, new Comparator<Pair<String, Double>>() {
            @Override
            public int compare(Pair<String, Double> o1, Pair<String, Double> o2) {
                return o1.getSecond().compareTo(o2.getSecond());
            }
        });

        return pairs;
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

    private int[] getUnitIndexes(String[] units){
        int[] indexes = new int[units.length];

        for(int i=0;i< units.length;i++){

            if(unitNames.contains(units[i]))
                indexes[i] = unitNames.indexOf(units[i]);
            else
                indexes[i] = -1;
        }

        return indexes;
    }

    private Effect getTeamEffect(String traitName,int style){

        for(int i=0; i<traits.length;i++){

            if(traitName.equals(traits[i].getApiName())){

                for(int j=0;j<traits[i].getEffects().length;j++){

                    if(traits[i].getEffects()[j].getStyle() == style)
                        return traits[i].getEffects()[j];
                }
            }
        }

        throw new RuntimeException("No such team");

    }

    public void setTraits(Trait[] traits) {
        this.traits = traits;
    }

    public void setUnits(Unit[] units) {
        this.units = units;
    }

    public void setItems(Item[] items){

        List<Item> tempItemsList = new ArrayList<>();

        for(Item item : items){

            if(item.getId() <= 99 && item.getId() > 0 && item.getId()/10 <= item.getId()%10)
                tempItemsList.add(item);

        }

        System.out.println(tempItemsList);

        this.items = tempItemsList.toArray(new Item[0]);
    }

    public void setMatrixData(double[][][] matrix, List<String> teamNames,
                              double[][][] unitMatrix, int[][][] itemMatrix,
                              List<String> unitNames, List<Integer> itemIndexes,
                              int totalGames){

        this.matrix = matrix;
        this.teamNames = teamNames;
        this.unitMatrix = unitMatrix;
        this.itemMatrix = itemMatrix;
        this.unitNames = unitNames;
        this.itemIndexes = itemIndexes;
        this.totalGames= totalGames;

    }
}
