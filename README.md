## Lazy Jar Cleaner
Lazy is a tool created to strip Java jars of their functional methods to allow developers to distribute functional developer API's without having to provide the methods content, exposing sensitive content.

### Usage
From the command-line: `java -jar Lazy.jar <path/to/input.jar> <path/to/output.jar> [path/to/config.json]`
Components listed inside `<>` are considered required, components listed inside `[]` are optional.

Lazy has a decent amount of logging included by default, we recommend using ` > log.txt` on the end of the above command to dump the log of Lazy to a `log.txt` file in your working directory.

### [Configuration Options](https://bin.supremeventures.ca/hepejikaci.json)
| Config Name                      | Description                                                                                           | Format                      |
|----------------------------------|-------------------------------------------------------------------------------------------------------|-----------------------------|
| `EXEMPT`                         | Packages exempt from being stripped by Lazy (Essentially Skipping & adding to output jar)             | `package0/package1/*`       |
| `EXCLUDE`                        | Packages to be excluded from the output (These packages / classes are not included in the output jar) | `package0/package1/*`       |
| `INCLUDE_PUB_STATIC_FIELDS`      | Should Lazy include fields marked as `public static`                                                  | `true` or `false`           |
| `INCLUDE_PRIV_STATIC_FIELDS`     | Should Lazy include fields marked as `private static`                                                 | `true` or `false`           |
| `INCLUDE_PUB_NON_STATIC_FIELDS`  | Should Lazy include fields marked as non-static `public`                                              | `true` or `false`           |
| `INCLUDE_PRIV_NON_STATIC_FIELDS` | Should Lazy include fields marked as non-static `private`                                             | `true` or `false`           |
| `INCLUDE_PRIVATE_METHODS`        | Should Lazy include emptied methods which are marked as `private`                                     | `true` or `false`           |
| `INCLUDE_NATIVE_METHODS`         | Should Lazy include emptied methods which are marked as `native`                                      | `true` or `false`           |
| `VERBOSE`                        | Should Lazy do extra debugging logging when processing?                                               | `true` or `false`           |
| `RETENTION_ANNOTATIONS`          | Fields & methods annotated with these annotations are excluded.                                       | `pack0/pack1/APIAnnotation` |
| `DO_JETBRAINS_CONTRACTS`         | Should Lazy add JetBrains contracts?                                                                  | `true` or `false`           |

*Retention annotations override all other configuration options.*
*Default config is generated when Lazy is run without a config file specified in the command line.*
*Config name is `config.json`*

### Building Yourself

1. Clone the repo `git clone https://github.com/Savag3life/Lazy.git`
2. CD into the new file `cd Lazy`
3. Build `mvn clean install`
4. The result will be `./target/Lazy-with-dependencies.jar`

