package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-config-certification-test-plan",
	displayName = "OpenID Connect Core: Config Certification Profile Authorization server test ",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCConfigTestPlan implements TestPlan {
	public static List<ModuleListEntry> testModulesWithVariants() {
		return List.of(
			new ModuleListEntry(
				List.of(
					// This plan attempts to match 'config' as defined here:
					// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
					// the tests are in the same order as the table and the comments list the 3rd column in the table.

					// OP-IDToken-none - this appears to be a mistake in the above PDF; this test has no relevance to the config
					// profile and it not included in the python config profile.
					OIDCCDiscoveryEndpointVerification.class // OP-Discovery-Config
					// ProviderConfigurationResponse.verify() included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointIssuer
					// ProviderConfigurationResponse.verify() included in OIDCCDiscoveryEndpointVerification, also in CheckDiscEndpointIssuer
					// IdToken.verify() - described as "Discovered issuer matches ID Token iss value" - isn't in the python config
					//                    tests, can't be done in a pure discovery test as we have no id token (checked by
					//                    OIDCCServerTest in the other certification profiles)
					// CheckEndpoint() included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointAuthorizationEndpoint
					// CheckEndpoint() included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointTokenEndpoint
					// CheckEndpoint() included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointUserinfoEndpoint
					// OP-Discovery-jwks_uri included in OIDCCDiscoveryEndpointVerification, CheckJwksUri
					// OP-Discovery-JWKs - included in OIDCCDiscoveryEndpointVerification, ValidateServerJWKs
					// CheckScopeSupport() - included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointScopesSupportedContainsOpenId
					// ProviderConfigurationResponse.verify() - included in OIDCCDiscoveryEndpointVerification, OIDCCCheckDiscEndpointResponseTypesSupported
					// ProviderConfigurationResponse.verify() - included in OIDCCDiscoveryEndpointVerification, OIDCCCheckDiscEndpointSubjectTypesSupported
					// ProviderConfigurationResponse.verify() - included in OIDCCDiscoveryEndpointVerification, OIDCCCheckDiscEndpointIdTokenSigningAlgValuesSupported
					// OP-Discovery-claims_supported - included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointClaimsParameterSupported
					// VerifyOPEndpointsUseHTTPS() - included in OIDCCDiscoveryEndpointVerification, CheckDiscEndpointAllEndpointsAreHttps
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery"),
					new Variant(ClientRegistration.class, "static_client")
				)
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Config OP";
	}
}
