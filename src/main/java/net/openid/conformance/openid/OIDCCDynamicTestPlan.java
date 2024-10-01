package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ServerMetadata;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-dynamic-certification-test-plan",
	displayName = "OpenID Connect Core: Dynamic Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCDynamicTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// ResponseType.class is not specified so will be offered in the menu
		// This plan attempts to match 'dynamic' as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table and the comments list the 3rd column in the table.

		final List<Variant> variantPrivateKeyJwtDynReg = List.of(
			new Variant(ServerMetadata.class, "discovery"),
			// most tests it doesn't matter what client auth is used, but private_key_jwt is required for [at least]
			// OIDCCRegistrationJwksUri, OIDCCRefreshTokenRPKeyRotation
			new Variant(ClientAuthType.class, "private_key_jwt"),
			new Variant(ClientRegistration.class, "dynamic_client"),
			new Variant(ResponseMode.class, "default")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCIdTokenRS256.class, // OP-IDToken-RS256
					OIDCCIdTokenUnsigned.class, // OP-IDToken-none
					OIDCCUserInfoRS256.class, // OP-UserInfo-RS256
					OIDCCEnsureRedirectUriInAuthorizationRequest.class, // OP-redirect_uri-Missing
					OIDCCRedirectUriQueryOK.class, // OP-redirect_uri-Query-OK
					// OP-redirect_uri-Query-OK - two identical entries in the PDF
					OIDCCRedirectUriQueryMismatch.class,// OP-redirect_uri-Query-Mismatch
					OIDCCRedirectUriQueryAdded.class, // OP-redirect_uri-Query-Added
					OIDCCRedirectUriRegFrag.class // OP-redirect_uri-RegFrag
				),
				variantPrivateKeyJwtDynReg
			),
			new ModuleListEntry(
				List.of(
					// next section exactly matches OIDCCConfigTestPlan
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
					// end of duplicate of OIDCCConfigTestPlan
				),
				List.of(
					new Variant(ServerMetadata.class, "discovery"),
					new Variant(ClientRegistration.class, "dynamic_client")
				)
			),
			new ModuleListEntry(
				List.of(
					// OP-Discovery-WebFinger-Email - listed in PDF but not included by python code for this profile
					// OP-Discovery-WebFinger - listed in PDF but not included by python code for this profile
					OIDCCServerTest.class, // OP-Registration-Endpoint
					// OP-Registration-Dynamic - covered by OIDCCServerTest
					OIDCCRegistrationLogoUri.class, // OP-Registration-logo_uri
					OIDCCRegistrationPolicyUri.class, // OP-Registration-policy_uri
					OIDCCRegistrationTosUri.class, // OP-Registration-tos_uri
					// OP-Registration-jwks - covered by OIDCCServerTest
					OIDCCRegistrationJwksUri.class, // OP-Registration-jwks_uri
					OIDCCRegistrationSectorUri.class, // not in pdf/python - positive test for sectoruri, without which OIDCCRegistrationSectorBad could pass for any number of reasons
					OIDCCRegistrationSectorBad.class // OP-Registration-Sector-Bad
				),
				variantPrivateKeyJwtDynReg
			),
			new ModuleListEntry(
				List.of(
					OIDCCServerRotateKeys.class // OP-Rotation-OP-Sig
				),
				List.of(new Variant(ServerMetadata.class, "discovery"))
			),
			new ModuleListEntry(
				List.of(
					OIDCCRefreshTokenRPKeyRotation.class, // OP-Rotation-RP-Sig
					// OP-request_uri-Support - pdf lists this, but python classifies it as an 'extra' test not in the certification profile
					OIDCCRequestUriUnsigned.class, // OP-request_uri-Unsigned-Dynamic
					OIDCCRequestUriSignedRS256.class, // OP-request_uri-Sig
					OIDCCEnsureRequestObjectWithRedirectUri.class, // new test that ensures OP is processing the request object when passing OIDCCRequestUriUnsigned
					OIDCCRefreshToken.class, // extra test not in python (skipped if refresh tokens not supported)
					OIDCCEnsureClientAssertionWithIssAudSucceeds.class // extra test not in python (warnings only if failed)
				),
				variantPrivateKeyJwtDynReg
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Dynamic OP";
	}
}
