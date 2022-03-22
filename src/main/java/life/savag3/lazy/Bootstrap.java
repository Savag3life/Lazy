package life.savag3.lazy;

public class Bootstrap {

    // java -jar Lazy.jar <Path/To/Input.jar> <Path/To/Output.jar> [Path/To/Config.json]
    public static void main(String[] args) {
        new Lazy(args);
    }

}
