package net.openid.conformance.cmf.op

import net.openid.conformance.condition.client.*
import net.openid.conformance.sequence.Steps
import net.openid.conformance.sequence.Steps.condition
import net.openid.conformance.sequence.Steps.continueOnFailure
import net.openid.conformance.sequence.Steps.stopOnFailure
import net.openid.conformance.testmodule.PublishTestModule
import net.openid.conformance.testmodule.TestExecutionUnit
import net.openid.conformance.util.JWKUtil

/*
 * Request objects the authorization server must reject. With PAR the rejection may come from the PAR endpoint, an
 * error page, or an error at the redirect uri.
 */

private fun invalidRequestOrAccessDenied(): TestExecutionUnit =
	continueOnFailure().requirements("OIDCC-3.1.2.6", "RFC6749-4.2.2.1")
		.condition(EnsureInvalidRequestInvalidRequestUriOrAccessDeniedError::class.java)

private fun invalidRequestObject(): TestExecutionUnit =
	continueOnFailure().requirements("OIDCC-3.1.2.6").condition(EnsureInvalidRequestObjectError::class.java)

private fun invalidRequestUri(): TestExecutionUnit =
	continueOnFailure().requirements("JAR-4").condition(EnsureInvalidRequestUriError::class.java)

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-without-exp-fails",
	displayName = "Chile CMF OP: ensure request object without exp fails",
	summary = "This test should end with the authorization server showing an error message: invalid_request, invalid_request_object (for request object by value), invalid_request_uri (when PAR in use) or access_denied (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithoutExpFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-4").condition(ExpectRequestObjectMissingExpClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2", "PAR-2.3"),
			callback = expectAuthorizationError(invalidRequestOrAccessDenied())
		) {
			skip(AddExpToRequestObject::class.java, "NOT adding exp to request object")
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-without-nbf-fails",
	displayName = "Chile CMF OP: ensure request object without nbf fails",
	summary = "This test should end with the authorization server showing an error message: invalid_request, invalid_request_object (for request object by value), invalid_request_uri (when PAR in use) or access_denied (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithoutNbfFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-3").condition(ExpectRequestObjectMissingNbfClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.3", "PAR-2.1-3"),
			callback = expectAuthorizationError(invalidRequestOrAccessDenied())
		) {
			skip(AddNbfToRequestObject::class.java, "NOT adding nbf to request object")
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-state-only-outside-request-object-not-used",
	displayName = "Chile CMF OP: check that a state value passed only outside request object is not used",
	summary = "This test uses a request object that does not contain state, but state is passed in the url parameters to the authorization endpoint (hence state should be ignored, as FAPI says only parameters inside the request object should be used). The expected result is a successful authentication that returns neither state nor s_hash. It is also permissible to show (a screenshot of which should be uploaded) or return an error message: invalid_request, invalid_request_object or access_denied.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPStateOnlyOutsideRequestObjectNotUsed : AbstractCMFChileOPTestModule() {
	override fun legs(): List<AuthorizationLeg> {
		val flow = flow()
		return listOf(
			AuthorizationLeg(
				beforeRedirect = Steps.modify(withoutState(flow))
					.insertBefore(BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint::class.java, condition(AddStateToAuthorizationEndpointRequest::class.java))
					.build(),
				afterCallback = flow.afterCallback(
					onError = authorizationErrorResponseSteps(
						continueOnFailure().requirements("OIDCC-3.1.2.6", "RFC6749-4.2.2.1")
							.condition(EnsureInvalidRequestInvalidRequestObjectInvalidRequestUriOrAccessDeniedError::class.java),
						// the request object had no state, so the error response may have none either
						stateCheck = stateCheckIfPresent()
					),
					onSuccess = noStateSuccessChecks()
				),
				placeholder = expectErrorPage(stopOnFailure().requirements("RFC6749-4.1.1").condition(ExpectRequestObjectMissingStateErrorPage::class.java))
			)
		)
	}
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-without-redirect-uri-fails",
	displayName = "Chile CMF OP: ensure request object without redirect_uri fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid due to the missing redirect uri (a screenshot of which should be uploaded), or in an error from the PAR endpoint, or (in the case where the client only has one redirect_uri registered) with the user being redirected back to the client's registered redirect_uri with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithoutRedirectUriFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-MS-ID1-5.3.2-1").condition(ExpectRequestObjectMissingRedirectUriErrorPage::class.java)),
			parRejected = continueOnFailure().requirements("JAR-6.2").condition(EnsurePARInvalidRequestOrInvalidRequestObjectError::class.java),
			callback = expectAuthorizationError(
				continueOnFailure().requirements("OIDCC-3.1.2.6").condition(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject::class.java)
			)
		) {
			insertBefore(SignRequestObject::class.java, condition(RemoveRedirectUriFromRequestObject::class.java))
			// the claims the redirect is built from must not reintroduce it either
			insertBefore(CallPAREndpoint::class.java, condition(RemoveRedirectUriFromRequestObject::class.java))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-expired-request-object-fails",
	displayName = "Chile CMF OP: ensure expired request object fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureExpiredRequestObjectFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("RFC7519-4.1.4").condition(ExpectExpiredRequestObjectClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2", "PAR-2.3"),
			callback = expectAuthorizationError(invalidRequestObject())
		) {
			replace(AddExpToRequestObject::class.java, stopOnFailure().requirements("RFC7519-4.1.4").condition(AddExpiredExpToRequestObject::class.java))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-bad-aud-fails",
	displayName = "Chile CMF OP: ensure request object with bad values of aud fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithBadAudFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("OIDCC-6.1", "RFC7519-4.1.3").condition(ExpectRequestObjectWithBadAudClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2", "PAR-2.3"),
			callback = expectAuthorizationError(invalidRequestUri())
		) {
			replace(AddAudToRequestObject::class.java, stopOnFailure().requirements("OIDCC-6.1", "RFC7519-4.1.3").condition(AddBadAudToRequestObject::class.java))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-exp-over-60-fails",
	displayName = "Chile CMF OP: ensure request object with exp value of more then than 60 minutes after the nbf value fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithExpOver60Fails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-4").condition(ExpectRequestObjectWithExpOver60ClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.3", "PAR-2.1-3"),
			callback = expectAuthorizationError(invalidRequestUri())
		) {
			replace(
				AddExpToRequestObject::class.java,
				stopOnFailure().requirements("OIDCC-6.1", "RFC7519-4.1.4").condition(AddExpValueIs70MinutesInFutureToRequestObject::class.java)
			)
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-nbf-over-60-fails",
	displayName = "Chile CMF OP: ensure request object with nbf value over 60 minutes in the past fails",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithNbfOver60Fails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("OIDCC-6.1", "RFC7519-4.1.5").condition(ExpectRequestObjectWithNbfOver60ClaimErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.3", "PAR-2.1-3"),
			callback = expectAuthorizationError(invalidRequestUri())
		) {
			replace(
				AddNbfToRequestObject::class.java,
				stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-3").condition(AddNbfValueIs70MinutesInPastToRequestObject::class.java)
			)
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-signed-request-object-with-RS256-fails",
	displayName = "Chile CMF OP: ensure signed request object with RS256 fails",
	summary = "This test authenticates as normal except that the request object is signed using RS256. RS256 is not permitted by the FAPI specification. The test must end with the authorization server showing an an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureSignedRequestObjectWithRS256Fails : AbstractCMFChileOPTestModule() {

	override fun extraConfiguration(): TestExecutionUnit =
		Steps.action("Require an RSA client key") { env ->
			val alg = JWKUtil.getAlgFromClientJwks(env)
			if (alg != "PS256") { // FAPI only allows ES256 and PS256
				fireTestSkipped("This test requires RSA keys to be performed, the alg in client configuration is '%s' so this test is being skipped. If your server does not support PS256 then this will not prevent you certifying.".format(alg))
			}
		}

	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(ExpectSignedRS256RequestObjectErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2"),
			callback = expectAuthorizationError(invalidRequestObject())
		) {
			insertBefore(
				ConvertAuthorizationEndpointRequestToRequestObject::class.java,
				Steps.of(
					"Sign request object with RS256",
					Steps.action("Use a copy of the client JWKS for RS256") { env ->
						env.putObject("client_jwks_rs256", env.getObject("client_jwks").deepCopy())
						env.mapKey("client_jwks", "client_jwks_rs256")
					},
					stopOnFailure().requirements("FAPI2-SP-FINAL-5.4").condition(ChangeClientJwksAlgToRS256::class.java)
				)
			)
			insertAfter(SignRequestObject::class.java, unmapKey("Restore the client JWKS", "client_jwks"))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-signature-algorithm-is-not-none",
	displayName = "Chile CMF OP: ensure request object signature algorithm is not none",
	summary = "This test should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectSignatureAlgorithmIsNotNone : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(stopOnFailure().requirements("FAPI2-MS-ID1-5.4.2-2").condition(ExpectRequestObjectUnverifiableErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2"),
			callback = expectAuthorizationError(invalidRequestObject())
		) {
			insertBefore(ConvertAuthorizationEndpointRequestToRequestObject::class.java, exposeStateInAuthorizationEndpointRequest())
			replace(SignRequestObject::class.java, condition(SerializeRequestObjectWithNullAlgorithm::class.java))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-request-object-with-invalid-signature-fails",
	displayName = "Chile CMF OP: ensure request object with invalid signature fails",
	summary = "This test should end with the authorization server showing an error message that the signature of the request object is not verified (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureRequestObjectWithInvalidSignatureFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(condition(ExpectRequestObjectInvalidSignatureErrorPage::class.java)),
			parRejected = parInvalidRequestObject("JAR-6.2"),
			callback = expectAuthorizationError(invalidRequestObject())
		) {
			insertAfter(SignRequestObject::class.java, condition(InvalidateRequestObjectSignature::class.java))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-matching-key-in-authorization-request",
	displayName = "Chile CMF OP: ensure matching key in authorization request",
	summary = "This test sends a valid request object for client 1 but signed by client 2, and should end with the authorization server showing an error message that the request object is invalid (a screenshot of which should be uploaded), or with an error from the PAR endpoint, or with the user being redirected back to the conformance suite with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureMatchingKeyInAuthorizationRequest : AbstractCMFChileOPMultipleClientTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(
				stopOnFailure().requirements("FAPI2-MS-ID1-5.3.1-1", "OIDCC-6.3.2").condition(ExpectRequestObjectUnverifiableErrorPage::class.java)
			),
			parRejected = continueOnFailure().requirements("JAR-6.2").condition(EnsurePARInvalidRequestOrInvalidRequestObjectError::class.java),
			callback = expectAuthorizationError(invalidRequestObject())
		) {
			insertBefore(
				ConvertAuthorizationEndpointRequestToRequestObject::class.java,
				Steps.of(
					"Sign request object containing client_id for client 1 using JWK for client 2",
					mapKey("Use client 2's JWKS", "client_jwks", "client_jwks2"),
					exposeStateInAuthorizationEndpointRequest()
				)
			)
			insertAfter(SignRequestObject::class.java, unmapKey("Restore client 1's JWKS", "client_jwks"))
		}
	)
}

