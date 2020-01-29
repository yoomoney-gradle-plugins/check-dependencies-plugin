package ru.yandex.money.gradle.plugins.library.dependencies;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.FixedDependencies;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.ConfigurationConflictsAnalyzer;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.ConflictVersionsChecker;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.ConflictedLibraryInfo;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.resolvers.DummyVersionConflictResolver;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.resolvers.RepositoryVersionConflictResolver;
import ru.yandex.money.gradle.plugins.library.dependencies.analysis.conflicts.resolvers.VersionConflictResolver;
import ru.yandex.money.gradle.plugins.library.dependencies.dsl.VersionSelectors;
import ru.yandex.money.gradle.plugins.library.dependencies.exclusions.ExclusionRulesLoader;
import ru.yandex.money.gradle.plugins.library.dependencies.exclusions.StaleExclusionsDetector;
import ru.yandex.money.gradle.plugins.library.dependencies.repositories.Repository;
import ru.yandex.money.gradle.plugins.library.dependencies.repositories.aether.AetherRepository;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Задача на проверку согласованности изменений версий используемых библиотек. Если изменение версии библиотеки связано
 * с фиксацией версии в <i>Spring Dependency Management</i> плагине, то останавливает билд и выводит список библиотек,
 * у которых изменение версий не запланировано.
 *
 * @author Brovin Yaroslav (brovin@yamoney.ru)
 * @since 27.01.2017
 */
public class CheckDependenciesTask extends ConventionTask {

    private final Logger log = LoggerFactory.getLogger(CheckDependenciesTask.class);

    private final CheckDependenciesReporter reporter = new CheckDependenciesReporter();
    private ConflictVersionsChecker conflictVersionsChecker;
    private StaleExclusionsDetector staleExclusionsDetector;
    private FixedDependencies fixedDependencies;
    private VersionConflictResolver conflictResolver;

    @Input
    @Optional
    private List<String> exclusionsRulesSources;
    @Input
    private List<String> excludedConfigurations;
    @Input
    private VersionSelectors versionSelectors;

    /**
     * Запускается при выполнении таски
     */
    @TaskAction
    public void check() {
        ExclusionRulesLoader exclusionRulesLoader = loadExclusionsRules();
        conflictVersionsChecker = new ConflictVersionsChecker(exclusionRulesLoader.getTotalExclusionRules());
        staleExclusionsDetector = StaleExclusionsDetector.create(exclusionRulesLoader.getLocalExclusionRules());
        fixedDependencies = FixedDependencies.from(getProject());
        conflictResolver = createVersionConflictResolver();

        boolean hasVersionsConflict = false;
        for (Configuration configuration : getCheckedConfigurations()) {
            List<ConflictedLibraryInfo> conflictedLibraries = calculateConflictedVersionsLibrariesFor(configuration);
            if (!conflictedLibraries.isEmpty()) {
                reporter.reportConflictedLibrariesForConfiguration(configuration, conflictedLibraries);
                hasVersionsConflict = true;
            }
        }

        if (hasVersionsConflict) {
            throw new IllegalStateException(reporter.getFormattedReport());
        }

        if (staleExclusionsDetector.hasStaleExclusions()) {
            reporter.reportStaleExclusions(staleExclusionsDetector.getStaleExclusions());
            throw new IllegalStateException(reporter.getFormattedReport());
        }
    }

    private VersionConflictResolver createVersionConflictResolver() {
        VersionSelectors versionSelectors = getVersionSelectors();

        if (versionSelectors.count() > 0) {
            Repository repository = AetherRepository.create(getMavenRepositoryUrls());
            return new RepositoryVersionConflictResolver(repository, getVersionSelectors());
        }

        return new DummyVersionConflictResolver();
    }

    private List<String> getMavenRepositoryUrls() {
        Collection<ArtifactRepository> repositories = getProject().getRepositories().getAsMap().values();
        List<String> mavenUrls = new ArrayList<>(repositories.size());

        for (ArtifactRepository repository: repositories) {
            if (repository instanceof MavenArtifactRepository) {
                mavenUrls.add(((MavenArtifactRepository)repository).getUrl().toString());
            } else {
                log.warn("Non-maven repository was skipped: {}", repository.getName());
            }
        }

        return mavenUrls;
    }

    /**
     * Возвращает сет проверяемых конфигураций с учетом настройки плагина (Списка исключенных из проверки конфигураций)
     *
     * @return Набор проверяемых конфигураций
     */
    private Iterable<Configuration> getCheckedConfigurations() {
        List<String> excludedConfigurations = getExcludedConfigurations();
        return getProject().getConfigurations().matching(configuration ->
                excludedConfigurations == null || !excludedConfigurations.contains(configuration.getName())
        );
    }

    private ExclusionRulesLoader loadExclusionsRules() {
        ExclusionRulesLoader loader = new ExclusionRulesLoader();
        List<String> exclusionsSources = getExclusionsRulesSources();
        if (exclusionsSources != null) {
            loader.load(getProject(), exclusionsSources);
        }
        return loader;
    }

    /**
     * Анализирует версии библиотек для конфигурации проекта и сравнивает их с со списком фиксированных версий библиотек для конфигурации.
     *
     * @param configuration конфигурация сборки
     * @return Правомерны изменения версий библиотек или нет
     **/
    private List<ConflictedLibraryInfo> calculateConflictedVersionsLibrariesFor(@Nonnull Configuration configuration) {
        return ConfigurationConflictsAnalyzer.create(fixedDependencies, configuration,
                conflictVersionsChecker, staleExclusionsDetector, conflictResolver)
                .findConflictedLibraries();
    }

    /**
     * Возвращает список источников местоположений файлов с правилами разрешающими изменение версии библиотек.
     * <p>
     * ВАЖНО: Не смотря на тривиальный код геттера, Gradle перехватывает вызов этого геттера и анализирует возвращаемое значение.
     * Если оно null, то gradle попытается взять значение для свойства "exclusionsRulesSources" из getConventionMapping().
     *
     * @return список источников местоположений файлов
     */
    @Nullable
    List<String> getExclusionsRulesSources() {
        return exclusionsRulesSources;
    }

    /**
     * Задает список местоположений файлов с правилами разрешающими изменение версий библиотек. Этот сеттер может быть использован
     * из Gradle Build скрипта.
     *
     * @param exclusionsRulesSources набор местоположений файлов правил
     */
    void setExclusionsRulesSources(List<String> exclusionsRulesSources) {
        this.exclusionsRulesSources = new ArrayList<>(exclusionsRulesSources);
    }

    /**
     * Возвращает список конфигураций, которые не должны проверяться плагином.
     * <p>
     * ВАЖНО: Не смотря на тривиальный код геттера, Gradle перехватывает вызов этого геттера и анализирует возвращаемое значение.
     * Если оно null, то gradle попытается взять значение для свойства "excludedConfigurations" из getConventionMapping().
     *
     * @return список исключенных из проверки конфигураций
     */
    @Nullable
    List<String> getExcludedConfigurations() {
        return excludedConfigurations;
    }

    /**
     * Задает список конфигураций, которые не должны проверяться плагином.
     *
     * @param excludedConfigurations список исключаемых конфигураций
     */
    void setExcludedConfigurations(List<String> excludedConfigurations) {
        this.excludedConfigurations = new ArrayList<>(excludedConfigurations);
    }

    VersionSelectors getVersionSelectors() {
        return versionSelectors;
    }

    void setVersionSelector(VersionSelectors versionSelectors) {
        this.versionSelectors = versionSelectors;
    }
}