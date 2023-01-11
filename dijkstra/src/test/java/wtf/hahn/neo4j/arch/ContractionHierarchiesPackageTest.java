package wtf.hahn.neo4j.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "wtf.hahn.neo4j.contractionHierarchies")
public class ContractionHierarchiesPackageTest {

    @ArchTest
    private final ArchRule does_not_depend_on_dijkstra = noClasses()
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("wtf.hahn.neo4j.dijkstra");

    @ArchTest
    private final ArchRule only_access_neo4j_public_api = noClasses()
            .should()
            .accessClassesThat(new DescribedPredicate<>("are not neo4j public api") {
        @Override
        public boolean test(JavaClass javaClass) {
            return javaClass.getPackageName().startsWith("org.neo4j")
                    && !javaClass.isAnnotatedWith(org.neo4j.annotations.api.PublicApi.class);
        }
    });

}
