package net.openid.conformance.openid.client;

import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryIssuerMismatch;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryJwksUriKeys;
import net.openid.conformance.openid.client.config.OIDCCClientTestDiscoveryOpenIDConfiguration;
import net.openid.conformance.openid.client.config.OIDCCClientTestSigningKeyRotation;
import net.openid.conformance.openid.client.config.OIDCCClientTestSigningKeyRotationNative;
import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-config-certification-test-plan",
	displayName = "OpenID Connect Core: Configuration Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientConfigTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match config relying party tests as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as https://rp.certification.openid.net:8080/list?profile=CNF

		final List<Variant> variantResponseTypeCode = List.of(
			new Variant(ResponseType.class, "code")	//same as python suite
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTestDiscoveryOpenIDConfiguration.class, // rp-discovery-openid-configuration
					OIDCCClientTestDiscoveryJwksUriKeys.class,	//rp-discovery-jwks_uri-keys
					OIDCCClientTestDiscoveryIssuerMismatch.class,	//rp-discovery-issuer-not-matching-config
					OIDCCClientTestIdTokenSigAlgNone.class,	//rp-id_token-sig-none
					//rp-key-rotation-op-sign-key-native does not exist in the profile document
					OIDCCClientTestSigningKeyRotationNative.class,	//rp-key-rotation-op-sign-key-native
					OIDCCClientTestSigningKeyRotation.class	//rp-key-rotation-op-sign-key
					//TODO Decide what to do: rp-userinfo-sig is marked as optional in profile document
					// but not listed at https://rp.certification.openid.net:8080/list?profile=CNF
				),
				variantResponseTypeCode
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Config RP";
	}

}
