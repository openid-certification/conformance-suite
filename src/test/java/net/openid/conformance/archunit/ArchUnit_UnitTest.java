package net.openid.conformance.archunit;

import ch.qos.logback.classic.Level;
import com.google.gson.JsonElement;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import net.openid.conformance.logging.TestInstanceEventLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameContaining;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

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

	/**
	 * Condition unit tests MUST NOT {@code @Mock} a {@link TestInstanceEventLog}; they should
	 * use {@code BsonEncoding.testInstanceEventLog()} instead. That factory returns a
	 * Mockito-spied real log whose payloads are round-tripped through the same BSON encode
	 * path {@code DBEventLog} uses in production, so any un-encodable value (missing codec,
	 * dotted map key, etc.) fails at unit-test time rather than in a live test run.
	 *
	 * <p>A small allowlist exists for tests that deliberately stub/verify the mock's methods
	 * (i.e. they are testing {@code AbstractCondition}'s forwarding behaviour or the log wiring
	 * itself, not the BSON payload). If you're adding a new condition {@code _UnitTest} you
	 * should not need to extend the allowlist — use the factory.
	 */
	@Test
	public void testInstanceEventLogShouldNotBeMocked() {
		JavaClasses testClasses = new ClassFileImporter()
			.withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
			.importPackages("net.openid.conformance");

		Set<String> allowlistedClasses = Set.of(
			// Tests whose whole purpose is to verify that AbstractCondition forwards log calls
			// correctly to the event log — they need a Mockito mock with verify(...).
			"net.openid.conformance.condition.AbstractCondition_UnitTest");

		JavaClasses scope = testClasses.that(new DescribedPredicate<>("not in TestInstanceEventLog @Mock allowlist") {
			@Override
			public boolean test(JavaClass javaClass) {
				return !allowlistedClasses.contains(javaClass.getFullName());
			}
		});

		ArchRule rule = noFields()
			.that().haveRawType(TestInstanceEventLog.class)
			.should().beAnnotatedWith(Mock.class)
			.because("condition _UnitTests must use BsonEncoding.testInstanceEventLog() instead "
				+ "of @Mock TestInstanceEventLog, so every logged payload is round-tripped "
				+ "through the same BSON encode path DBEventLog uses in production");

		rule.check(scope);
	}
}
