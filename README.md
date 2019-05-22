# check-dependencies-plugin


## Подключение
```groovy
buildscript {
    repositories {
        maven { url 'http://nexus.yamoney.ru/repository/thirdparty/' }
        maven { url 'http://nexus.yamoney.ru/repository/central/' }
        maven { url 'http://nexus.yamoney.ru/repository/releases/' }
        maven { url 'http://nexus.yamoney.ru/repository/jcenter.bintray.com/' }
        maven { url 'https://nexus.yamoney.ru/repository/gradle-plugins/' }
    }
    dependencies {
        classpath 'io.spring.gradle:dependency-management-plugin:0.6.1.RELEASE'
        classpath 'ru.yandex.money.gradle.plugins:yamoney-check-dependencies-plugin:4.+'
    }
}
apply plugin: 'yamoney-check-dependencies-plugin'

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
Проверяются только версии внутренних библиотек (пакеты ru.yamoney и ru.yandex.money).

### Вывод новых доступных версий для внешних и внутренних библиотек

Печатает доступные новые версии внутренних и внешних зависимостей. Вызывается только при ручном запуске тасок 
printNewOuterDependenciesVersions и printNewInnerDependenciesVersions.

### Вывод актуальных версий для внутренних библиотек

Печатает актуальные версии зависимостей. Вызывается только при ручном запуске 
printActualInnerDependenciesVersions, printActualOuterDependenciesVersions.

```
   [
       {
           "scope": "compile",
           "name": "yamoney-json-utils",
           "version": "1.0.0",
           "group": "ru.yandex.money.common"
       },
       {
           "scope": "compile",
           "name": "yamoney-xml-utils",
           "version": "1.0.0",
           "group": "ru.yandex.money.common"
       },
       {
           "scope": "compile",
           "name": "yamoney-backend-platform-config",
           "version": "19.0.0",
           "group": "ru.yandex.money.common"
       }
   ]
```

Результат сохраняется в build/report/dependencies/ в actual_inner_dependencies.json & actual_outer_dependencies.json

### Проверка наличия snapshot-версий подключаемых библиотек

   Проверяет наличие snapshot-версий подключаемых зависимостей. Вызывается только при ручном запуске таски 
checkSnapshotsDependencies. Выбрасывает исключение при наличии зависимостей с версией, содержащей "-snapshot".
    Для того, чтобы разрешить наличие snapshot-зависимостей необходимо указать в build.gradle такое свойство:
```
    ext.allowSnapshot = "true"
```

   В этом случае проверка snapshot-зависимостей производиться не будет.

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
   exclusionsRulesSources = ["ru.yandex.money.platform:platform-dependencies:",
                             "ru.yandex.money.platform:libraries-dependencies:1.0.2"]
}
```

Так же плагин разрешает использовать несколько источников файлов с правилами:

```groovy
checkDependencies {
   exclusionsRulesSources = ["my_libraries_versions_exclusions.properties", 
                             "settings/additional_libraries_versions_exclusions.properties", 
                             "ru.yandex.money.platform:platform-dependencies:"]
}
```

Для описания самих правил может использоваться одна из следующих форм записи:

```properties
org.slf4j.jul-to-slf4j = 1.7.15 -> 1.7.16
org.slf4j.jcl-over-slf4j = 1.7.7, 1.7.15 -> 1.7.16
org.slf4j.slf4j-api = 1.6.3, 1.6.4, 1.7.0, 1.7.6, 1.7.7, 1.7.10, 1.7.12, 1.7.13 -> 1.7.16
```

#### Отключение конфигураций из проверки

Помимо этой настройки плагин позволяет исключить из проверки конфигурации. Для этого используется список исключения 
<b>excludedConfigurations</b>:

```groovy
checkDependencies {
    excludedConfigurations = ["testCompile", "testRuntime"]
}
```

Все перечисленные конфигурации будут исключены из проверки.

Подробнее о настройки IO Spring Dependency Management plugin описано на [официальной странице проекта](https://github.com/spring-gradle-plugins/dependency-management-plugin)

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
"ru.yandex.money" или "ru.yamoney", нужно задать настройку includeMajorVersionCheckPrefixLibraries следующим образом:

```groovy
majorVersionChecker {
   includeGroupIdPrefixes = ['ru.yamoney', 'ru.yandex.money']    // По умолчанию список пуст
}
```

Также можно исключить из проверки конкретные артефакты:
```groovy
majorVersionChecker {
   excludeDependencies = ["ru.yandex.money.common:yamoney-xml-utils", 
                                        "ru.yandex.money.common:yamoney-json-utils"]  // По умолчанию список пуст
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