## Lazy Jar Cleaner
Lazy is a tool created to strip Java jars of their functional methods to allow developers to distribute functional developer API's without having to provide any context into the methods or its usage. This is to try and help power the community & some of it's developers into a better future that might not be able to make their own tools.

*Disclaimer - Due to the current limiations of ASM, the String utilities do not currently work. We are attempting to find a way around this.*

### Usage
Execute: `java -jar Lazy.jar path/to/input.jar path/to/output.jar`

Optionally you can also specify a path to an external config with a thrid CLI argument.

### Building Yourself

1. Clone the repo `git clone https://github.com/Savag3life/Lazy.git`
2. CD into the new file `cd Lazy`
3. Build `mvn clean install`
4. The result will be `./target/Lazy-with-dependencies.jar`

