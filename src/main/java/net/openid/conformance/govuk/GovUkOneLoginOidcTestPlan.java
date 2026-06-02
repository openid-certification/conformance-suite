package net.openid.conformance.govuk;

import net.openid.conformance.openid.OIDCCClaimsEssential;
import net.openid.conformance.openid.OIDCCEnsureRedirectUriInAuthorizationRequest;
import net.openid.conformance.openid.OIDCCIdTokenRS256;
import net.openid.conformance.openid.OIDCCIdTokenSignature;
import net.openid.conformance.openid.OIDCCIdTokenUnsigned;
import net.openid.conformance.openid.OIDCCRedirectUriQueryMismatch;
import net.openid.conformance.openid.OIDCCRedirectUriQueryOK;
import net.openid.conformance.openid.OIDCCRedirectUriRegFrag;
import net.openid.conformance.openid.OIDCCRefreshTokenRPKeyRotation;
import net.openid.conformance.openid.OIDCCRegistrationJwksUri;
import net.openid.conformance.openid.OIDCCRegistrationLogoUri;
import net.openid.conformance.openid.OIDCCRegistrationPolicyUri;
import net.openid.conformance.openid.OIDCCRegistrationSectorBad;
import net.openid.conformance.openid.OIDCCRegistrationSectorUri;
import net.openid.conformance.openid.OIDCCRegistrationTosUri;
import net.openid.conformance.openid.OIDCCRequestUriSignedRS256;
import net.openid.conformance.openid.OIDCCRequestUriUnsigned;
import net.openid.conformance.openid.OIDCCScopeAddress;
import net.openid.conformance.openid.OIDCCScopeAll;
import net.openid.conformance.openid.OIDCCScopeProfile;
import net.openid.conformance.openid.OIDCCServerRotateKeys;
import net.openid.conformance.openid.OIDCCTestPlan;
import net.openid.conformance.openid.OIDCCUserInfoRS256;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;

import java.util.List;

@PublishTestPlan(
	testPlanName = "govuk-one-login-oidc-test-plan",
	displayName = "GOV.UK One Login: OIDC test plan based on the OIDC Core Basic Certification Profile",
	profile = TestPlan.ProfileNames.optest,
	specFamily = TestPlan.SpecFamilyNames.govuk
)
public class GovUkOneLoginOidcTestPlan extends OIDCCTestPlan {
	private static final List<Class<? extends TestModule>> EXCLUDED_MODULES = List.of(
		// Test modules excluded because they test features unsupported by One Login

		// Dynamic registration
		OIDCCRegistrationJwksUri.class,
		OIDCCRegistrationLogoUri.class,
		OIDCCRegistrationPolicyUri.class,
		OIDCCRegistrationSectorUri.class,
		OIDCCRegistrationTosUri.class,
		OIDCCRedirectUriRegFrag.class,
		OIDCCRegistrationSectorBad.class,

		// Unsupported OIDC claims/scopes
		OIDCCClaimsEssential.class,
		OIDCCScopeAddress.class,
		OIDCCScopeAll.class,
		OIDCCScopeProfile.class,

		// Signed user info response
		OIDCCUserInfoRS256.class,

		// Pass-by-reference request objects (via request_uri)
		OIDCCRequestUriUnsigned.class,
		OIDCCRequestUriSignedRS256.class,

		// Test modules excluded because they are designed to run with dynamic registration only
		// TODO: figure out an alternative way to run these tests

		// ID token signing algorithms
		OIDCCIdTokenRS256.class,
		OIDCCIdTokenSignature.class,
		OIDCCIdTokenUnsigned.class,

		// Further Redirect URI tests
		OIDCCRedirectUriQueryOK.class,
		OIDCCEnsureRedirectUriInAuthorizationRequest.class,
		OIDCCRedirectUriQueryMismatch.class,

		// Key rotation tests
		// Might be possible with JWKS configuration but quite fiddly
		OIDCCRefreshTokenRPKeyRotation.class,
		OIDCCServerRotateKeys.class);

	@Override
	public List<ModuleListEntry> testModulesWithVariants() {
		var variants = List.of(
			new Variant(ServerMetadata.class, "discovery"),
			new Variant(ClientRegistration.class, "static_client")
		);

		// Get modules from the OIDCC Test Plan
		var basicModules = super.testModulesWithVariants();

		// Update the variants and exclude certain unsupported modules
		return basicModules.stream()
			.map(moduleListEntry ->
				new ModuleListEntry(
					moduleListEntry.testModules.stream()
						.filter(testModule -> !EXCLUDED_MODULES.contains(testModule))
						.toList(),
					variants))
			.toList();
	}
}
