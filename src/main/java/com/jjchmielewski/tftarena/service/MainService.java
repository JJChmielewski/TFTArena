package com.jjchmielewski.tftarena.service;

import com.jjchmielewski.tftarena.communitydragon.CDItem;
import com.jjchmielewski.tftarena.communitydragon.CDTrait;
import com.jjchmielewski.tftarena.communitydragon.CDUnit;
import com.jjchmielewski.tftarena.communitydragon.stats.CDEffect;
import com.jjchmielewski.tftarena.repository.GameRepository;
import com.jjchmielewski.tftarena.responses.ResponseItem;
import com.jjchmielewski.tftarena.responses.ResponseTeam;
import com.jjchmielewski.tftarena.responses.ResponseUnit;
import com.jjchmielewski.tftarena.riotapi.Team;
import com.jjchmielewski.tftarena.riotapi.entities.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MainService {


    private final GameRepository gameRepository;

    //team, enemy, 0 strength, 1 times played vs enemy, 2 total times played
    private double[][][] matrix;

    //team, unit, 0 sum place when in, 1 sum place when not, 2 times played, 3 times not played
    private double[][][] unitMatrix;

    //unit, item, 0 times played
    private int[][][] itemMatrix;

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

    public Pair<String, Double>[] predictMatch(Team[] teams){

        String[] teamNames = new String[teams.length];

        for(int t=0;t<teams.length;t++){
            teamNames[t] = teams[t].getTeamName();
        }

        Pair<String, Double>[] guessedTeams = predictMatch(teamNames);

        for(int i=0;i< guessedTeams.length;i++){

            for(int j=0;j<guessedTeams.length;j++){

                if(guessedTeams[i].getFirst().equals(teams[j].getTeamName())){
                    double unitWeight=0;
                    int teamIndex = this.teamNames.indexOf(guessedTeams[i].getFirst());

                    for(int u=0;u<teams[j].getUnits().length;u++){
                        String unitName = teams[j].getUnits()[u].getCharacter_id()+"_"+teams[j].getUnits()[u].getTier();

                        int unitIndex = this.unitNames.indexOf(unitName);

                        if(unitIndex < 0){
                            System.out.println(teams[j].getUnits()[u]);
                            continue;
                        }

                        unitWeight+= (unitMatrix[teamIndex][unitIndex][1]/unitMatrix[teamIndex][unitIndex][3]) -
                                (unitMatrix[teamIndex][unitIndex][0]/unitMatrix[teamIndex][unitIndex][2]);
                    }

                    guessedTeams[i] = Pair.of(guessedTeams[i].getFirst(), guessedTeams[i].getSecond()+unitWeight);
                }

            }

        }

        return this.sortPairsDesc(guessedTeams);

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

    public ResponseTeam buildTeam(String teamName, int level, double minPercentagePlayed){

        //1 trait, 2 style, 4 unit, 5 star
        String[] teamNameSplit = teamName.split("_");

        int teamIndex = teamNames.indexOf(teamName);
        CDEffect teamEffect = getTeamEffect(teamNameSplit[1], Integer.parseInt(teamNameSplit[2]));

        List<ResponseUnit> teamUnits = new ArrayList<>();

        List<ResponseUnit> validUnits = new ArrayList<>();
        List<ResponseUnit> unitsWithTrait = new ArrayList<>();
        ResponseUnit mainUnit = null;

        for(int u=0;u<unitMatrix[teamIndex].length;u++){

            if(unitMatrix[teamIndex][u][2]/(unitMatrix[teamIndex][u][2] + unitMatrix[teamIndex][u][3]) >= minPercentagePlayed){

                double[] unitData = unitMatrix[teamIndex][u];

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
                unitSplit = unitsWithTrait.get(l).getApiName().split("_");
            else
                unitSplit = validUnits.get(l-teamUnits.size()).getApiName().split("_");

            if(!teamUnitNames.contains(unitSplit[1])){

                if(teamUnits.size() < teamEffect.minUnits() && l < unitsWithTrait.size()){
                    teamUnitNames.add(unitSplit[1]);
                    teamUnits.add(unitsWithTrait.get(l));
                    traitUnitsInTeam++;
                    continue;
                }

                if(!teamUnits.contains(mainUnit) && mainUnit != null){
                    teamUnitNames.add(mainUnit.getApiName().split("_")[1]);
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

    public double checkAlgorithm(){

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
            //Pair<String, Double>[] data = predictMatch(teamComps);

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

        return method1[method1.length-1];
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

        for(int i=0; i< traits.length;i++){
            this.traits.put(traits[i].name(), traits[i]);
        }
    }

    public void setUnits(CDUnit[] units) {
        this.units = new HashMap<>();

        for(int i=0; i<units.length;i++){
            this.units.put(units[i].apiName(), units[i]);
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
                              double[][][] unitMatrix, int[][][] itemMatrix,
                              List<String> unitNames, List<Integer> itemIndexes,
                              int totalGames){

        this.matrix = matrix;
        this.teamNames = teamNames;
        this.unitMatrix = unitMatrix;
        this.itemMatrix = itemMatrix;
        this.unitNames = unitNames;
        this.itemIDs = itemIndexes;
        this.totalGames= totalGames;

    }
}
