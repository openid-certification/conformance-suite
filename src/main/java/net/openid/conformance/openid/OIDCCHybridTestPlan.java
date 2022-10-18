package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-hybrid-certification-test-plan",
	displayName = "OpenID Connect Core: Hybrid Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCHybridTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// ClientRegistration.class is not specified so will be offered in the menu
		// This plan attempts to match hybrid as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table and the comments list the 3rd column in the table.
		// the table is somewhat misleading, as the python suite actually requires users to run the tests 3 times
		// (apart from the OP-Response-* tests which explicitly call out the different response types), meaning the
		// table is repeated below for each response type
		final List<Variant> variantCodeIdTokenBasic = List.of(
			new Variant(ResponseType.class, "code id_token"),
			new Variant(ClientAuthType.class, "client_secret_basic"),
			new Variant(ResponseMode.class, "default")
		);
		final List<Variant> variantCodeTokenBasic = List.of(
			new Variant(ResponseType.class, "code token"),
			new Variant(ClientAuthType.class, "client_secret_basic"),
			new Variant(ResponseMode.class, "default")
		);
		final List<Variant> variantCodeIdTokenTokenBasic = List.of(
			new Variant(ResponseType.class, "code id_token token"),
			new Variant(ClientAuthType.class, "client_secret_basic"),
			new Variant(ResponseMode.class, "default")
		);
		return List.of(
			// 1st run of table; response_type=code id_token
			new ModuleListEntry(
				List.of(
					OIDCCServerTest.class, // OP-Response-code+id_token / OP-Response-code+token / OP-Response-code+id_token+token
					OIDCCResponseTypeMissing.class, // OP-Response-Missing
					// 4 x IdToken.verify() are covered by OIDCCServerTest
					OIDCCIdTokenSignature.class, // OP-IDToken-Signature & OP-IDToken-kid
					// OP-IDToken-at_hash & OP-IDToken-c_hash are covered by OIDCCServerTest
					OIDCCUserInfoGet.class, // OP-UserInfo-Endpoint
					OIDCCUserInfoPostHeader.class, // OP-UserInfo-Header
					OIDCCUserInfoPostBody.class, // OP-UserInfo-Body
					// OpenIDSchema.verify() covered by OP-UserInfo-Endpoint
					OIDCCEnsureRequestWithoutNonceFails.class, // OP-nonce-NoReq-noncode
					// OP-nonce-noncode covered by OIDCCServerTest
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
					OIDCCEnsureRegisteredRedirectUri.class // OP-redirect_uri-NotReg
					// OP-ClientAuth-Basic-Dynamic covered by OIDCServerTest
					// OP-ClientAuth-Basic-Static covered by OIDCServerTest
				),
				variantCodeIdTokenBasic
			),
			new ModuleListEntry(
				List.of(OIDCCServerTestClientSecretPost.class), // OP-ClientAuth-SecretPost-Dynamic
				List.of(
					new Variant(ResponseType.class, "code id_token"),
					new Variant(ClientAuthType.class, "client_secret_post"),
					new Variant(ResponseMode.class, "default")
				)
			),
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
				variantCodeIdTokenBasic
			),
			// 2nd run of table; response_type=code token
			new ModuleListEntry(
				List.of(
					OIDCCServerTest.class, // OP-Response-code+id_token / OP-Response-code+token / OP-Response-code+id_token+token
					// OIDCCResponseTypeMissing.class, // OP-Response-Missing - does not need to be repeated, it's the same for all response_type
					// 4 x IdToken.verify() are covered by OIDCCServerTest
					OIDCCIdTokenSignature.class, // OP-IDToken-Signature & OP-IDToken-kid
					// OP-IDToken-at_hash & OP-IDToken-c_hash are covered by OIDCCServerTest
					OIDCCUserInfoGet.class, // OP-UserInfo-Endpoint
					OIDCCUserInfoPostHeader.class, // OP-UserInfo-Header
					OIDCCUserInfoPostBody.class, // OP-UserInfo-Body
					// OpenIDSchema.verify() covered by OP-UserInfo-Endpoint
					OIDCCEnsureRequestWithoutNonceSucceedsForCodeFlow.class, // OP-nonce-NoReq-noncode - sort of; a request without nonce should succeed for code token
					// OP-nonce-noncode covered by OIDCCServerTest
					// OP-IDToken-Signature here is a duplicate covered above
					OIDCCScopeProfile.class, // OP-scope-profile
					OIDCCScopeEmail.class, // OP-scope-email
					OIDCCScopeAddress.class, // OP-scope-address
					OIDCCScopePhone.class, // OP-scope-phone
					OIDCCScopeAll.class, // OP-scope-All
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
					OIDCCEnsureRegisteredRedirectUri.class // OP-redirect_uri-NotReg
					// OP-ClientAuth-Basic-Dynamic covered by OIDCServerTest
					// OP-ClientAuth-Basic-Static covered by OIDCServerTest
				),
				variantCodeTokenBasic
			),
			new ModuleListEntry(
				List.of(OIDCCServerTestClientSecretPost.class), // OP-ClientAuth-SecretPost-Dynamic
				List.of(
					new Variant(ResponseType.class, "code token"),
					new Variant(ClientAuthType.class, "client_secret_post"),
					new Variant(ResponseMode.class, "default")
				)
			),
			// OP-ClientAuth-SecretPost-Static same as OP-ClientAuth-SecretPost-Dynamic
			new ModuleListEntry(
				List.of(
					OIDCCRequestUriUnsignedSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request_uri-Unsigned
					OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request-Unsigned
					OIDCCClaimsEssential.class, // OP-claims-essential
					OIDCCEnsureRequestObjectWithRedirectUri.class, // new test that ensures OP is processing the request object when passing OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported
					OIDCCRefreshToken.class // new test; skipped if refresh tokens not supported
				),
				variantCodeTokenBasic
			),
			// 3rd run of table: response_type=code id_token token
			new ModuleListEntry(
				List.of(
					OIDCCServerTest.class, // OP-Response-code+id_token / OP-Response-code+token / OP-Response-code+id_token+token
					// OIDCCResponseTypeMissing.class, // OP-Response-Missing - does not need to be repeated, it's the same for all response_type
					// 4 x IdToken.verify() are covered by OIDCCServerTest
					OIDCCIdTokenSignature.class, // OP-IDToken-Signature & OP-IDToken-kid
					// OP-IDToken-at_hash & OP-IDToken-c_hash are covered by OIDCCServerTest
					OIDCCUserInfoGet.class, // OP-UserInfo-Endpoint
					OIDCCUserInfoPostHeader.class, // OP-UserInfo-Header
					OIDCCUserInfoPostBody.class, // OP-UserInfo-Body
					// OpenIDSchema.verify() covered by OP-UserInfo-Endpoint
					OIDCCEnsureRequestWithoutNonceFails.class, // OP-nonce-NoReq-noncode
					// OP-nonce-noncode covered by OIDCCServerTest
					// OP-IDToken-Signature here is a duplicate covered above
					OIDCCScopeProfile.class, // OP-scope-profile
					OIDCCScopeEmail.class, // OP-scope-email
					OIDCCScopeAddress.class, // OP-scope-address
					OIDCCScopePhone.class, // OP-scope-phone
					OIDCCScopeAll.class, // OP-scope-All
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
					OIDCCEnsureRegisteredRedirectUri.class // OP-redirect_uri-NotReg
					// OP-ClientAuth-Basic-Dynamic covered by OIDCServerTest
					// OP-ClientAuth-Basic-Static covered by OIDCServerTest
				),
				variantCodeIdTokenTokenBasic
			),
			new ModuleListEntry(
				List.of(OIDCCServerTestClientSecretPost.class), // OP-ClientAuth-SecretPost-Dynamic
				List.of(
					new Variant(ResponseType.class, "code id_token token"),
					new Variant(ClientAuthType.class, "client_secret_post"),
					new Variant(ResponseMode.class, "default")
				)
			),
			// OP-ClientAuth-SecretPost-Static same as OP-ClientAuth-SecretPost-Dynamic
			new ModuleListEntry(
				List.of(
					OIDCCRequestUriUnsignedSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request_uri-Unsigned
					OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request-Unsigned
					OIDCCClaimsEssential.class, // OP-claims-essential
					OIDCCEnsureRequestObjectWithRedirectUri.class, // new test that ensures OP is processing the request object when passing OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported
					OIDCCRefreshToken.class // new test; skipped if refresh tokens not supported
				),
				variantCodeIdTokenTokenBasic
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Hybrid OP";
	}
}
