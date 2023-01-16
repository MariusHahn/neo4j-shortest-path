package wtf.hahn.neo4j.arch;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "wtf.hahn.neo4j")
public class GeneralRules {

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
