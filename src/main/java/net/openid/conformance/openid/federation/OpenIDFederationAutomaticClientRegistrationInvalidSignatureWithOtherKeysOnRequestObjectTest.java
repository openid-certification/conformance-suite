package net.openid.conformance.openid.federation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.common.ExpectInvalidRequestOrInvalidRequestObjectErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIAuthRequestMethod;
import org.springframework.http.HttpMethod;

@PublishTestModule(
	testName = "openid-federation-automatic-client-registration-invalid-signature-with-other-keys-on-request-object",
	displayName = "OpenID Federation OP test: Invalid signature (using other keys) on request object",
	summary = "The test acts as an RP wanting to perform automatic client registration with an OP, " +
		"with JAR and HTTP GET to the authorization endpoint. The request object is signed with another set of keys " +
		"than the one in the jwks, and must be rejected." +
		"<br/><br/>" +
		"If the server does not return an invalid_request, invalid_request_object or a similar well-defined " +
		"and appropriate error back to the client, it must show an error page saying the request is invalid due to " +
		"an invalid signature — upload a screenshot of the error page.",
	profile = "OIDFED"
)
@SuppressWarnings("unused")
public class OpenIDFederationAutomaticClientRegistrationInvalidSignatureWithOtherKeysOnRequestObjectTest extends OpenIDFederationAutomaticClientRegistrationTest {

	protected static String SOME_OTHER_JWKS = """
		{
		   "keys": [
			   {
				   "p": "0F9fYePZVmm996Pt64j4owTSqBvJtuFw9dhufE-Xtl-4W1tKc_IlJgMpr-g7jAazRSkHQjOIaMp2ht88ZGfE1pd2II0XPZDxdhrpoeEWIfWabG2nzQVBPHxkZxGtnb5meQsCTDzKoorUwx7UKGBmWtHHPa_aDlf6U8kllgdZhJE",
				   "kty": "RSA",
				   "q": "pxWqBMgmLG6BW5ugZZTditN4nByvl_64TPrJFjj9Z4dmt-Oa8CyRlDBlRkwUjzqHfmBmvw9GXGpXpiAEcEBbOuC-MWuXs-5Gh0Bi1zNeNuroKL9oYy6i1BXVa7sKc3MnWQPm7Wbyey1gp-RQXbe-CQ-nFhhuuvssW9JJLJg5O4M",
				   "d": "UXoPaumj0MDsViRtW_RGPaPxSnW1KYJGjcN0nTbNBwP-o52IVVl05kLp8xDOadJzlt60QWosoomDqGFmVnbLWEigmjKSBCLi_pjNEPvopKA_yQYSXfnEqa0jylWU49UN0-3vu12gn4bKz5AmF39Iv6paTUNKMKt9_1qwMDY7A_Xi3GGzni1n9mOd8yt9mBd1_bjNLJIZTneRRz4RyieQO9HapvJkpiCn6Stksu6tLBy38enzIh9ZLwDaWLkBa4gw0i9w5coVo8c01OPRN5cjxqId_EKntuOT8qGyNaWmMEBiVimEi4Zt-EaibwV2JaHiD44AdGU_I0QgVLqaCikK4Q",
				   "e": "AQAB",
				   "use": "sig",
				   "kid": "mzTAZxDRQ0eGoyXfe5444t7R37J-vBNsvAHLapvq5_w",
				   "qi": "jkijajIZZX2r9r5VDWxQZV-BEU1Xpv61nOsirIGWF-v8tvbaDvhx-FxmDf4QZaDx7rIT8Okjy7bETg7UnQY6bSuQpVQxocsMKd3o7v7xBUfE4YI3Ioxktu_M2-z0sY-9sLyB1VKaY4yC2wbs-ZR86krMwKfASOlvqHK579TezmM",
				   "dp": "osDrqnUijAlZ1PSY4DEDK6mXtLDHtG2QIqZjXft5H3zSbaD3nWqUM4Wi6P_2it-s5KcwQTgpItiJDb2N2UroB0kUfkZV0cPFph9NPyClfe7tKCXaPBwPoWPztEGtnxlZxs-fgta025Kerq0Ev2jUabmS6w7QvgVfxFPHZNDmzdE",
				   "alg": "RS256",
				   "dq": "VEhuWjkoYHNh13caowND-fqQtZcW0IJSL6q18vrOgQHhQ9ORXaPtz6XWAG7yVAm_PXWdfrVS1d1i108BTip4mqux5TYT8VS5yRImic3EsckvURa0rotg4cEdxlk0fWZSK-n2_DxZBt-uCSL3kz7Torh9nXy3pqR7DNp3xQIIpMc",
				   "n": "h__Zbuhoi9_tlL-wTOVkgd6Ex1O1o6iADkDJWJko1ZsyTN-dOclc506aAWMfR_s8iPa-ArBot820eFespU5JNw_15jAbYEnoFK43PLcfPc9mz62o54SNTPtfrfWnbTs_CVrXwnwhVqRp9FkncB9nDpEPSYZer2O3fRSHZyhjHs5jAVjeUnG3-4MT_rxSRj-wPFgXRfI8eRVkfL4pRHZiKHd8GDth62ipe45FDfAXdxkiIpLsFy9Dh5XRaZr99XHq83gd-ZA_1eB1-s--VuP0ZnctMFqC2VQAVmG7bV_KPacOqpxiK0XIBFrPIG4weqKu4nEmiq_J197CfrsIAKVBMw"
			   }
		   ]
		}""";

	@Override
	protected FAPIAuthRequestMethod getRequestMethod() {
		return FAPIAuthRequestMethod.BY_VALUE;
	}

	@Override
	protected HttpMethod getHttpMethodForAuthorizeRequest() {
		return HttpMethod.GET;
	}

	@Override
	protected void signRequestObject() {
		env.putObjectFromJsonString("some_other_jwks", SOME_OTHER_JWKS);
		env.mapKey("client_jwks", "some_other_jwks");
		callAndContinueOnFailure(SignRequestObject.class, Condition.ConditionResult.FAILURE);
		env.unmapKey("client_jwks");
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectInvalidRequestOrInvalidRequestObjectErrorPage.class, Condition.ConditionResult.FAILURE);
		env.putString("error_callback_placeholder", env.getString("invalid_request_error"));
	}

	@Override
	protected void redirect(HttpMethod method) {
		performRedirectAndWaitForPlaceholdersOrCallback("error_callback_placeholder", method.name());
	}

	@Override
	protected void processCallback() {
		env.mapKey("authorization_endpoint_response", "callback_query_params");
		performGenericAuthorizationEndpointErrorResponseValidation();
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject.class, Condition.ConditionResult.WARNING);
		fireTestFinished();
	}

}
