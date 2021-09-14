package midautumn.robot;

public class TerrainSensor extends Sensor<Character> {

    @Override
    public Character getData() {
        return owner.getWorld().getTerrain(owner.getPosition());
    }
}
