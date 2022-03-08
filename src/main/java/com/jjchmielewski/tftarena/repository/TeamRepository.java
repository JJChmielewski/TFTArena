package com.jjchmielewski.tftarena.repository;

import com.jjchmielewski.tftarena.entitis.nodes.Item;
import com.jjchmielewski.tftarena.entitis.nodes.Team;
import com.jjchmielewski.tftarena.entitis.nodes.UnitNode;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.TeamUnitRelationship;
import com.jjchmielewski.tftarena.entitis.nodes.relationships.UnitItemRelationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class TeamRepository {

    @Autowired
    private TeamNEO4JRepository teamNEO4JRepository;

    @Autowired
    private UnitNEO4JRepository unitNEO4JRepository;

    @Autowired
    private ItemNeo4JRepository itemNeo4JRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private TransactionTemplate transactionTemplate;

    @Value("${tftarena.prefix.traitPrefix}")
    private String traitPrefix;

    @Value("${tftarena.prefix.unitPrefix}")
    private String unitPrefix;


    public void saveGraph(List<Team> teams, UnitNode[] units, Item[] items){

        transactionTemplate = new TransactionTemplate(platformTransactionManager);

        transactionTemplate.execute(status -> {

            for(Team t: teams){
                teamNEO4JRepository.saveNode(t.getName());
            }
            for(Item i : items){
                itemNeo4JRepository.saveItem(i.getItemId());
            }

            for(UnitNode unit: units){
                if(unit!=null) {
                    unitNEO4JRepository.saveUnit(unit.getName());

                    for(UnitItemRelationship r : unit.getItems()){
                        unitNEO4JRepository.saveUnitItemRelationship(unit.getName(),r.getItem().getItemId(),r.getTimesPlayed());
                    }
                }
            }

            return 1;
        });

        System.out.println("Saved nodes");

        //int increment = 10000000/(teams.get(0).getEnemyTeams().size() + teams.get(0).getUnits().size());
        int increment = 1;

        if(increment < 0)
            increment=1;

        for(int t=0;t<teams.size();t+= increment){

            int temp = t+increment;

            if(temp > teams.size())
                temp = teams.size();

            int loopBegin = t;
            int loopEnd = temp;
            transactionTemplate.execute(status -> {
                for(int i = loopBegin; i< loopEnd ;i++){

                    Team team = teams.get(i);

                    for(TeamRelationship r : team.getEnemyTeams()){
                        teamNEO4JRepository.saveTeamRelationships(team.getName(),r.getEnemyTeam().getName(),r.getStrength());
                    }
                    for(TeamUnitRelationship r : team.getUnits()){
                        teamNEO4JRepository.saveTeamUnitRelationship(team.getName(),r.getUnit().getName(),r.getWeight(),r.getPercentagePlayed());
                    }
                }


                return 1;
            });

            System.out.println(t);
        }
    }


    public void deleteAll(){
        teamNEO4JRepository.deleteAll();
        unitNEO4JRepository.deleteAll();
        itemNeo4JRepository.deleteAll();
    }

    public List<Team> getMatchInfo(String[] teams){

        /*
        for(int i=0;i<teams.length;i++){
            String[] temp = teams[i].split("_");
            if(temp.length == 4)
                teams[i] = this.traitPrefix+temp[0]+"_"+temp[1]+this.unitPrefix+temp[2]+"_"+temp[3];
        }*/

        return teamNEO4JRepository.findStrength(teams);
    }
}
