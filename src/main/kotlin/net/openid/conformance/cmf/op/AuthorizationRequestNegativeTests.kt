package net.openid.conformance.cmf.op

import com.google.gson.JsonObject
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import net.openid.conformance.condition.Condition
import net.openid.conformance.condition.Condition.ConditionResult
import net.openid.conformance.condition.client.*
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage
import net.openid.conformance.condition.common.ExpectResponseTypeErrorPage
import net.openid.conformance.condition.common.RARSupport
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.Command
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.testmodule.TestFailureException

@PublishTestModule(
	testName = "cmf-chile-op-ensure-registered-redirect-uri",
	displayName = "Chile CMF OP: ensure registered redirect URI",
	summary = "This test uses an unregistered redirect uri. The authorization server should display an error saying the redirect uri is invalid, a screenshot of which should be uploaded.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRegisteredRedirectUri : AbstractCMFChileOPTestModule() {

	override fun extraConfiguration(): TestExecutionUnit =
		Steps.of(
			"Use unregistered redirect uri",
			condition(CreateBadRedirectUriByAppending::class.java),
			Command().exposeEnvironmentString("redirect_uri")
		)

	override fun legs(): List<AuthorizationLeg> =
		listOf(
			rejectedRequestLeg(
				errorPage = expectErrorPage(
					stopOnFailure().requirements("PAR-2.1-3").condition(ExpectRedirectUriErrorPage::class.java),
					"redirect_uri_error"
				),
				parRejected = continueOnFailure().requirements("PAR-2.3").condition(EnsurePARInvalidRequestOrInvalidRequestObjectError::class.java),
				// processCallback() fails the test before these could run
				callback = { Steps.NONE }
			)
		)

	override fun processCallback() {
		throw TestFailureException(id, "The authorization server called the registered redirect uri. This should not have happened as the client provided a bad redirect_uri in the request.")
	}

	override fun handleHttp(path: String, req: HttpServletRequest, res: HttpServletResponse, session: HttpSession, requestParts: JsonObject): Any? {
		callbackEndpoint = "callback/" + env.getString("bad_redirect_path")
		if (path == callbackEndpoint) {
			throw TestFailureException(id, "The authorization server redirected the user to the requested but randomised/unregistered redirect uri. This must not happen as the provided redirect uri could not have been registered.")
		}
		return super.handleHttp(path, req, res, session, requestParts)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-unsigned-authorization-request-without-using-par-fails",
	displayName = "Chile CMF OP: ensure unsigned authorization request without using PAR fails",
	summary = "This test calls the authorization endpoint without using a request object (i.e. with all the parameters passed in the url query), and should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with an invalid_request error.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureUnsignedAuthorizationRequestWithoutUsingParFails : AbstractCMFChileOPTestModule() {
	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.of(
					"Plain authorization request",
					flow.createAuthorizationRequest,
					Steps.block(
						"Make request to authorization endpoint",
						stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-1").condition(BuildPlainRedirectToAuthorizationEndpoint::class.java)
					)
				),
				afterCallback = flow.errorCallback(
					authorizationErrorResponseSteps(
						continueOnFailure().requirements("OIDCC-3.3.2.6").condition(EnsureInvalidRequestError::class.java),
						stateCheck = stateCheckIfPresent()
					)
				),
				placeholder = expectErrorPage(
					stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-1").condition(ExpectAuthorizationRequestWithoutRequestObjectErrorPage::class.java),
					"request_unverifiable_error"
				)
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-redirect-uri-in-authorization-request",
	displayName = "Chile CMF OP: ensure redirect URI in authorization request",
	summary = """This test sends an authorization request that does not include a `redirect_uri` parameter. The following outcomes are permitted:
- the PAR endpoint rejects the request with an `invalid_request` error;
- the authorization endpoint shows an error page saying the redirect uri is missing from the request (a screenshot of which should be uploaded);
- the authorization server accepts the request and uses the registered redirect URI as a default (which is permitted, but not required, by RFC6749 section 3.1.2.3 when only one redirect URI is registered).
""",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRedirectUriInAuthorizationRequest : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(
				stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-6").condition(ExpectRedirectUriMissingErrorPage::class.java),
				"redirect_uri_missing_error"
			),
			parRejected = continueOnFailure().requirements("PAR-2.3").condition(EnsurePARInvalidRequestOrInvalidRequestObjectError::class.java),
			// the server may use the registered redirect uri as a default, so a callback is a normal successful flow
			callback = { it.afterCallback() }
		) {
			insertAfter(
				RARSupport.AddRARToAuthorizationEndpointRequest::class.java,
				Steps.action("Remove redirect_uri from authorization request") { env ->
					env.getObject("authorization_endpoint_request").remove("redirect_uri")
				}
			)
		}
	)
}

