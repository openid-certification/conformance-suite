package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.FAPIOpenIDConnect
import net.openid.conformance.variant.VariantNotApplicable

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-multiple-aud-succeeds",
	displayName = "Chile CMF OP: ensure request object with multiple aud succeeds",
	summary = "This test pass aud value as an array containing good and bad values then server must accept it.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureServerAcceptsRequestObjectWithMultipleAud : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(modifiedLeg {
		replace(
			AddAudToRequestObject::class.java,
			stopOnFailure().requirements("RFC7519-4.1.3").condition(AddMultipleAudToRequestObject::class.java)
		)
		replace(
			BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java,
			stopOnFailure().requirements("PAR-4").condition(BuildRequestObjectByReferenceRedirectToAuthorizationEndpointWithoutDuplicates::class.java)
		)
	})
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-request-without-state-success",
	displayName = "Chile CMF OP: ensure authorization endpoint request without state success",
	summary = "This test makes an authentication request that does not include 'state'. State is an optional parameter, so the authorization server must successfully authenticate and must not return state nor s_hash.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureAuthorizationRequestWithoutStateSuccess : AbstractCMFChileOPTestModule() {
	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = withoutState(flow),
				afterCallback = flow.afterCallback(onSuccess = noStateSuccessChecks())
			)
		)
	}
}

/** The authorization request with state left out of the request object. */
internal fun withoutState(flow: OPFlow): TestExecutionUnit =
	Steps.modify(flow.beforeRedirect())
		.skip(AddStateToAuthorizationEndpointRequest::class.java, "NOT adding state to request object")
		.build()

/** A successful response to a request without state must not contain state. */
internal fun noStateSuccessChecks(): TestExecutionUnit =
	Steps.of(
		"Verify successful authorization response without state",
		condition(CheckMatchingCallbackParameters::class.java),
		condition(CheckIfAuthorizationEndpointError::class.java),
		continueOnFailure().condition(VerifyNoStateInAuthorizationResponse::class.java),
		authorizationCodeSteps()
	)

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-request-without-nonce-success",
	displayName = "Chile CMF OP: ensure authorization endpoint request without nonce success",
	summary = "This test makes an authentication request that does not include 'nonce'. nonce is an optional parameter for response_type=code, so the authorization server must successfully authenticate and must not return a nonce.",
	profile = CMF_OP_PROFILE
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPEnsureAuthorizationRequestWithoutNonceSuccess : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(modifiedLeg {
		skip(CreateRandomNonceValue::class.java, "NOT creating nonce")
		skip(AddNonceToAuthorizationEndpointRequest::class.java, "NOT adding nonce to request object")
	})
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-request-with-64-char-nonce-success",
	displayName = "Chile CMF OP: ensure authorization request with 64 char nonce",
	summary = "This test passes a 64 character nonce in the authorization request. As per https://openid.net/specs/fapi-security-profile-2_0-final.html#section-5.3.2.2 item 14, the authorization server must successfully authenticate and return the nonce correctly.",
	profile = CMF_OP_PROFILE
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPEnsureAuthorizationRequestWith64CharNonceSuccess : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(modifiedLeg {
		insertAfter(SET_REQUESTED_NONCE_LENGTH, putInteger("Request 64 character nonce", "requested_nonce_length", 64))
	})
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-other-scope-order-succeeds",
	displayName = "Chile CMF OP: ensure other scope order succeeds",
	summary = "This test makes a FAPI authorization request but with the order of the entries in the 'scope' reversed, which must succeed. As per RFC6749 section 3.3, 'If the value contains multiple space-delimited strings, their order does not matter'. The reason for this test is that some OAuth clients process scopes in a way that the order they are sent to the server is not under the control of a developer using that client, and as per the spec such requests must still be accepted.",
	profile = CMF_OP_PROFILE
)
// plain OAuth may not have the multiple scopes this test needs
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPEnsureOtherScopeOrderSucceeds : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(modifiedLeg {
		insertAfter(
			RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
			stopOnFailure().requirements("RFC6749-3.3").condition(ReverseScopeOrderInAuthorizationEndpointRequest::class.java)
		)
	})
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-nbf-8-seconds-in-the-future-is-accepted",
	displayName = "Chile CMF OP: ensure request object with nbf 8 seconds in the future is accepted",
	summary = "This test ensures the Authorization Server accepts request objects with a nbf claim slightly in the future (e.g., 8 seconds) to account for clock skew. It verifies robustness against minor time discrepancies, avoiding unnecessary request rejections.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithNbf8SecondsInTheFutureIsAccepted : AbstractCMFChileOPTestModule() {
	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		// rejecting the request is only a warning: the server does not tolerate clock skew, and the test ends
		val parRejected = Steps.of(
			"PAR endpoint does not tolerate clock skew",
			continueOnFailure().onFail(ConditionResult.WARNING).requirements("PAR-2.2", "PAR-2.3")
				.condition(CheckPAREndpointResponse201WithNoError::class.java),
			Steps.check("PAR endpoint does not tolerate clock skew") { _, c ->
				c.log("PAR endpoint doesn't seem to support clock skew, finishing test prematurely.")
			}
		)
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect(parRejected))
					.replace(
						AddNbfToRequestObject::class.java,
						stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.1").condition(AddNbfValueIs8SecondsInFutureToRequestObject::class.java)
					)
					.build(),
				afterCallback = flow.afterCallback(),
				redirectWhen = { parAccepted(it) }
			)
		)
	}
}
