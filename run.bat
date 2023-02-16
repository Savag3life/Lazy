@echo off

set "test-jar=%cd%\GucciCommons.jar"
set "cleaned-test-jar=%cd%\Gucci.jar"
set "console-log=%cd%\console.log"

set "source=%cd%\target\Lazy-jar-with-dependencies.jar"
set "destination=%cd%\Lazy.jar"

DEL %destination%
DEL %cleaned-test-jar%
DEL %console-log%

copy %source% %destination%

java -jar %destination% %test-jar% %cleaned-test-jar% > %console-log%
pause