# check-dependencies-plugin


## Подключение
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'io.spring.gradle:dependency-management-plugin:0.6.1.RELEASE'
        classpath 'ru.yoomoney.gradle.plugins:check-dependencies-plugin:4.+'
    }
}
apply plugin: 'ru.yoomoney.gradle.plugins.check-dependencies-plugin'

```

## Функционал

Плагин включает в себя несколько функциональностей:

### Проверка легитимность изменения версий используемых библиотек в проекте.

Проверяются как прямые, так и транзитивные зависимости.

Зачастую проект может содержать большое количество повторно используемых библоиотек разных версий, найденных по транзитивным
зависимостям. Однако, при запуске приложения может быть использована только одна версия одной и той же библиотеки.
Чтобы гарантировать согласованность этой библиотеки с другими, Gradle имеет встроенный механизм решения конфликтов версий.
По умолчанию Gradle из всех версий одной и той же библиотеки выбирает самую последнюю. При таком подходе нет гарантии, что самая
новая версия библиотеки будет обратно совместима с предыдущей версией. А значит нельзя гарантировать, что такое повышение
не сломает проект.

Для того, чтобы избежать не контролируемое изменение версий, используется подход с фиксацией набор версий бибилиотек, на которых
гарантируется работа приложения.

Для фиксации используется сторонний плагин <b>IO Spring Dependency Management plugin</b>. Список фиксируемых библиотек с
версиями хранится в maven xml.pom файле. Плагин предоставляет программный доступ к этому списку.

Обратной стороной фиксации служит неконтролируемое понижение версии библиотек. Чтобы сделать этот процесс изменения версий
библиотек контролируемым сделан этот плагин.

После того, как все зависимости и версии библиотек определены, плагин выполняет проверку. Правила проверки следующие:

<ol>
<li>Если изменение версии библиотеки связано с фиксацией версии, плагин остановит билд с ошибкой.</li>
<li>Если изменение версии библиотеки не связано с фиксацией версии, то билд допускается к выполнению.</li>
</ol>

В большинстве случаев не возможно подобрать такой набор версий библиотек, который бы удовлетворил всем подключаемым библиотекам 
прямо и по транзититивным зависимостям. Поэтому плагин поддерживает введение исключение из правил. Удостоверившись, 
что более новая версия библиотеки полностью обратно совместима со старой версии, можно разрешить обновление с одной версии 
библиоетки до другой.


### Проверка конфликтов мажорных версий подключаемых библиотек

Прямые и транзитивные зависимости библиотек проверяются на наличие конфликтов мажорных версий.
При наличии конфликтов сборка неуспешна, кроме случаев, если запускалась таска ":dependencies" - в этом случае выводится запись
о наличии конфликта в лог. 
Для определения, какие зависимости нужно проверять, существует настройка includeGroupIdPrefixes.
Например, в ней можно указать, что проверять нужно только внутренние библиотеки компании, указав префикс "ru.yoomoney".

### Вывод новых доступных версий для библиотек  

Печатает доступные новые версии зависимостей.  
Есть два режима:
1) Вывод новых версий для всех имеющихся в проекте зависимостей.  
   Для запуска этого режима необходимо вызвать вручную таску printNewDependencies.  
2) Вывод новых версий для зависимостей из списка. Список определяется с помощью настройки:
```
checkDependencies {
    inclusionPrefixesForPrintDependencies = ['ru.yoomoney']
}
```

   В настройку передаются префиксы groupId артефактов.  
   Функицональность может быть полезна для вывода новых зависимостей, относящихся к внутренним для компании, 
   тогда в настройку нужно передать префикс компании, как в примере выше.  
   Для запуска этого режима необходимо вызвать вручную таску printNewDependenciesByInclusion.

### Вывод актуальных версий для библиотек

Печатает актуальные версии зависимостей.  
Для этой функциональности также есть два режима:
1) Вывод версий для всех имеющихся в проекте зависимостей.  
   Для запуска этого режима необходимо вызвать вручную таску printActualDependencies.  
   
2) Вывод версий для зависимостей из списка. Список определяется с помощью настройки:  
```
checkDependencies {
    inclusionPrefixesForPrintDependencies = ['org.apache']
}
``` 
   В настройку передаются префиксы groupId артефактов.  
   
   Для запуска этого режима необходимо вызвать вручную таску printNewDependenciesByInclusion.   

Пример вывода:

```
   [
       {
           "scope": "compile",
           "name": "json-utils",
           "version": "1.0.0",
           "group": "ru.yoomoney.common"
       },
       {
           "scope": "compile",
           "name": "xml-utils",
           "version": "1.0.0",
           "group": "ru.yoomoney.common"
       }
   ]
