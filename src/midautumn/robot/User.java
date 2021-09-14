package midautumn.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User {
    public static void main(String[] args) {

//        Robot panicPenguin = new Robot("Panic!", 0, 0.5);
//
//        // create memory
//        Memory<Character> terrain = panicPenguin.createMemory(new Memory<>("terrain", '0'));
//
//        // create and attach sensors
//        panicPenguin.attachSensor(new TerrainSensor().setProcessor(terrain::setData));
//
//        // program the robot
//        panicPenguin.setProgram(robot -> {
//            List<Command> commands = new ArrayList<>();
//            commands.add(r -> r.say(terrain.getData().toString()));
//            commands.add(r -> r.turnBy(Math.toRadians(5)));
//            commands.add(r -> r.go(0.1));
//            return commands;
//        });
//
//        World world = new World(
//                        "#######\n" +
//                        "#  0  #\n" +
//                        "# ÀÖ ÖÐ #\n" +
//                        "# ¿ì Çï #\n" +
//                        "#     #\n" +
//                        "#######");
//        panicPenguin.spawnInWorld(world, '0');
//        world.run();


//        // Task 2.3
        String mazeMap =

                        "############## #### \n" +
                        "#  0   #   ##    ## $\n" +
                        "#  #  ##   ## ## # ##\n" +
                        "####   # #     #   ##\n" +
                        "#      #    ## ##   #\n" +
                        "#  #      # ##      #\n" +
                        "#####################";

        World world = new World(mazeMap);
        Robot robot = makeMazeRunner();
        robot.spawnInWorld(world, '0');
        world.run();
    }

    public static int dir = 1; //0 up 1 right 2 down 3 left

    public static Robot makeMazeRunner() {

        Robot panicPenguin = new Robot("Maze!", 0, 0.5);

        // create memory
        Memory<Character> terrain = panicPenguin.createMemory(new Memory<>("terrain", '0'));
        Memory<Character> end = panicPenguin.createMemory(new Memory<>("end", '$'));
        // create and attach sensors
        panicPenguin.attachSensor(new TerrainSensor().setProcessor(terrain::setData));
        panicPenguin.attachSensor(new TerrainSensor().setProcessor(end::setData));

        // program the robot
        panicPenguin.setProgram(robot -> {
            Position position = robot.getPosition();
            List<Command> commands = new ArrayList<>();

            if (Objects.equals(end.getData().toString(), "$")) {
                return commands;
            }
            switch (dir) {
                case 0:
                    if ('#' != robot.getWorld().getTerrain(position.x + 1, position.y)) {
                        commands.add(r -> r.turnTo(0));
                        commands.add(r -> r.say(terrain.getData().toString()));
                        commands.add(r -> r.go(1));
                        dir = 1;
                        return commands;
                    } else {
                        if ('#' != robot.getWorld().getTerrain(position.x, position.y - 1)) {
                            commands.add(r -> r.turnTo(1.5 * Math.PI));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            return commands;
                        } else {
                            commands.add(r -> r.turnTo(Math.PI));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            dir = 3;
                            return commands;
                        }
                    }
                case 1:
                    if ('#' != robot.getWorld().getTerrain(position.x, position.y + 1)) {
                        commands.add(r -> r.turnTo(Math.PI * 0.5));
                        commands.add(r -> r.say(terrain.getData().toString()));
                        commands.add(r -> r.go(1));
                        dir = 2;
                        return commands;
                    } else {
                        if ('#' != robot.getWorld().getTerrain(position.x + 1, position.y)) {
                            commands.add(r -> r.turnTo(0));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            return commands;
                        } else {
                            commands.add(r -> r.turnTo(1.5 * Math.PI));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            dir = 0;
                            return commands;
                        }
                    }
                case 2:
                    if ('#' != robot.getWorld().getTerrain(position.x - 1, position.y)) {
                        commands.add(r -> r.turnTo(Math.PI));
                        commands.add(r -> r.say(terrain.getData().toString()));
                        commands.add(r -> r.go(1));
                        dir = 3;
                        return commands;
                    } else {
                        if ('#' != robot.getWorld().getTerrain(position.x, position.y + 1)) {
                            commands.add(r -> r.turnTo(Math.PI * 0.5));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            return commands;
                        } else {
                            commands.add(r -> r.turnTo(0));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            dir = 1;
                            return commands;
                        }
                    }
                case 3:
                    if ('#' != robot.getWorld().getTerrain(position.x, position.y - 1)) {
                        commands.add(r -> r.turnTo(1.5 * Math.PI));
                        commands.add(r -> r.say(terrain.getData().toString()));
                        commands.add(r -> r.go(1));
                        dir = 0;
                        return commands;
                    } else {
                        if ('#' != robot.getWorld().getTerrain(position.x - 1, position.y)) {
                            commands.add(r -> r.turnTo(Math.PI));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            return commands;
                        } else {
                            commands.add(r -> r.turnTo(Math.PI * 0.5));
                            commands.add(r -> r.say(terrain.getData().toString()));
                            commands.add(r -> r.go(1));
                            dir = 2;
                            return commands;
                        }
                    }
            }
            return commands;
        });
        return panicPenguin;
    }
}
