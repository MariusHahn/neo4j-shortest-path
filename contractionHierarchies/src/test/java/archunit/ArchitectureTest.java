package archunit;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import wtf.hahn.neo4j.testUtil.ArchRuleConstants;

@AnalyzeClasses(packages = "wtf.hahn.neo4j.contractionHierarchies")
public class ArchitectureTest {
    @ArchTest
    private final ArchRule test_classes_are_in_same_package = GeneralCodingRules.testClassesShouldResideInTheSamePackageAsImplementation();

    @ArchTest
    private final ArchRule fields_are_private_or_final = ArchRuleConstants.FIELDS_ARE_PRIVATE_OR_FINAL;

    @ArchTest
    private final ArchRule only_access_neo4j_public_api = ArchRuleConstants.ONLY_ACCESS_NEO4J_PUBLIC_API;
}
