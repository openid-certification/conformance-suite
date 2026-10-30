package net.openid.conformance.cmf.op

import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.variant.FAPIOpenIDConnect
import net.openid.conformance.variant.VariantNotApplicable

/*
 * Requests the authorization server may either accept or reject. An accepted request must complete the normal
 * flow; a rejected one must be rejected with the right error.
 */

/**
 * A request sent with a different value outside the request object than inside it, just before the redirect.
 * PAR errors fail the test, as the request pushed to the PAR endpoint is valid.
 */
abstract class AbstractCMFChileOPDifferentValueOutsideRequestObject : AbstractCMFChileOPTestModule() {

	protected abstract fun errorPage(): TestExecutionUnit

	protected abstract fun addDifferentValueOutsideRequestObject(): TestExecutionUnit

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect())
					.insertBefore(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java, addDifferentValueOutsideRequestObject())
					.build(),
				afterCallback = flow.afterCallback(
					onError = authorizationErrorResponseSteps(
						continueOnFailure().requirements("OIDCC-3.3.2.6").condition(EnsureInvalidRequestError::class.java)
					)
				),
				placeholder = errorPage()
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-different-nonce-inside-and-outside-request-object",
	displayName = "Chile CMF OP: ensure different nonce inside and outside request object",
	summary = "This test passes a different nonce in the authorization_endpoint parameters to the one inside the signed request object. There are 3 valid ways the authorization server may handle this: it must either return an invalid_request error back to the client, or must show an error page (saying the request is invalid as the 'nonce' value in the request object and outside it are different - upload a screenshot of the error page), or must successfully authenticate and return the nonce from inside the request object in the id_token.",
	profile = CMF_OP_PROFILE
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPEnsureDifferentNonceInsideAndOutsideRequestObject : AbstractCMFChileOPDifferentValueOutsideRequestObject() {
	override fun errorPage() = expectErrorPage(
		stopOnFailure().requirements("OIDCC-6.1").condition(ExpectRequestDifferentNonceInsideAndOutsideErrorPage::class.java),
		"request_unverifiable_error"
	)

	override fun addDifferentValueOutsideRequestObject(): TestExecutionUnit =
		condition(AddIncorrectNonceToAuthorizationEndpointRequest::class.java)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-different-state-inside-and-outside-request-object",
	displayName = "Chile CMF OP: ensure different state inside and outside request object",
	summary = "This test passes a different state in the authorization_endpoint parameters to the one inside the signed request object. There are 3 valid ways the authorization server may handle this: it must either return an invalid_request error back to the client, or must show an error page (saying the request is invalid as the 'state' value in the request object and outside it are different - upload a screenshot of the error page), or must successfully authenticate and return the state from inside the request object in the id_token.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureDifferentStateInsideAndOutsideRequestObject : AbstractCMFChileOPDifferentValueOutsideRequestObject() {
	override fun errorPage() = expectErrorPage(
		stopOnFailure().requirements("FAPI2-MS-ID1-5.3.2-1").condition(ExpectRequestDifferentStateInsideAndOutsideErrorPage::class.java),
		"request_unverifiable_error"
	)

	override fun addDifferentValueOutsideRequestObject(): TestExecutionUnit =
		condition(AddIncorrectStateToAuthorizationEndpointRequest::class.java)
}

/**
 * A long value the server may reject as invalid_request at the PAR endpoint, with an error page or at the
 * redirect uri. A rejection at the redirect uri is only a warning; an accepted value must be returned intact.
 */
abstract class AbstractCMFChileOPLongValue : AbstractCMFChileOPTestModule() {

	protected abstract fun errorPage(): TestExecutionUnit

	protected abstract fun requestLongValue(modifier: Steps.Modifier)

	protected open fun extraErrorChecks(): TestExecutionUnit = Steps.NONE

	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		val parRejected = continueOnFailure().requirements("PAR-2.3").condition(EnsurePARInvalidRequestError::class.java)
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(flow.beforeRedirect(parRejected)).also { requestLongValue(it) }.build(),
				afterCallback = flow.afterCallback(
					onError = authorizationErrorResponseSteps(
						continueOnFailure().onFail(ConditionResult.WARNING).requirements("OIDCC-3.3.2.6")
							.condition(EnsureInvalidRequestError::class.java),
						extraErrorChecks()
					)
				),
				placeholder = errorPage(),
				redirectWhen = { parAccepted(it) }
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-request-with-long-nonce",
	displayName = "Chile CMF OP: ensure authorization request with long nonce",
	summary = "This test passes a 384 character nonce in the authorization request. The authorization server must either successfully authenticate and return the nonce correctly, or if it does not support nonces that long it may return an invalid_request error back to the client or show an error page (saying the server rejects long nonce - upload a screenshot of the error page). The server MUST NOT return the nonce corrupted or truncated.",
	profile = CMF_OP_PROFILE
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect::class, values = ["plain_oauth"])
class CMFChileOPEnsureAuthorizationRequestWithLongNonce : AbstractCMFChileOPLongValue() {
	override fun errorPage() = expectErrorPage(
		continueOnFailure().onFail(ConditionResult.WARNING).condition(ExpectRequestObjectWithLongNonceErrorPage::class.java)
	)

	override fun requestLongValue(modifier: Steps.Modifier) {
		modifier.insertAfter(SET_REQUESTED_NONCE_LENGTH, putInteger("Request 384 character nonce", "requested_nonce_length", 384))
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-authorization-request-with-long-state",
	displayName = "Chile CMF OP: ensure authorization request with long state",
	summary = "This test passes a long state in the authorization request. The authorization server must either return an invalid_request error back to the client, and must show an error page (saying the server rejects long state - upload a screenshot of the error page) or must successfully authenticate and return the state correctly.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureAuthorizationRequestWithLongState : AbstractCMFChileOPLongValue() {
	override fun errorPage() = expectErrorPage(
		continueOnFailure().onFail(ConditionResult.WARNING).condition(ExpectRequestObjectWithLongStateErrorPage::class.java)
	)

	override fun requestLongValue(modifier: Steps.Modifier) {
		modifier.insertAfter(SET_REQUESTED_STATE_LENGTH, putInteger("Request 1000 character state", "requested_state_length", 1000))
	}

	override fun extraErrorChecks(): TestExecutionUnit =
		continueOnFailure().onFail(ConditionResult.WARNING).requirements("FAPI2-SP-FINAL-5.3.2.2")
			.condition(WarningAboutRejectingLongState::class.java)
}
