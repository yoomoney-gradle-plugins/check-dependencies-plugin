package ru.yandex.money.gradle.plugins.library.dependencies.checkversion;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.money.gradle.plugins.library.dependencies.dsl.LibraryName;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Выполняет проверку по всем найденным конфликтам мажорных версий
 *
 * @author horyukova
 * @since 09.12.2018
 */
class CheckVersionAction implements Action<DependencyResolveDetails> {
    private static final Logger log = LoggerFactory.getLogger(CheckVersionAction.class);

    private final Project project;
    private final Map<LibraryName, Set<String>> conflictModules;
    private final Boolean pushMetrics;
    private final Boolean failBuild;
    private final MetricsSender metricsSender;

    CheckVersionAction(Project project, Map<LibraryName, Set<String>> conflictModules,
                       MajorVersionCheckerExtension majorVersionCheckerExtension,
                       MetricsSender metricsSender) {
        this.project = project;
        this.conflictModules = conflictModules;
        this.pushMetrics = majorVersionCheckerExtension.pushMetrics;
        this.failBuild = majorVersionCheckerExtension.failBuild;
        this.metricsSender = metricsSender;
    }

    @Override
    public void execute(DependencyResolveDetails dependency) {

        LibraryName libraryName = new LibraryName(dependency.getRequested().getGroup(), dependency.getRequested().getName());

        if (conflictModules.containsKey(libraryName)) {
            String errorMsg = String.format("There is major version conflict for dependency=%s:%s, versions=%s",
                    libraryName.getGroup(), libraryName.getName(), conflictModules.get(libraryName));
            log.error(errorMsg);

            if (pushMetrics) {
                metricsSender.sendMajorConflict(libraryName);
            }

            if (failBuild && !isDependenciesTask()) {
                throw new GradleException(errorMsg);
            }
        }
    }

    private boolean isDependenciesTask() {
        List<String> taskNames = project.getGradle().getStartParameter().getTaskNames();
        return !taskNames.isEmpty() && taskNames.get(0).endsWith("dependencies");
    }
}