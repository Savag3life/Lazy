package life.savag3.lazy;

/**
 * Bootstrap class to start the program. Also used to escape the static context.
 *
 * @author Jacob C (Savag3life)
 * @since 2023-09-04
 */
public class Bootstrap {

    // java -jar Lazy.jar <Path/To/Input.jar> <Path/To/Output.jar> [Path/To/Config.json]
    // Map: `<>` fields are required, `[]` fields are optional
    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Invalid argument counts. Found " + args.length + ", Required 2");
            System.out.println("Usage: java -jar <Path/To/Input.jar> <Path/To/Output.jar> [Path/To/Config.json]");
            System.out.println("Map: `<>` fields are required, `[]` fields are optional");
            System.exit(1);
        }

        new Lazy(args[0], args[1], args.length == 3 ? args[2] : null);
    }

}