```

Результат сохраняется в build/report/dependencies/ в actual_dependencies_by_inclusion.json & actual_all_dependencies.json

### Проверка наличия snapshot-версий подключаемых библиотек

   Проверяет наличие snapshot-версий подключаемых зависимостей. Вызывается только при ручном запуске таски 
checkSnapshotsDependencies. Выбрасывает исключение при наличии зависимостей с версией, содержащей "-snapshot".
    Для того, чтобы разрешить наличие snapshot-зависимостей необходимо указать в build.gradle такое свойство:
```
    ext.allowSnapshot = "true"
```

   В этом случае проверка snapshot-зависимостей производиться не будет.

### Проверка наличия запрещенных артефактов в подключаемых библиотеках
   
   В качестве настройки принимается список запрещенных к использованию артефактов. Затем просматривает текущие
зависимости, в случае нахождения среди них запрещенных артефактов запрещает сборку.
   Также предлагает заменить найденные запрещенные версии на новейшие, доступные для данной зависимости, если она 
не совпадает с запрещенной.

#### Настройки проверки наличия запрещенных артефактов в подключаемых библиотеках

   Список запрещенных артефактов может задаваться такими способами:
```groovy
     forbiddenDependenciesChecker {
            after {             //запрещены все версии joda-time:joda-time выше 4.0.0 (включая все более поздние мажорные)
                 forbidden 'joda-time:joda-time:4.0.0'
                 recommended '4.0.7'
                 comment 'bla bla'
            }
            before {           //запрещены все версии org.apache.tomcat.embed:tomcat-embed-core ниже 4.0.0
                 forbidden 'org.apache.tomcat.embed:tomcat-embed-core:4.2.0'
                 recommended '4.2.7'
                 comment 'bla bla'
            }
            eq {               //запрещена org.apache.commons:commons-lang3 версии 2.1.4
                 forbidden 'org.apache.commons:commons-lang3:2.1.4'
                 recommended '2.1.7'
                 comment 'bla bla'
            }
            range {            //запрещены версии com.google.guava:guava от 4.0.0 до 4.0.2 включительно
                 forbidden 'com.google.guava:guava'
                 startVersion '4.0.0'
                 endVersion '4.0.2'
                 recommended '4.0.7'
                 comment 'bla bla'
            }
     }
