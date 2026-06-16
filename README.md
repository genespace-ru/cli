# cli
genespace command line interface
gs cli является надстройкой над nextflow cli
таким образом, пользователь, имеющий опыт с nextflow остается в привычном окружении и может использовать те же самые команды

**Сборка:**
mvn clean package

Есть зависимости на SNAPHOT версию диаграммы, необходимо, чтобы nexus был доступен


**Запуск команды:**
java -jar target/genespace.jar -log my.log ....

*Справка*
java -jar target/genespace.jar -log my.log help

*Конвертация*
java -jar target/genespace.jar -log my.log convert test.wdl -format nextflow

*Запуск wdl (сначала будет конвертирован в nf)*
java -jar target/genespace.jar -log my.log rungs test.wdl
или сокращенно
java -jar target/genespace.jar -log my.log test.wdl

**Запуск команды с возможностью подключиться дебаггером на порт 8077**
java -agentlib:jdwp=transport=dt_socket,address=*:8077,server=y,suspend=n -jar target/genespace.jar -log rungs.log rungs  test.wdl