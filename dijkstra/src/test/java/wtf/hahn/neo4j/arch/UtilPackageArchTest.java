package wtf.hahn.neo4j.arch;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "wtf.hahn.neo4j.util")
public class UtilPackageArchTest {

    @ArchTest
    private final ArchRule util_is_independent = noClasses().should()
            .dependOnClassesThat()
            .resideInAnyPackage("wtf.hahn.neo4j..");

    @ArchTest
    private final ArchRule test_classes_are_in_same_package = GeneralCodingRules.testClassesShouldResideInTheSamePackageAsImplementation();

    @ArchTest
    private final ArchRule fields_are_private_or_final = ArchRuleConstants.FIELDS_ARE_PRIVATE_OR_FINAL;

}
