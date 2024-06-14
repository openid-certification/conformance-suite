package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-basic-certification-test-plan",
	displayName = "OpenID Connect Core: Basic Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCBasicTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// ClientRegistration.class is not specified so will be offered in the menu
		// This plan attempts to match 'basic' as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table and the comments list the 3rd column in the table.

		final List<Variant> variantCodeBasic = List.of(
			new Variant(ResponseType.class, "code"),
			// the choice of client_secret_basic here is relatively arbitary, and client_secret_post could have been
			// used instead - the certification profile requires that both basic and post are tested, but doesn't
			// dictate which variant the other tests are run with
			new Variant(ClientAuthType.class, "client_secret_basic"),
			new Variant(ResponseMode.class, "default")
		);
		return List.of(
			new ModuleListEntry(
				List.of(
					OIDCCServerTest.class, // OP-Response-code
					OIDCCResponseTypeMissing.class, // OP-Response-Missing
					// 4 x IdToken.verify() are covered by OIDCCServerTest
					OIDCCIdTokenSignature.class, // OP-IDToken-Signature & OP-IDToken-kid
					OIDCCIdTokenUnsigned.class, // OP-IDToken-none
					OIDCCUserInfoGet.class, // OP-UserInfo-Endpoint
					OIDCCUserInfoPostHeader.class, // OP-UserInfo-Header
					OIDCCUserInfoPostBody.class, // OP-UserInfo-Body
					// OpenIDSchema.verify() covered by OP-UserInfo-Endpoint
					OIDCCEnsureRequestWithoutNonceSucceedsForCodeFlow.class, // OP-nonce-NoReq-code
					// OP-nonce-code covered by OIDCCServerTest
					// OP-IDToken-Signature here is a duplicate covered above
					OIDCCScopeProfile.class, // OP-scope-profile
					OIDCCScopeEmail.class, // OP-scope-email
					OIDCCScopeAddress.class, // OP-scope-address
					OIDCCScopePhone.class, // OP-scope-phone
					OIDCCScopeAll.class, // OP-scope-All
					OIDCEnsureOtherScopeOrderSucceeds.class, // new test in java suite
					OIDCCDisplayPage.class, // OP-display-page
					OIDCCDisplayPopup.class, // OP-display-popup
					OIDCCPromptLogin.class, // OP-prompt-login
					OIDCCPromptNoneNotLoggedIn.class, // OP-prompt-none-NotLoggedIn
					OIDCCPromptNoneLoggedIn.class, // OP-prompt-none-LoggedIn
					OIDCCMaxAge1.class, // 3 x OP-Req-max_age=1
					OIDCCMaxAge10000.class, // OP-Req-max_age=10000
					OIDCCEnsureRequestWithUnknownParameterSucceeds.class, // OP-Req-NotUnderstood
					OIDCCIdTokenHint.class, // OP-Req-id_token_hint
					OIDCCLoginHint.class, // OP-Req-login_hint
					OIDCCUiLocales.class, // OP-Req-ui_locales
					OIDCCClaimsLocales.class, // OP-Req-claims_locales
					OIDCCEnsureRequestWithAcrValuesSucceeds.class, // OP-Req-acr_values
					// VerifyState() covered by OIDCCServerTest
					OIDCCAuthCodeReuse.class, // OP-OAuth-2nd
					OIDCCAuthCodeReuseAfter30Seconds.class, // OP-OAuth-2nd-30s
					// OP-OAuth-2nd-Revokes covered by oidcc-codereuse-30seconds
					// OP-OAuth-2nd-30s is already covered
					OIDCCEnsureRegisteredRedirectUri.class, // OP-redirect_uri-NotReg
					// OP-ClientAuth-Basic-Dynamic covered by OIDCServerTest
					// OP-ClientAuth-Basic-Static covered by OIDCServerTest
					OIDCCEnsurePostRequestSucceeds.class
				),
				variantCodeBasic
			),
			// now switch variants to check that client_secret_post works
			new ModuleListEntry(
				List.of(OIDCCServerTestClientSecretPost.class), // OP-ClientAuth-SecretPost-Dynamic
				List.of(
					new Variant(ResponseType.class, "code"),
					new Variant(ClientAuthType.class, "client_secret_post"),
					new Variant(ResponseMode.class, "default")
				)
			),
			// remaining variants run with original variants
			// OP-ClientAuth-SecretPost-Static same as OP-ClientAuth-SecretPost-Dynamic
			new ModuleListEntry(
				List.of(
					OIDCCRequestUriUnsignedSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request_uri-Unsigned
					OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request-Unsigned
					OIDCCClaimsEssential.class, // OP-claims-essential
					OIDCCEnsureRequestObjectWithRedirectUri.class, // new test that ensures OP is processing the request object when passing OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported
					OIDCCRefreshToken.class, // new test; skipped if refresh tokens not supported
					OIDCCEnsureRequestWithValidPkceSucceeds.class // new test
					),
				variantCodeBasic
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Basic OP";
	}
}
