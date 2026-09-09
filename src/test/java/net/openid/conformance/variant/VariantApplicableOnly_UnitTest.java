package net.openid.conformance.variant;

import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import variantvalidationfixtures.ApplicableOnlyFixtures;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code @VariantApplicableOnly} restricts a module to the listed values, and composes with
 * itself and with {@code @VariantNotApplicable} across the class hierarchy as an intersection.
 */
class VariantApplicableOnly_UnitTest {

	private static VariantService variantService;

	@BeforeAll
	static void setUp() {
		variantService = new VariantService(holder -> true);
	}

	private static Set<ClientAuthType> applicableValues(Class<? extends TestModule> moduleClass) {
		VariantService.TestModuleHolder holder = variantService.new TestModuleHolder(moduleClass);
		return Arrays.stream(ClientAuthType.values())
				.filter(v -> holder.isApplicableForVariant(new VariantSelection(Map.of("client_auth_type", v.toString()))))
				.collect(toSet());
	}

	@Test
	void onlyListedValuesAreApplicable() {
		assertThat(applicableValues(ApplicableOnlyFixtures.SingleValueModule.class))
				.containsExactly(ClientAuthType.MTLS);
	}

	@Test
	void notApplicableOnBaseClassStillExcludesValueListedInSubclassWhitelist() {
		assertThat(applicableValues(ApplicableOnlyFixtures.ApplicableOnlyAfterNotApplicableModule.class))
				.containsExactly(ClientAuthType.MTLS);
	}

	@Test
	void whitelistsAcrossHierarchyIntersect() {
		assertThat(applicableValues(ApplicableOnlyFixtures.IntersectingApplicableOnlyModule.class))
				.containsExactly(ClientAuthType.PRIVATE_KEY_JWT);
	}
}
