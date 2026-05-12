package net.openid.conformance.variant;

import com.google.gson.JsonObject;
import net.openid.conformance.info.Plan;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.AbstractTestModule;
import net.openid.conformance.testmodule.PublishTestModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for a bug in {@link VariantService.TestModuleHolder#isApplicableForVariant}
 * where {@code effectiveAllowedValues.isEmpty()} unconditionally returned {@code true},
 * incorrectly including modules whose statically-restricted parameter had ALL remaining values
 * conditionally excluded for the selected variant.
 *
 */
class VariantServiceIsApplicableForVariantTestCase {

    @VariantParameters({FAPI2FinalOPProfile.class, VCICredentialOfferParameterVariant.class, VCIWalletAuthorizationCodeFlowVariant.class, ClientAuthType.class})
	@VariantNotApplicable(parameter = ClientAuthType.class, values = {
		"none", "client_secret_basic", "client_secret_post", "client_secret_jwt"
	})
	@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = {
		"plain_fapi"
	})
	@VariantNotApplicableWhen(
		parameter = VCICredentialOfferParameterVariant.class,
		values = {"by_reference"},
		whenParameter = FAPI2FinalOPProfile.class,
		hasValues = {"vci", "vci_haip", "fapi_client_credentials_grant"}
	)

	public static class FakeAbstractModule extends AbstractTestModule {
        @Override
        public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
            throw new UnsupportedOperationException("fake module — not for execution");
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException("fake module — not for execution");
        }
    }

	@PublishTestModule(
		testName = "variant-service-bug-demo-restricted-module",
		displayName = "Bug Demo: Restricted Module",
		profile = "Test an OpenID Provider / Authorization Server",
		summary = "Fake module used only in VariantServiceIsApplicableForVariant_UnitTest"
	)
	@VariantNotApplicable(parameter = VCICredentialOfferParameterVariant.class, values = {"by_value"})
	public static class FakeRestrictedModule extends FakeAbstractModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
			throw new UnsupportedOperationException("fake module — not for execution");
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException("fake module — not for execution");
		}
	}

	@PublishTestModule(
		testName = "variant2-service-bug-demo-restricted-module",
		displayName = "Bug Demo: Unrestricted Module",
		profile = "Test an OpenID Provider / Authorization Server",
		summary = "Fake module used only in VariantServiceIsApplicableForVariant_UnitTest"
	)
	public static class FakeUnrestrictedModule extends AbstractTestModule {
		@Override
		public void configure(JsonObject config, String baseUrl, String externalUrlOverride, String baseMtlsUrl) {
			throw new UnsupportedOperationException("fake module — not for execution");
		}

		@Override
		public void start() {
			throw new UnsupportedOperationException("fake module — not for execution");
		}
	}

    @PublishTestPlan(
        testPlanName = "variant-service-bug-demo-plan",
        displayName = "Variant Service Bug Demo Plan",
        profile = "Test an OpenID Provider / Authorization Server",
        specFamily = TestPlan.SpecFamilyNames.fapi2SecurityProfile,
        testModules = {FakeRestrictedModule.class, FakeUnrestrictedModule.class}
    )
    public static class FakeBugDemoPlan implements TestPlan {}

    private static final String MODULE_NAME = "variant-service-bug-demo-restricted-module";

    private VariantService.TestPlanHolder plan;

    @BeforeEach
    void setUp() {
        plan = new VariantService(holder -> true).getTestPlan("variant-service-bug-demo-plan");
    }

    @Test
    void moduleIsExcludedWhenStaticAndConditionalExclusionsLeaveNoAllowedValues() {
        VariantSelection variant = new VariantSelection(Map.of(
            "client_auth_type", "private_key_jwt",
			"fapi_profile", "vci"
        ));

        List<Plan.Module> modules = plan.getTestModulesForVariant(variant);

        assertFalse(
            modules.stream().anyMatch(m -> m.getTestModule().equals(MODULE_NAME)),
            "A module restricted to by_value only must not appear when by_value is also " +
            "conditionally excluded (effectiveAllowedValues is empty but allowedValues was already restricted)"
        );
    }

    @Test
    void moduleIsExcludedWhenFilteredOut() {
		VariantSelection variant = new VariantSelection(Map.of(
			"client_auth_type", "private_key_jwt",
			"fapi_profile", "vci",
			"vci_credential_offer_variant", "by_value"
		));

        List<Plan.Module> modules = plan.getTestModulesForVariant(variant);

        assertFalse(
            modules.stream().anyMatch(m -> m.getTestModule().equals(MODULE_NAME)),
            "A module restricted to by_value should appear when by_value is not conditionally excluded"
        );
    }

	@Test
	void moduleIsIncludedWhenAllowedValueExistsForVariant() {
		VariantSelection variant = new VariantSelection(Map.of(
			"client_auth_type", "private_key_jwt",
			"fapi_profile", "plain_fapi",
			"vci_credential_offer_variant", "by_reference"
		));

		List<Plan.Module> modules = plan.getTestModulesForVariant(variant);

		assertFalse(
			modules.stream().anyMatch(m -> m.getTestModule().equals(MODULE_NAME)),
			"A module restricted to by_value should appear when by_value is not conditionally excluded"
		);
	}
}
