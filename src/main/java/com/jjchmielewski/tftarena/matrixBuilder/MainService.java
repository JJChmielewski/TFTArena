package com.jjchmielewski.tftarena.matrixBuilder;

import com.jjchmielewski.tftarena.entitis.documents.Team;
import com.jjchmielewski.tftarena.entitis.documents.dummyClasses.Game;
import com.jjchmielewski.tftarena.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

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

    //unit, item, 0 times played, 1 times not played
    protected static int[][][] itemMatrix;

    //indexes of matrices
    protected static List<String> teamNames;

    protected static List<String> unitNames;

    protected static List<Integer> itemIndexes;

    protected static int totalGames;


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

                if(guessedTeams[i].equals(teams[j].getTeamName())){
                    double unitWeight=0;
                    int teamIndex = this.teamNames.indexOf(guessedTeams[i].getFirst());

                    for(int u=0;u<teams[j].getUnits().length;u++){
                        int unitIndex = this.unitNames.indexOf(teams[j].getUnits()[u]);

                        unitWeight+= (unitMatrix[teamIndex][unitIndex][1]/unitMatrix[teamIndex][unitIndex][3]) -
                                (unitMatrix[teamIndex][unitIndex][0]/unitMatrix[teamIndex][unitIndex][2]);
                    }

                    guessedTeams[i] = Pair.of(guessedTeams[i].getFirst(), guessedTeams[i].getSecond()*(unitWeight+7));
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

    public Pair<String,Double>[] findBestTeams(String[] enemyTeams, String[] teamUnits, double minPercentagePlayed){

        Pair<String, Double>[] teams = findBestTeams(enemyTeams, minPercentagePlayed);

        Pair<String, Double>[] unitTeams = findBestTeamsWithUnits(teamUnits);

        Pair<String, Double>[] result = new Pair[teamNames.size()];

        double[] indexSums = new double[teamNames.size()];

        for(int i=0;i< teamNames.size();i++){

            indexSums[teamNames.indexOf(teams[i].getFirst())] += i;

            indexSums[teamNames.indexOf(unitTeams[i].getFirst())] +=i;

        }

        for(int i=0;i< teamNames.size();i++){

            result[i] = Pair.of(teamNames.get(i), indexSums[i]);
        }

        return this.sortPairsAsc(result);

    }

    public Pair<String,Double>[] findBestTeams(String[] enemyTeams, String[] teamUnits,double minPercentagePlayed, int limit){

        if(limit < 0)
            return null;

        Pair<String, Double>[] teams = findBestTeams(enemyTeams,teamUnits, minPercentagePlayed);
        Pair<String, Double>[] result = new Pair[limit];

        for(int i=0; i<limit; i++){
            result[i] = teams[i];
        }

        return result;

    }

    public Pair<String,Double>[] findBestTeamsWithUnits(String[] units){

        Pair<String,Double>[] teams = new Pair[unitMatrix.length];

        int[] unitIndexes = getUnitIndexes(units);

        for(int t=0; t<unitMatrix.length;t++){

            double value=0;

            for(int unit : unitIndexes){

                double[] unitData = unitMatrix[t][unit];

                if(unit < 0)
                    continue;

                value+=(unitData[1]/unitData[3]) - (unitData[0]/unitData[2]);
            }

            Pair<String,Double> team = Pair.of(teamNames.get(t),value);

            teams[t] = team;
        }

        teams = sortPairsDesc(teams);

        return teams;

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

            //Pair<String, Double>[] data = predictMatch(teams);
            Pair<String, Double>[] data = predictMatch(teamComps);

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

}
