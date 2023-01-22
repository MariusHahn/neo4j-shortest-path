package wtf.hahn.neo4j.testUtil;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.FieldsShouldConjunction;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchRuleConstants {

    public static final FieldsShouldConjunction FIELDS_ARE_PRIVATE_OR_FINAL =
            fields().should().bePrivate().orShould().beFinal();

    public static final ArchRule ONLY_ACCESS_NEO4J_PUBLIC_API = noClasses()
            .should()
            .accessClassesThat(new DescribedPredicate<>("are not neo4j public api") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getPackageName().startsWith("org.neo4j")
                            && !javaClass.isAnnotatedWith(org.neo4j.annotations.api.PublicApi.class);
                }
            });
}
