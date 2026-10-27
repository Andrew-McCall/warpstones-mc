package dev.warpstones.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "dev.warpstones.core", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreArchitectureTest {
    @ArchTest
    static final ArchRule coreDependsOnlyOnJdkAndNightConfig = classes()
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("java..", "dev.warpstones.core..", "com.electronwill.nightconfig..");
}
