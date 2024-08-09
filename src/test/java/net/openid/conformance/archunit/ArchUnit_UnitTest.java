package net.openid.conformance.archunit;

import ch.qos.logback.classic.Level;
import com.google.gson.JsonElement;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameContaining;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@ExtendWith(MockitoExtension.class)
public class ArchUnit_UnitTest {
	@BeforeEach
	public void setup() {
		// disable debug logging for ArchUnit as it's quite spammy
		ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("com.tngtech.archunit");
		root.setLevel(Level.INFO);
	}

	@Test
	public void doNotCallJsonElementGetAs() {
		JavaClasses importedClasses = new ClassFileImporter().importPackages("net.openid.conformance");

		// see https://gitlab.com/openid/conformance-suite/-/issues/398 for more explanation
		JavaClasses allExceptOIDFJSON = importedClasses.that(DescribedPredicate.not(nameContaining("OIDFJSON")));

		ArchRule rule = noClasses().should().callMethodWhere(
			target(nameMatching("getAs[^J].*")) // ignores getAsJsonObject/getAsJsonPrimitive/etc which are fine
				.and(target(owner(assignableTo(JsonElement.class)))
		)).because("the getAs methods perform implicit conversions that might not be desirable - use OIDFJSON wrapper instead");

		rule.check(allExceptOIDFJSON);
	}
}
