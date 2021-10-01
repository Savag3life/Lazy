package life.savag3.cleaner;

public class Bootstrap {

    // java -jar Cleaner-jar-with-dependencies.jar path/to/jar.jar path/to/new-jar.jar
    public static void main(String[] args) {
        new Lazy(args);
    }

}
