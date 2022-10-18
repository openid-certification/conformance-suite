package net.openid.conformance.openid.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.OIDCCClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-implicit-certification-test-plan",
	displayName = "OpenID Connect Core: Implicit Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientImplicitTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match implicit relying party tests as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table

		List<Class<? extends TestModule>> moduleListForIdToken = List.of(
			OIDCCClientTest.class, // rp-response_type-id_token
			OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
			OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
			OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
			OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
			OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
			//this is marked as "rejection allowed" in profile doc
			OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks
			OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
			OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256

			OIDCCClientTestNonce.class,	//rp-nonce-unless-code-flow
			OIDCCClientTestNonceInvalid.class,	//rp-nonce-invalid

			//Profile document page 15:
			//	Scope openid present in all requests
			//	'openid' scope value should be present in the Authentication Request
			//	(implicitly tested)
			//tested implicitly by EnsureOpenIDInScopeRequest

			//TODO rp-scope-userinfo-claims is optional in the profile document but mandatory at
			// https://rp.certification.openid.net:8080/list?profile=I
			OIDCCClientTestScopeUserInfoClaims.class	//rp-scope-userinfo-claims

			//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
			// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
			//Uses https for all endpoints unless only using code flow
			//Uses HTTPS for all endpoints
			//(implicitly tested)

		);

		List<Class<? extends TestModule>> moduleListForIdTokenToken = List.of(
			OIDCCClientTest.class, // rp-response_type-id_token+token
			OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
			OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
			OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
			OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
			OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
			//this is marked as "rejection allowed" in profile doc
			OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks
			OIDCCClientTestInvalidAtHashInIdToken.class,	//rp-id_token-bad-at_hash
			//TODO rp-id_token-missing-at_hash is not listed in the profile document but it is mandatory in the python suite
			// see https://rp.certification.openid.net:8080/list?profile=IT
			OIDCCClientTestMissingAtHashInIdToken.class,	//rp-id_token-missing-at_hash
			OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
			OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256
			//In profile document rp-userinfo-bearer-body is listed as an alternative to rp-userinfo-bearer-header
			// since we don't have an "either TestA or TestB must pass" mechanism in the framework
			// we should just say that rp-response_type-code and rp-response_type-id_token+token cover these
			//OIDCCClientTestUserinfoBearerHeader.class,	//rp-userinfo-bearer-header
			//OIDCCClientTestUserinfoBearerBody.class,	//rp-userinfo-bearer-header

			//Profile document page 15 row 1:
			//	Does not access UserInfo Endpoint with query parameter method
			//	Does not send Access Token as URI query parameter
			//	(implicitly tested)
			//tested implicitly by OIDCCExtractBearerAccessTokenFromRequest

			OIDCCClientTestInvalidSubInUserinfoResponse.class,	//rp-userinfo-bad-sub-claim
			OIDCCClientTestNonce.class,	//rp-nonce-unless-code-flow
			OIDCCClientTestNonceInvalid.class,	//rp-nonce-invalid

			//Profile document page 15:
			//	Scope openid present in all requests
			//	'openid' scope value should be present in the Authentication Request
			//	(implicitly tested)
			//tested implicitly by EnsureOpenIDInScopeRequest

			//TODO rp-scope-userinfo-claims is optional in the profile document but mandatory at
			// https://rp.certification.openid.net:8080/list?profile=IT
			OIDCCClientTestScopeUserInfoClaims.class	//rp-scope-userinfo-claims

			//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
			// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
			//Uses https for all endpoints unless only using code flow
			//Uses HTTPS for all endpoints
			//(implicitly tested)

		);


		final List<Variant> variantResponseTypeIdToken = List.of(
			new Variant(ResponseType.class, "id_token"),
			new Variant(ResponseMode.class, "default"),
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);

		final List<Variant> variantResponseTypeIdTokenToken = List.of(
			new Variant(ResponseType.class, "id_token token"),
			new Variant(ResponseMode.class, "default"),
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);

		return List.of(
			new ModuleListEntry(
				moduleListForIdToken,
				variantResponseTypeIdToken
			),
			new ModuleListEntry(
				moduleListForIdTokenToken,
				variantResponseTypeIdTokenToken
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Implicit RP";
	}

}
