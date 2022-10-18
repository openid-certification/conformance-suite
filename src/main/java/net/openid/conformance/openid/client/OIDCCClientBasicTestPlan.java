package net.openid.conformance.openid.client;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.OIDCCClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-client-basic-certification-test-plan",
	displayName = "OpenID Connect Core: Basic Certification Profile Relying Party Tests",
	profile = TestPlan.ProfileNames.rptest
)
public class OIDCCClientBasicTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// This plan attempts to match basic relying party tests as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table

		final List<Variant> variantResponseTypeCode = List.of(
			new Variant(ResponseType.class, "code"),
			new Variant(ResponseMode.class, "default"),
			//Not setting a fixed client authentication variant would also work,
			//because python tests don't care which client authentication method you use
			//and OIDCCClientTestClientSecretBasic always uses client_secret_basic regardless of the selected
			//client authentication variant
			new Variant(OIDCCClientAuthType.class, "client_secret_basic")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCClientTest.class, // rp-response_type-code
					OIDCCClientTestInvalidIssuerInIdToken.class,	//rp-id_token-issuer-mismatch
					OIDCCClientTestMissingSubInIdToken.class,	//rp-id_token-sub
					OIDCCClientTestInvalidAudInIdToken.class,	//rp-id_token-aud
					OIDCCClientTestMissingIatInIdToken.class,	//rp-id_token-iat
					//TODO rp-id_token-kid-absent-single-jwks is optional in the profile document but mandatory at
					// https://rp.certification.openid.net:8080/list?profile=C
					OIDCCClientTestKidAbsentSingleJwks.class,	//rp-id_token-kid-absent-single-jwks
					//TODO rp-id_token-kid-absent-multiple-jwks is optional in the profile document but mandatory at
					// https://rp.certification.openid.net:8080/list?profile=C
					OIDCCClientTestKidAbsentMultipleMatchingKeysInJwks.class,	//rp-id_token-kid-absent-multiple-jwks
					OIDCCClientTestIdTokenSignedUsingRS256.class,	//rp-id_token-sig-rs256
					OIDCCClientTestIdTokenSigAlgNone.class,	//rp-id_token-sig-none
					//TODO rp-id_token-bad-sig-rs256 is optional in the profile document but mandatory at
					// https://rp.certification.openid.net:8080/list?profile=C
					OIDCCClientTestInvalidIdTokenSignatureWithRS256.class,	//rp-id_token-bad-sig-rs256

					//TODO in the profile document rp-userinfo-bearer-body is listed
					// as an alternative to rp-userinfo-bearer-header but
					// since we don't have an "either TestA or TestB must pass" mechanism in the framework
					// we should just say that rp-response_type-code covers these
					//OIDCCClientTestUserinfoBearerHeader.class,	//rp-userinfo-bearer-header
					//OIDCCClientTestUserinfoBearerBody.class,	//rp-userinfo-bearer-header

					//Page 15 row 1:
					//	Does not access UserInfo Endpoint with query parameter method
					//	Does not send Access Token as URI query parameter
					//	(implicitly tested)
					//tested implicitly by OIDCCExtractBearerAccessTokenFromRequest

					OIDCCClientTestInvalidSubInUserinfoResponse.class,	//rp-userinfo-bad-sub-claim
					OIDCCClientTestNonceInvalid.class,	//rp-nonce-invalid

					//Profile document page 15:
					//	Scope openid present in all requests
					//	'openid' scope value should be present in the Authentication Request
					//	(implicitly tested)
					//tested implicitly by EnsureOpenIDInScopeRequest

					//TODO rp-scope-userinfo-claims is optional in the profile document but mandatory at
					// https://rp.certification.openid.net:8080/list?profile=C
					OIDCCClientTestScopeUserInfoClaims.class,	//rp-scope-userinfo-claims

					//OIDCCClientTestClientSecretBasic overrides client authentication variant and always uses client_secret_basic
					OIDCCClientTestClientSecretBasic.class	//rp-token_endpoint-client_secret_basic

					//TODO (clarify) this is enforced by EnsureValidRedirectUriForAuthorizationEndpointRequest for redirect_uri.
					// should it be enforced for something other than the redirect_uri (e.g jwks_uri)?
					//Uses https for all endpoints unless only using code flow
					//Uses HTTPS for all endpoints
					//(implicitly tested)
				),
				variantResponseTypeCode
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Basic RP";
	}

}
