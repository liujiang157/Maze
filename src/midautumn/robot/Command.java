package midautumn.robot;

public interface Command {
    /**
     * @param robot
     * @return if true, it means that the robot can execute the next command in the todos queue immediately
     * otherwise it will take a little break
     */
    boolean execute(Robot robot);
}
