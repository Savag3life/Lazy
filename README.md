## Lazy Jar Cleaner
Lazy is a tool created to strip Java jars of their functional methods to allow developers to distribute functional developer API's without having to provide the methods content, exposing sensitive content.

### Usage
From the command-line: `java -jar Lazy.jar <path/to/input.jar> <path/to/output.jar> [path/to/config.json]`
Components listed inside `<>` are considered required, components listed inside `[]` are optional.

The log for lazy can be hard to navigate in the command-line & will log quickly. We recommend using ` > log.txt` on the end of the above command to dump the log of Lazy to a `log.txt` file in your working directory.

### Configuration 
| Config Name                      | Description                               | Format          |
|----------------------------------|-------------------------------------------|-----------------|
| `EXEMPT`                         | Exempt packages & classes being stripped. | `package0/package1/*` |
| `EXCLUDE`                        | Packages excluded from output file.       | `package0/package1/*` |
| `INCLUDE_PUB_STATIC_FIELDS`      | Include public static fields.             | `true` or `false`|
| `INCLUDE_PRIV_STATIC_FIELDS`     | Include private static fields.            | `true` or `false`|
 | `INCLUDE_PUB_NON_STATIC_FIELDS`  | Include public non-static fields.         | `true` or `false`|
 | `INCLUDE_PRIV_NON_STATIC_FIELDS` | Include private non-static fields.        | `true` or `false`|
 | `INCLUDE_PRIVATE_METHODS`        | Include private methods.                  | `true` or `false`|
 | `INCLUDE_NATIVE_METHODS`         | Include native methods.                   | `true` or `false`|
| `INCLUDE_ABSTRACT_CLASSES`       | Include abstract classes.                 |   `true` or `false`|
| `INCLUDE_ENUM_DATA`              | Include enum & enum fields                |   `true` or `false`|
| `VERBOSE`                        | Should Lazy do a shit ton of logging?     |   `true` or `false`|

*Default config is generated when Lazy is run without a config file specified in the command line.*
*Config name is `config.json`*
### Building Yourself

1. Clone the repo `git clone https://github.com/Savag3life/Lazy.git`
2. CD into the new file `cd Lazy`
3. Build `mvn clean install`
4. The result will be `./target/Lazy-with-dependencies.jar`

