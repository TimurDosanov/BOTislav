import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

import static bwapi.UnitType.Protoss_Pylon;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;

    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit discovered " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");
        
        int i = 0;
        for(BaseLocation baseLocation : BWTA.getBaseLocations()){
        	System.out.println("Base location #" + (++i) + ". Printing location's region polygon:");
        	for(Position position : baseLocation.getRegion().getPolygon().getPoints()){
        		System.out.print(position + ", ");
        	}
        	System.out.println();
        }

    }

    @Override
    public void onFrame() {

        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());

        StringBuilder units = new StringBuilder("My units:\n");

        //iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            if (myUnit.getType() == UnitType.Protoss_Nexus && self.minerals() >= 50 && self.allUnitCount()<=9 ) {
                myUnit.train(UnitType.Protoss_Probe);
            }
            if(self.minerals() >=150)
            {
                if (myUnit.getType() == UnitType.Protoss_Probe && self.allUnitCount() >11) {

                    TilePosition buildTile= getBuildTile(myUnit, UnitType.Protoss_Gateway, self.getStartLocation());
                    //and, if found, send the worker to build it (and leave others alone - break;)
                    if (buildTile != null) {
                        myUnit.build(UnitType.Protoss_Gateway, buildTile);
                        break;
                    }
                }
            }
                if ((self.supplyTotal() - self.supplyUsed() < 2) && (self.minerals() >= 100)) {
                    //iterate over units to find a worker

                        if (myUnit.getType() == UnitType.Protoss_Probe) {

                            TilePosition buildTile= getBuildTile(myUnit, UnitType.Protoss_Pylon, self.getStartLocation());

                            if (buildTile != null) {
                                myUnit.build(UnitType.Protoss_Pylon, buildTile);
                                break;
                            }
                        }
                    }








            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
                Unit closestMineral = null;
                for (Unit neutralUnit : game.neutral().getUnits()) {
                    if (neutralUnit.getType().isMineralField()) {
                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
                            closestMineral = neutralUnit;
                        }
                    }
                }
                if (closestMineral != null) {
                    myUnit.gather(closestMineral, false);
                }
            }
        }
        game.drawTextScreen(10, 25, units.toString());
    }
    public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile) {
        TilePosition ret = null;
        int maxDist = 3;
        int stopDist = 40;


        if (buildingType.isRefinery()) {
            for (Unit n : game.neutral().getUnits()) {
                if ((n.getType() == UnitType.Resource_Vespene_Geyser) &&
                        ( Math.abs(n.getTilePosition().getX() - aroundTile.getX()) < stopDist ) &&
                        ( Math.abs(n.getTilePosition().getY() - aroundTile.getY()) < stopDist )
                        ) return n.getTilePosition();
            }
        }

        while ((maxDist < stopDist) && (ret == null)) {
            for (int i=aroundTile.getX()-maxDist; i<=aroundTile.getX()+maxDist; i++) {
                for (int j=aroundTile.getY()-maxDist; j<=aroundTile.getY()+maxDist; j++) {
                    if (game.canBuildHere(new TilePosition(i,j), buildingType, builder, false)) {
                        // units that are blocking the tile
                        boolean unitsInWay = false;
                        for (Unit u : game.getAllUnits()) {
                            if (u.getID() == builder.getID()) continue;
                            if ((Math.abs(u.getTilePosition().getX()-i) < 4) && (Math.abs(u.getTilePosition().getY()-j) < 4)) unitsInWay = true;
                        }
                        if (!unitsInWay) {
                            return new TilePosition(i, j);
                        }

                        if (buildingType.requiresCreep()) {
                            boolean creepMissing = false;
                            for (int k=i; k<=i+buildingType.tileWidth(); k++) {
                                for (int l=j; l<=j+buildingType.tileHeight(); l++) {
                                    if (!game.hasCreep(k, l)) creepMissing = true;
                                    break;
                                }
                            }
                            if (creepMissing) continue;
                        }
                    }
                }
            }
            maxDist += 2;
        }

        if (ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
        return ret;
    }

    public static void main(String[] args) {
        new TestBot1().run();
    }
}