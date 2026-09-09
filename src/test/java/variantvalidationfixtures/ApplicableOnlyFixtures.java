package variantvalidationfixtures;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantApplicableOnly;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantParameters;

/**
 * Fixtures for VariantApplicableOnly_UnitTest, covering how {@code @VariantApplicableOnly}
 * composes with itself and with {@code @VariantNotApplicable} across a class hierarchy.
 * This package is intentionally outside the {@code net.openid} classpath-scan root so
 * VariantService never picks these up.
 */
public final class ApplicableOnlyFixtures {

	private ApplicableOnlyFixtures() {
	}

	@PublishTestModule(testName = "fixture-applicable-only-single", displayName = "fixture", profile = "fixture")
	@VariantParameters({ClientAuthType.class})
	@VariantApplicableOnly(parameter = ClientAuthType.class, values = {"mtls"})
	public abstract static class SingleValueModule implements TestModule {
	}

	@VariantParameters({ClientAuthType.class})
	@VariantNotApplicable(parameter = ClientAuthType.class, values = {"none"})
	public abstract static class NotApplicableBase implements TestModule {
	}

	/** Base excludes "none"; the subclass whitelist re-listing it must not bring it back. */
	@PublishTestModule(testName = "fixture-applicable-only-after-not-applicable", displayName = "fixture", profile = "fixture")
	@VariantApplicableOnly(parameter = ClientAuthType.class, values = {"mtls", "none"})
	public abstract static class ApplicableOnlyAfterNotApplicableModule extends NotApplicableBase {
	}

	@VariantParameters({ClientAuthType.class})
	@VariantApplicableOnly(parameter = ClientAuthType.class, values = {"mtls", "private_key_jwt"})
	public abstract static class ApplicableOnlyBase implements TestModule {
	}

	/** Two whitelists in the hierarchy intersect rather than union. */
	@PublishTestModule(testName = "fixture-applicable-only-intersection", displayName = "fixture", profile = "fixture")
	@VariantApplicableOnly(parameter = ClientAuthType.class, values = {"private_key_jwt", "none"})
	public abstract static class IntersectingApplicableOnlyModule extends ApplicableOnlyBase {
	}
}
