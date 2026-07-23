package com.ecommerce.sale.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

class ArchitectureRulesTest {

    private final JavaClasses importedClasses = new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.ecommerce.sale");

    @Test
    void domainShouldNotDependOnOuterLayers() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..application..", "..infrastructure..", "..presentation..");

        rule.check(importedClasses);
    }

    @Test
    void applicationShouldNotDependOnInfrastructureOrPresentation() {
        ArchRule rule = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..infrastructure..", "..presentation..");

        rule.check(importedClasses);
    }

    @Test
    void domainAndApplicationShouldNotUseSpringStereotypes() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().beAnnotatedWith(Component.class)
            .orShould().beAnnotatedWith(Service.class)
            .orShould().beAnnotatedWith(Repository.class);

        rule.check(importedClasses);
    }

    @Test
    void domainAndApplicationShouldNotUseHttpClients() {
        ArchRule rule = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("java.net.http.HttpClient")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
            .orShould().dependOnClassesThat().haveFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient");

        rule.check(importedClasses);
    }

    @Test
    void domainShouldNotUseFloatingPointFields() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..")
            .should(new ArchCondition<>("not declare float or double fields") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    javaClass.getFields().forEach(field -> {
                        String typeName = field.getRawType().getName();
                        boolean valid = !"double".equals(typeName)
                            && !"float".equals(typeName)
                            && !"java.lang.Double".equals(typeName)
                            && !"java.lang.Float".equals(typeName);
                        events.add(new SimpleConditionEvent(field, valid,
                            "Campo " + field.getFullName() + " no debe ser float/double"));
                    });
                }
            });

        rule.check(importedClasses);
    }

    @Test
    void monetaryFieldsShouldUseBigDecimalWhenPresent() {
        ArchRule rule = classes()
            .that().resideInAPackage("..domain..")
            .should(new ArchCondition<>("use BigDecimal for monetary fields when defined") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    javaClass.getFields().stream()
                        .filter(field -> field.getName().toLowerCase().contains("amount"))
                        .forEach(field -> {
                            boolean valid = field.getRawType().isEquivalentTo(BigDecimal.class)
                                || field.getRawType().getName().equals("java.lang.Long")
                                || field.getRawType().isEquivalentTo(long.class);
                            events.add(new SimpleConditionEvent(field, valid,
                                "Campo monetario " + field.getFullName() + " debe ser BigDecimal o Long"));
                        });
                }
            });

        rule.check(importedClasses);
    }

    @Test
    void useCasesShouldExposeSinglePublicExecuteMethod() {
        ArchRule rule = classes()
            .that().resideInAPackage("..application.usecase..")
            .should(new ArchCondition<>("have only one public execute method") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                    List<JavaMethod> publicOwnMethods = javaClass.getMethods().stream()
                        .filter(method -> method.getOwner().equals(javaClass))
                        .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                        .toList();

                    boolean valid = publicOwnMethods.size() == 1
                        && "execute".equals(publicOwnMethods.getFirst().getName());

                    events.add(new SimpleConditionEvent(javaClass, valid,
                        javaClass.getName() + " debe tener exactamente un metodo publico execute"));
                }
            });

        rule.check(importedClasses);
    }
}