/**
 * A response type other than code, which would return tokens through the browser. The error may come back in the
 * query or the fragment, so the callback does not assume the query.
 */
abstract class AbstractCMFChileOPResponseTypeFails : AbstractCMFChileOPTestModule() {

	protected abstract val responseTypeCondition: Class<out Condition>

	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(
				stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").condition(ExpectResponseTypeErrorPage::class.java),
				"response_type_error"
			),
			parRejected = onEndpointResponse(
				CallPAREndpoint.RESPONSE_KEY,
				continueOnFailure().requirements("PAR-2.3").condition(EnsureHttpStatusCodeIs4xx::class.java),
				continueOnFailure().onFail(ConditionResult.WARNING).requirements("PAR-2.3")
					.condition(EnsurePARUnsupportedResponseTypeOrInvalidRequestOrUnauthorizedClientError::class.java)
			),
			callback = { flow ->
				Steps.block(
					flow.client.logPrefix + "Verify authorization endpoint error response",
					continueOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").of(
						"No authorization code",
						RejectAuthCodeInUrlQuery::class.java,
						RejectAuthCodeInUrlFragment::class.java
					),
					condition(DetectWhetherErrorResponseIsInQueryOrFragment::class.java),
					continueOnFailure().requirements("OIDCC-3.3.2.6").condition(EnsureUnsupportedResponseTypeOrInvalidRequestError::class.java),
					authorizationErrorResponseSteps()
				)
			}
		) {
			replace(
				SetAuthorizationEndpointRequestResponseTypeToCode::class.java,
				stopOnFailure().requirements("FAPI2-SP-FINAL-5.3.2.2-1").condition(responseTypeCondition)
			)
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-response-type-code-idtoken-fails",
	displayName = "Chile CMF OP: ensure response_type code id_token fails",
	summary = "This test uses response_type=code id_token in the authorization request, which is not permitted in FAPI2 Security Profile as it would return an id_token via the browser where it may be leaked - only the authorization code flow ('response_type=code') is permitted. The authorization server should show an error message that the response type is unsupported or the request is invalid (a screenshot of which should be uploaded) or the user should be redirected back to the conformance suite with a correct error response, or an error could be returned from the PAR endpoint.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureResponseTypeCodeIdTokenFails : AbstractCMFChileOPResponseTypeFails() {
	override val responseTypeCondition: Class<out Condition> = SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken::class.java
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-response-type-token-fails",
	displayName = "Chile CMF OP: ensure response_type token fails",
	summary = "This test uses response_type=token in the authorization request, which is not permitted in FAPI2 Security Profile as it would return an access token via the browser where it may be leaked - only the authorization code flow ('response_type=code') is permitted. The authorization server should show an error message that the response type is unsupported or the request is invalid (a screenshot of which should be uploaded) or the user should be redirected back to the conformance suite with a correct error response, or an error could be returned from the PAR endpoint.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureResponseTypeTokenFails : AbstractCMFChileOPResponseTypeFails() {
	override val responseTypeCondition: Class<out Condition> = SetAuthorizationEndpointRequestResponseTypeToToken::class.java
}
