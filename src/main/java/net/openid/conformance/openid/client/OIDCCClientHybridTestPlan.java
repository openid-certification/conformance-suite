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
	testPlanName = "oidcc-client-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Hybrid Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientHybridTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match hybrid relying party tests as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table

		List<Class<? extends TestModule>> moduleListForCodeIdToken = List.of(
			OIDCCClientTest.class, // rp-response_type-code+id_token
			OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
			OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
			OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
			OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
			OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
			//this is marked as "rejection allowed" in profile doc
			OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks

			OIDCCClientTestInvalidCHashInIdToken.class,	//rp-id_token-bad-c_hash
			//TODO this is not listed in profile doc but it is mandatory at https://rp.certification.openid.net:8080/list?profile=CI
			OIDCCClientTestMissingCHashInIdToken.class,	//rp-id_token-missing-c_hash

			OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
			OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256

			//TODO in profile document rp-userinfo-bearer-body is listed
			// as an alternative to rp-userinfo-bearer-header but
			// since we don't have an "either TestA or TestB must pass" mechanism in the framework
			// we should just say that rp-response_type-code+id_token covers these
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
			// https://rp.certification.openid.net:8080/list?profile=CI
			OIDCCClientTestScopeUserInfoClaims.class,	//rp-scope-userinfo-claims

			//OIDCCClientTestClientSecretBasic overrides client authentication variant and always uses client_secret_basic
			OIDCCClientTestClientSecretBasic.class	//rp-token_endpoint-client_secret_basic

			//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
			// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
			//Uses https for all endpoints unless only using code flow
			//Uses HTTPS for all endpoints
			//(implicitly tested)

		);

		List<Class<? extends TestModule>> moduleListForCodeToken = List.of(
			OIDCCClientTest.class, // rp-response_type-code+token
			OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
			OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
			OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
			OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
			OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
			//this is marked as "rejection allowed" in profile doc
			OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks

			OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
			OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256

			//TODO in profile document rp-userinfo-bearer-body is listed
			// as an alternative to rp-userinfo-bearer-header but
			// since we don't have an "either TestA or TestB must pass" mechanism in the framework
			// we should just say that rp-response_type-code+id_token covers these
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
			// https://rp.certification.openid.net:8080/list?profile=CT
			OIDCCClientTestScopeUserInfoClaims.class,	//rp-scope-userinfo-claims

			//OIDCCClientTestClientSecretBasic overrides client authentication variant and always uses client_secret_basic
			OIDCCClientTestClientSecretBasic.class	//rp-token_endpoint-client_secret_basic

			//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
			// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
			//Uses https for all endpoints unless only using code flow
			//Uses HTTPS for all endpoints
			//(implicitly tested)

		);

		List<Class<? extends TestModule>> moduleListForCodeIdTokenToken = List.of(
			OIDCCClientTest.class, // rp-response_type-code+id_token+token
			OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
			OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
			OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
			OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
			OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
			//this is marked as "rejection allowed" in profile doc
			OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks

			OIDCCClientTestInvalidCHashInIdToken.class,	//rp-id_token-bad-c_hash
			//TODO this is not listed in profile doc but it is mandatory at https://rp.certification.openid.net:8080/list?profile=CI
			OIDCCClientTestMissingCHashInIdToken.class,	//rp-id_token-missing-c_hash

			OIDCCClientTestInvalidAtHashInIdToken.class,	//rp-id_token-bad-at_hash
			OIDCCClientTestMissingAtHashInIdToken.class,	//rp-id_token-missing-at_hash


			OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
			OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256

			//TODO in profile document rp-userinfo-bearer-body is listed
			// as an alternative to rp-userinfo-bearer-header but
			// since we don't have an "either TestA or TestB must pass" mechanism in the framework
			// we should just say that rp-response_type-code+id_token covers these
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
			// https://rp.certification.openid.net:8080/list?profile=CIT
			OIDCCClientTestScopeUserInfoClaims.class,	//rp-scope-userinfo-claims

			//OIDCCClientTestClientSecretBasic overrides client authentication variant and always uses client_secret_basic
			OIDCCClientTestClientSecretBasic.class	//rp-token_endpoint-client_secret_basic

			//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
			// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
			//Uses https for all endpoints unless only using code flow
			//Uses HTTPS for all endpoints
			//(implicitly tested)

		);

		final List<Variant> variantResponseTypeCodeIdToken = List.of(
			new Variant(ResponseType.class, "code id_token"),
			new Variant(ResponseMode.class, "default"),
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);

		final List<Variant> variantResponseTypeCodeToken = List.of(
			new Variant(ResponseType.class, "code token"),
			new Variant(ResponseMode.class, "default"),
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);

		final List<Variant> variantResponseTypeCodeIdokenToken = List.of(
			new Variant(ResponseType.class, "code id_token token"),
			new Variant(ResponseMode.class, "default"),
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);

		return List.of(
			new ModuleListEntry(
				moduleListForCodeIdToken,
				variantResponseTypeCodeIdToken
			),
			new ModuleListEntry(
				moduleListForCodeToken,
				variantResponseTypeCodeToken
			),
			new ModuleListEntry(
				moduleListForCodeIdTokenToken,
				variantResponseTypeCodeIdokenToken
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Hybrid RP";
	}

}
