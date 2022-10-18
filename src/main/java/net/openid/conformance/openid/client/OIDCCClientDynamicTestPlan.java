package net.openid.conformance.openid.client;

import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryIssuerMismatch;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryJwksUriKeys;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryOpenIDConfiguration;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryWebfingerAcct;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryWebfingerURL;
import net.openid.conformance.openid.client.config.OIDCCClientTestDynamicRegistration;
import net.openid.conformance.openid.client.config.OIDCCClientTestSigningKeyRotation;
import net.openid.conformance.openid.client.config.OIDCCClientTestSigningKeyRotationNative;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ClientRequestType;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-dynamic-certification-test-plan",
	displayName = "OpenID Connect Core: Dynamic Certification Profile Relying Party Tests",
	summary = "This plan requires response_type 'code', request_uri support and dynamic client registration for all tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientDynamicTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match dynamic relying party tests as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as https://rp.certification.openid.net:8080/list?profile=DYN

		final List<Variant> variantResponseTypeCode = List.of(
			//same as python suite
			new Variant(ResponseType.class, "code"),
			//because dynamic client registration support is required
			new Variant(ClientRegistration.class, "dynamic_client"),
			//because request_uri support is required for two tests
			new Variant(ClientRequestType.class, "request_uri")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestDiscoveryWebfingerAcct.class,	//rp-discovery-webfinger-acct
					OIDCCClientTestDiscoveryWebfingerURL.class,	//rp-discovery-webfinger-url
					OIDCCClientTestDiscoveryOpenIDConfiguration.class, // rp-discovery-openid-configuration
					OIDCCClientTestDiscoveryJwksUriKeys.class,	//rp-discovery-jwks_uri-keys
					OIDCCClientTestDiscoveryIssuerMismatch.class,	//rp-discovery-issuer-not-matching-config
					OIDCCClientTestDynamicRegistration.class,	//rp-registration-dynamic
					OIDCCClientTestRequestUriSignedWithRS256.class,	//rp-request_uri-sig
					OIDCCClientTestRequestUriSignedWithNone.class,	//rp-request_uri-unsigned
					OIDCCClientTestIdTokenSigAlgNone.class,	//rp-id_token-sig-none

					//rp-key-rotation-op-sign-key-native does not exist in the profile document
					OIDCCClientTestSigningKeyRotationNative.class,	//rp-key-rotation-op-sign-key-native
					OIDCCClientTestSigningKeyRotation.class,	//rp-key-rotation-op-sign-key

					OIDCCClientTestSignedUserinfo.class	//rp-userinfo-sig
				),
				variantResponseTypeCode
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Dynamic RP";
	}

}