```
   Можно указать несколько блоков для одной и той же библиотеки.
   По умолчанию список пуст.

#### Настройка разрешающих правил изменения версий библиотек

Правила исключения описываются в property файле. По умолчанию используется файл с названием <b>libraries-versions-exclusions.properties</b>
расположенный в корне проекта. Однако, плагин позволяет переопределить название и место расположение такого файла. Для этого
используется расширение плагина:

```groovy
checkDependencies {
   exclusionsRulesSources = ["Путь к файлу", "Maven артефакт"]
}
```

Помимо чтения правил из файла, плагин поддерживает способ чтения правил из файла мавен артефакта. При этом можно указывать 
полное название артефакта (группа, id артефакта и версия), так и опускать версию:

```groovy
checkDependencies {
   exclusionsRulesSources = ["ru.yoomoney:platform-dependencies:",
                             "ru.yoomoney:libraries-dependencies:1.0.2"]
}
```

Так же плагин разрешает использовать несколько источников файлов с правилами:

```groovy
checkDependencies {
   exclusionsRulesSources = ["my_libraries_versions_exclusions.properties", 
                             "settings/additional_libraries_versions_exclusions.properties", 
                             "ru.yoomoney:platform-dependencies:"]
}
```

Для описания самих правил может использоваться одна из следующих форм записи:

```properties
org.slf4j.jul-to-slf4j = 1.7.15 -> 1.7.16
org.slf4j.jcl-over-slf4j = 1.7.7, 1.7.15 -> 1.7.16
org.slf4j.slf4j-api = 1.6.3, 1.6.4, 1.7.0, 1.7.6, 1.7.7, 1.7.10, 1.7.12, 1.7.13 -> 1.7.16
```

#### Включение конфигураций в проверку

Для того, чтобы конфигурации начали проверяться на наличие конфликтов, необходимо внести их в настройку 
<b>includedConfigurations</b>:

```groovy
checkDependencies {
    includedConfigurations = ["testImplementation", "testRuntime"]
}
```

По умолчанию проверка осуществляется в конфигурациях componentTestCompileClasspath и slowTestCompileClasspath, 
которые включают в себя зависимости из всех нужных для проверок конфигураций - compile, implementation, testCompile, 
testImplementation, runtime.

#### Настройки проверки конфликтов мажорных версий 

Проверку конфликтов можно отключить, выставив данную настройку в false:
```groovy
majorVersionChecker {
   enable = true // true является значением по умолчанию
}
```

Библиотеки, для которых будут проверяться конфликты мажорных версий можно описать с помощью префиксов, с которых начинаются 
названия групп. 
Например, для того, чтобы проверять конфликты только для библиотек, название группы которых начинается с
"com.google" или "org.apache", нужно задать настройку includeMajorVersionCheckPrefixLibraries следующим образом:

```groovy
majorVersionChecker {
   includeGroupIdPrefixes = ['com.google', 'org.apache']    // По умолчанию список пуст
}
```

Также можно исключить из проверки конкретные артефакты:
```groovy
majorVersionChecker {
   excludeDependencies = ["ru.yoomoney.common:xml-utils", 
                                        "ru.yoomoney.common:json-utils"]  // По умолчанию список пуст
}
```

Есть возможность установить, нужно ли фейлить билд при нахождении конфликта:
```
majorVersionChecker {
   failBuild = true  // По умолчанию билд фейлится
}
```

#### Поиск возможного решения конфликта версий среди зависимостей

Плагин предоставляет возможность определения подходящей версии библиотеки при наличии конфликта версий среди зависимостей,
указанных в секциях dependencyManagement секции и dependencies.
Суть реализованного эвристического метода описана ниже на примере.

Предположим, в проекте требуется использовать библиотеку alpha:3.0.
При этом также в проекте используется библиотека omega:2.0, из-за которой происходит конфликт версий: (omega:2.0) --> (beta:2.0) --> (alpha:2.0 -> 3.0).
При этом существуют другие версии библиотеки omega, зависимости которых выглядят следующим образом:
 - (omega:1.0 --> beta:1.0 --> alpha:1.0)
 - (omega:3.0 --> beta:3.0 --> alpha:3.0)
 - (omega:4.0 --> theta:4.0 --> alpha:4.0)
 - (omega:5.0 --> theta:5.0 --> zeta:5.0)

В самом лучшем варианте хотелось бы найти те версии библиотеки omega,
которые ТОЧНО НЕ ИМЕЮТ в качестве прямой/транзитивной зависимости библиотеку alpha с версией, отличной от 3.0,
и предложить пользователю/разработчику попробовать использовать именно их.

Более простая задача, решаемая реализованым эверистическим методом: исключить те версии библиотеки omega,
которые ТОЧНО ИМЕЮТ в качестве прямой/транзитивной зависимости библиотеку alpha с версией, отличной от 3.0,
тем самым исключив их из рассмотрения в качестве возможной замены библиотеки omega текущей версии 2.0.
Также хочется сделать это максимально быстро, даже если библиотека omega имеет много различных версий.

Реализованный алгоритм выглядит следующим образом:
для очередной версии omega:X.0 (где X.0 != 2.0), используя последовательность имен библиотек из конфликтного пути (omega --> beta --> alpha),
пробуем проследовать по аналогичному пути с целью встретить в результате библиотеку alpha версией, отличной от 2.0.
Возможны следующие варианты:
 (1) для версии omega:X.0 существует аналогичный путь зависимостей (с точностью до имен библиотек), как и в случае конфликта,
     который в результате "приводит" к библиотеке alpha с версией, отличной от 3.0. В этом случае МОЖНО УТВЕРЖДАТЬ,
     что данная версия библиотеки НЕ ПОДХОДИТ
 (2) для версии omega:X.0 существует аналогичный путь зависимостей (с точностью до имен библиотек), как и в случае конфликта,
     который в результате "приводит" к библиотеке alpha с версией, равной 3.0. В этом случае МОЖНО ПРЕДПОЛОЖИТЬ, что данная
     версия библиотеки ВОЗМОЖНО ПОДХОДИТ
 (3) для версии omega:X.0 не существует аналогичного путь зависимостей (с точностью до имен библиотек), как и в случае конфликта.
     В этом случае НЕЛЬЗЯ УТВЕРЖДАТЬ, что данная версия библиотеки ТОЧНО НЕ ПОДХОДИТ
В итоге, в качестве замены текушей версии библиотеки omega алгоритм предлагает версии, найденные в вариантах (2) и (3).

Для включения поиска подходящей версии библиотеки (при наличии конфликта версий) необходимо добавить в extension-е ```versionSelectors ```
добавить запись, ключ которой - имя библиотеки, а значение - булева функция, определяющая набор анализируемых версий указанной библиотеки.
Например, в случае, если для библиотеки ```junit:junit``` обнаружен конфликт версий, и требуется определить подходящую
версию библиотеки с маской 11.+, то в ```build.gradle``` необходимо добавить:
```groovy
checkDependencies.versionSelectors = [
    'junit:junit': { version ->
        def major = version.tokenize(".")[0].toInteger()
        major > 11
    }
]
```