@PublishTestModule(
	testName = "cmf-chile-op-ensure-unsigned-request-at-par-endpoint-fails",
	displayName = "Chile CMF OP: ensure unsigned request at PAR endpoint fails",
	summary = "This test passes the request parameters to the PAR endpoint without them being wrapped in a signed request object, which should be rejected as signed request objects are required.\n\nThe test should end with the authorization server showing an error message that the request is invalid (a screenshot of which should be uploaded), or in an error from the PAR endpoint, or with the user being redirected back to the client's registered redirect_uri with a correct error response.",
	profile = CMF_OP_PROFILE
)
class CMFChileOPEnsureUnsignedRequestAtParEndpointFails : AbstractCMFChileOPTestModule() {
	override fun legs() = listOf(
		rejectedRequestLeg(
			errorPage = expectErrorPage(
				stopOnFailure().requirements("FAPI2-MS-ID1-5.3.2-1").condition(ExpectAuthorizationRequestWithoutRequestObjectErrorPage::class.java),
				"request_unverifiable_error"
			),
			parRejected = continueOnFailure().requirements("PAR-2.3").condition(EnsurePARInvalidRequestError::class.java),
			callback = expectAuthorizationError(
				continueOnFailure().requirements("OIDCC-3.1.2.6")
					.condition(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObjectOrInvalidRequestUri::class.java)
			),
			signed = false
		)
	)
}

/**
 * Also send state outside the request object, so an error returned to the redirect uri can carry it even when the
 * server cannot read the request object.
 */
private fun exposeStateInAuthorizationEndpointRequest(): TestExecutionUnit =
	Steps.action("Expose state in authorization endpoint request") { env ->
		env.putBoolean("expose_state_in_authorization_endpoint_request", true)
	}
