import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class EconomyManager extends DefaultBWListener{

    Player self;
    Game game;
    Mirror mirror;
    int reservedMinerals = 0;
    boolean pylonWait = false;

    public EconomyManager(Player player, Game g, Mirror m){
        self = player;
        game = g;
        mirror = m;
        mirror.getModule().setEventListener(this);
    }

    public void onFrame() {
        StringBuilder units = new StringBuilder("My units:\n");
        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            if(!pylonWait && self.minerals() - reservedMinerals >= 100 && myUnit.canBuild() && 4 >= self.supplyTotal() - self.supplyUsed()){    //very basic, update later to be less restrictive later in game
                buildPylon(myUnit);
                pylonWait = true;
                reservedMinerals += 100;
            }
            buildWorkers(myUnit);
            sendToMine(myUnit);
        }
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
        }
    }

    public void buildPylon(Unit probe){
        probe.build(UnitType.Protoss_Pylon, game.getBuildLocation(UnitType.Protoss_Pylon, probe.getTilePosition()));
    }

    @Override
    public void onUnitComplete(Unit unit) {
        if (unit.getType() == UnitType.Protoss_Pylon){
            pylonWait = false;
        }
    }

    @Override
    public void onUnitCreate(Unit unit){
        if (unit.getType() == UnitType.Protoss_Pylon){
            reservedMinerals -= 100;
        }
    }
}
