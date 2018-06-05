import bwapi.*;
import bwapi.Error;
import bwta.BWTA;
import bwta.BaseLocation;

import javax.xml.transform.ErrorListener;
import java.util.logging.ErrorManager;

public class EconomyManager extends DefaultBWListener{

    Player self;
    Game game;
    Mirror mirror;
    int reservedMinerals;
    boolean pylonWait = false;

    public EconomyManager(Player player, Game g, Mirror m){
        self = player;
        game = g;
        mirror = m;
        mirror.getModule().setEventListener(this);
        reservedMinerals = 0;
    }

    public void onFrame() {
        if (game.getLastError().equals(Error.Unbuildable_Location)){
            pylonWait = false;
            game.setLastError(null);
        }
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            if(myUnit.getType() == UnitType.Protoss_Probe && !pylonWait && self.minerals() - reservedMinerals >= 100 && myUnit.canBuild() && 4 >= self.supplyTotal() - self.supplyUsed()){    //very basic, update later to be less restrictive later in game
                buildPylon(myUnit);
                //pylonWait = true;
                reservedMinerals += 100;

            }
            //temporary hardcoded strategy
            if (self.minerals() - reservedMinerals >= 150 && myUnit.canBuild()) {
                buildStructure(myUnit, UnitType.Protoss_Gateway);
                reservedMinerals += 150;
            }
            if (myUnit.getType() == UnitType.Protoss_Gateway && self.minerals() - reservedMinerals >= 100 && !myUnit.isTraining()){
                reservedMinerals += 100;
                myUnit.train(UnitType.Protoss_Zealot);
            }

            buildWorkers(myUnit);
            sendToMine(myUnit);
        }
        game.printf("reserved minerals: " + Integer.toString(reservedMinerals));
    }

    public void sendToMine(Unit myUnit){
        //if it's a worker and it's idle, send it to the closest mineral patch
        if (myUnit.getType().isWorker() && myUnit.isIdle()) {
            Unit closestMineral = null;

            //find the closest mineral
            for (Unit neutralUnit : game.neutral().getUnits()) {
                if (neutralUnit.getType().isMineralField()) {
                    if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                        closestMineral = neutralUnit;
                    }
                }
            }

            //if a mineral patch was found, send the worker to gather it
            if (closestMineral != null) {
                myUnit.gather(closestMineral, false);
            }
        }
    }

    public void buildWorkers(Unit myUnit){
        //if there's enough minerals, train a probe
        if (myUnit.getType() == UnitType.Protoss_Nexus && self.minerals() - reservedMinerals >= 50 && !myUnit.isTraining()) {
            myUnit.train(UnitType.Protoss_Probe);
            reservedMinerals += 100;
        }
    }

    public void buildPylon(Unit probe){
        probe.build(UnitType.Protoss_Pylon, game.getBuildLocation(UnitType.Protoss_Pylon, probe.getTilePosition()));
    }

    public void buildStructure(Unit probe, UnitType type) {
        probe.build(type, game.getBuildLocation(type ,probe.getTilePosition()));
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if (unit.getType() == UnitType.Protoss_Pylon){
            pylonWait = false;
        }
    }

    @Override
    public void onUnitCreate(Unit unit) {
        reservedMinerals -= unit.getType().mineralPrice();
        game.printf("unit created: " + unit.getType() + " " + Integer.toString(unit.getType().mineralPrice()));
    }
}
