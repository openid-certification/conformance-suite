package net.openid.conformance.openid;

import net.openid.conformance.plan.PublishTestPlan;
import net.openid.conformance.plan.TestPlan;
import net.openid.conformance.testmodule.TestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.ResponseMode;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantSelection;

import java.util.List;

@PublishTestPlan(
	testPlanName = "oidcc-implicit-certification-test-plan",
	displayName = "OpenID Connect Core: Implicit Certification Profile Authorization server test",
	profile = TestPlan.ProfileNames.optest
)
public class OIDCCImplicitTestPlan implements TestPlan {

	public static List<ModuleListEntry> testModulesWithVariants() {
		// ClientRegistration.class is not specified so will be offered in the menu
		// This plan attempts to match 'implicit' as defined here:
		// https://openid.net/wordpress-content/uploads/2018/06/OpenID-Connect-Conformance-Profiles.pdf
		// the tests are in the same order as the table and the comments list the 3rd column in the table.

		// general list of tests - these are run for both response types
		List<Class<? extends TestModule>> moduleList = List.of(
			OIDCCServerTest.class, // OP-Response-id_token
			// OIDCCResponseTypeMissing.class, // OP-Response-Missing - run just once below instead
			// 4 x IdToken.verify() are covered by OIDCCServerTest
			OIDCCIdTokenSignature.class, // OP-IDToken-Signature & OP-IDToken-kid
			// OP-IDToken-at_hash covered by OIDCCServerTest
			// 3 x userinfo tests aren't applicable for response_type=id_token (listed separately below for response_type=id_token token)
			// OpenIDSchema.verify() covered by OP-UserInfo-Endpoint
			OIDCCEnsureRequestWithoutNonceFails.class, // OP-nonce-NoReq-noncode
			// OP-nonce-nonecode covered by OIDCCServerTest
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
			OIDCCEnsureRegisteredRedirectUri.class, // OP-redirect_uri-NotReg
			OIDCCRequestUriUnsignedSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request_uri-Unsigned
			OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported.class, // OP-request-Unsigned
			OIDCCEnsureRequestObjectWithRedirectUri.class, // new test that ensures OP is processing the request object when passing OIDCCUnsignedRequestObjectSupportedCorrectlyOrRejectedAsUnsupported
			OIDCCClaimsEssential.class // OP-claims-essential
		);

		final List<Variant> variantIdTokenTokenBasic = List.of(
			new Variant(ResponseType.class, "id_token token"),
			new Variant(ClientAuthType.class, "client_secret_basic"), // not actually used but we still have to specify it (there's no way to mark a variant not applicable depending on the value of another variant)
			new Variant(ResponseMode.class, "default")
		);
		return List.of(
			new ModuleListEntry(
				moduleList,
				List.of(
					new Variant(ResponseType.class, "id_token"),
					new Variant(ClientAuthType.class, "client_secret_basic"), // not actually used but we still have to specify it (there's no way to mark a variant not applicable depending on the value of another variant)
					new Variant(ResponseMode.class, "default")
				)
			),
			new ModuleListEntry(
				moduleList,
				variantIdTokenTokenBasic
			),
			new ModuleListEntry(
				List.of(
					OIDCCUserInfoGet.class, // OP-UserInfo-Endpoint
					OIDCCUserInfoPostHeader.class, // OP-UserInfo-Header
					OIDCCUserInfoPostBody.class, // OP-UserInfo-Body
					OIDCCResponseTypeMissing.class // OP-Response-Missing - only necessary to run this once, not for each response_type
				),
				variantIdTokenTokenBasic
			)
		);
	}

	public static String certificationProfileName(VariantSelection variant) {
		return "Implicit OP";
	}
}
