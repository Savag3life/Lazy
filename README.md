## Lazy Jar Cleaner
Lazy is a tool created to strip Java jars of their functional methods to allow developers to distribute functional developer API's without having to provide any context into the methods or its usage. 

### Usage
Execute: `java -jar Lazy.jar path/to/input.jar path/to/output.jar`

### Building

1. Clone the repo `git clone https://github.com/Savag3life/Lazy.git`
2. CD into the new file `cd Lazy`
3. Build `mvn clean install`
4. The result will be `./target/Lazy-with-dependencies.jar`