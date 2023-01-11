package wtf.hahn.neo4j.arch;

import com.tngtech.archunit.lang.syntax.elements.FieldsShouldConjunction;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

public class ArchRuleConstants {
    public static final FieldsShouldConjunction FIELDS_ARE_PRIVATE_OR_FINAL =
            fields().should().bePrivate().orShould().beFinal();
}
