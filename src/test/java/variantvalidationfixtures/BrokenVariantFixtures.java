package variantvalidationfixtures;

import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPIResponseMode;
import net.openid.conformance.variant.VariantConfigurationFields;
import net.openid.conformance.variant.VariantHidesConfigurationFields;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantNotApplicableWhen;
import net.openid.conformance.variant.VariantParameter;
import net.openid.conformance.variant.VariantParameters;
import net.openid.conformance.variant.VariantSetup;

import java.util.List;

/**
 * Fixtures with deliberately invalid variant annotation values, for
 * VariantValueValidation_UnitTest. This package is intentionally outside the
 * {@code net.openid} classpath-scan root so VariantService never picks these up.
 *
 * The module fixtures reference real (scanned) parameter enums because
 * TestModuleHolder resolves parameters against the scanned registry.
 */
public final class BrokenVariantFixtures {

	private BrokenVariantFixtures() {
	}

	@VariantParameter(name = "bad_default_param", displayName = "Bad Default", description = "unit test fixture", defaultValue = "no_such_value")
	public enum BadDefaultParam {
		ONLY
	}

	@PublishTestModule(testName = "fixture-bad-not-applicable", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class})
	@VariantNotApplicable(parameter = ClientAuthType.class, values = {"no_such_value"})
	public abstract static class BadNotApplicableModule implements TestModule {
	}

	@PublishTestModule(testName = "fixture-bad-configuration-fields", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class})
	@VariantConfigurationFields(parameter = ClientAuthType.class, value = "no_such_value", configurationFields = {"client.fixture"})
	public abstract static class BadConfigurationFieldsModule implements TestModule {
	}

	@PublishTestModule(testName = "fixture-bad-hides-configuration-fields", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class})
	@VariantHidesConfigurationFields(parameter = ClientAuthType.class, value = "no_such_value", configurationFields = {"client.fixture"})
	public abstract static class BadHidesConfigurationFieldsModule implements TestModule {
	}

	@PublishTestModule(testName = "fixture-bad-setup", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class})
	public abstract static class BadSetupModule implements TestModule {
		@VariantSetup(parameter = ClientAuthType.class, value = "no_such_value")
		public void badSetup() {
		}
	}

	@PublishTestModule(testName = "fixture-bad-not-applicable-when-values", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class, FAPIResponseMode.class})
	@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = {"no_such_value"}, whenParameter = FAPIResponseMode.class, hasValues = {"jarm"})
	public abstract static class BadNotApplicableWhenValuesModule implements TestModule {
	}

	@PublishTestModule(testName = "fixture-bad-not-applicable-when-has-values", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class, FAPIResponseMode.class})
	@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = {"none"}, whenParameter = FAPIResponseMode.class, hasValues = {"no_such_value"})
	public abstract static class BadNotApplicableWhenHasValuesModule implements TestModule {
	}

	/** Every annotation type with valid values (including the "*" wildcard) — must construct without error. */
	@PublishTestModule(testName = "fixture-good", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class, FAPIResponseMode.class})
	@VariantNotApplicable(parameter = ClientAuthType.class, values = {"client_secret_basic"})
	@VariantConfigurationFields(parameter = ClientAuthType.class, value = "mtls", configurationFields = {"client.fixture"})
	@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = {"none"}, whenParameter = FAPIResponseMode.class, hasValues = {"jarm"})
	@VariantNotApplicableWhen(parameter = ClientAuthType.class, values = {"*"}, whenParameter = FAPIResponseMode.class, hasValues = {"plain_response"})
	public abstract static class GoodModule implements TestModule {
		@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
		public void goodSetup() {
		}
	}

	public static class BadExclusionPlan implements TestPlan {
		@Override
		public List<Variant> variantsNotApplicable() {
			return List.of(new Variant(ClientAuthType.class, "no_such_value"));
		}
	}

	public enum NotAVariantParameter {
		SOMETHING
	}

	public static class NotAVariantParameterPlan implements TestPlan {
		@Override
		public List<Variant> variantsNotApplicable() {
			return List.of(new Variant(NotAVariantParameter.class, "something"));
		}
	}
}
