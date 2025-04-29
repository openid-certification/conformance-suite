package net.openid.conformance.fapi2spid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.EnsureRedirectUriInRequestObjectMatchesOneOfClientRedirectUris;
import net.openid.conformance.condition.as.FetchClientKeys;
import net.openid.conformance.condition.as.GenerateRegistrationAccessToken;
import net.openid.conformance.condition.as.ValidateRedirectUriForTokenEndpointRequest;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureClientMetadataMatchSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureTlsClientAuthSubjectDnOnly;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSSAFromDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilFetchDirectorySSAJwks;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilRegisterClient;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateClientAuthenticationMethods;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateDefaultAcrValues;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateRegistrationClientUriQueryParams;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSSASignature;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSoftwareStatementIat;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.OIDCCExtractDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.OIDCCValidateClientRedirectUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientGrantTypes;
import net.openid.conformance.condition.as.dynregistration.ValidateClientLogoUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientPolicyUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientSubjectType;
import net.openid.conformance.condition.as.dynregistration.ValidateClientTosUris;
import net.openid.conformance.condition.as.dynregistration.ValidateClientUris;
import net.openid.conformance.condition.as.dynregistration.ValidateDefaultMaxAge;
import net.openid.conformance.condition.as.dynregistration.ValidateInitiateLoginUri;
import net.openid.conformance.condition.as.dynregistration.ValidateRequireAuthTime;
import net.openid.conformance.condition.as.dynregistration.ValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.common.CreateRandomRegistrationClientUri;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.RequireBearerRegistrationAccessToken;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-client-brazildcr-happypath-test",
	displayName = "FAPI2-Security-Profile-ID2: client DCR happy path test",
	summary = """
		Tests a 'happy path' flow; \
		first perform OpenID discovery from the displayed discoveryUrl, and (after obtaining a software statement from the directory) register the client. \
		Then call the authorization endpoint (which will immediately redirect back), \
		exchange the authorization code for an access token at the token endpoint and \
		make a GET request to the accounts/payments endpoint displayed.

		Finally, the client must make two \
		a GET calls to the RFC7592 Client Configuration Endpoint; a new registration access token is returned \
		each time - it is vital that the client PERMANENTLY \
		stores the registration_client_uri and registration_access_token so that future changes may be \
		made to the configuration of the client.

		If you do not have access to the directory you can use the keys in this configuration: https://gitlab.com/openid/conformance-suite/-/blob/master/scripts/test-configs-rp-against-op/fapi-brazil-rp-test-config-payments.json\
		""",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.jwks",
		"waitTimeoutSeconds"
	}
)

public class FAPI2SPID2BrazilClientDCRHappyPathTest extends AbstractFAPI2SPID2ClientTest {
	boolean resourceEndpointCalled = false;
	boolean clientConfigEndpointCalled = false;

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

	@Override
	protected void configureClients() {
		//do nothing, the client needs to register first
	}

	@Override
	protected void resourceEndpointCallComplete() {
		resourceEndpointCalled = true;
		setStatus(Status.WAITING);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if(path.equals("register")) {
			setStatus(Status.RUNNING);

			String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

			env.putObject(requestId, requestParts);

			call(exec().mapKey("client_request", requestId));

			callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING, "FAPI2-SP-ID2-5.2.1-1", "FAPI2-SP-ID2-5.2.1-2");

			call(exec().unmapKey("client_request"));

			setStatus(Status.WAITING);

			return handleRegistrationEndpointRequest(requestId);
		} else if (path.equals(env.getString("registration_client_uri", "path"))) {
			if (OIDFJSON.getString(requestParts.get("method")).equals("DELETE")) {
				// We ignore this to keep the OP tests against RP tests happy,
				// fapi2-security-profile-id2-brazildcr-happy-flow calls DELETE as it's final step.
				return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
			}

			setStatus(Status.RUNNING);

			String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);

			env.putObject(requestId, requestParts);

			return handleRegistrationClientUriRequest(requestId);

		} else {
			return super.handleHttpMtls(path, req, res, session, requestParts);
		}
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {

		if(path.equals("register")) {
			setStatus(Status.RUNNING);
			throw new TestFailureException(getId(), "As per https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-dynamic-client-registration-1_ID1.html#name-authorization-server-2, " +
				" dynamic client registration requests must be performed over a connection secured with " +
				"mutual tls using certificates issued by Brazil ICP (production) or the Directory of Participants (sandbox)");
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}

	}

	protected Object handleRegistrationClientUriRequest(String requestId) {
		if (!resourceEndpointCalled) {
			throw new TestFailureException(getId(), "Please call the accounts/payment endpoint before the Client Configuration Endpoint.");
		}
		call(exec().startBlock("Registration endpoint").mapKey("incoming_request", requestId));
		call(exec().mapKey("client_request", requestId));

		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class, Condition.ConditionResult.WARNING, "FAPI2-SP-ID2-5.2.1-1", "FAPI2-SP-ID2-5.2.1-2");
		callAndContinueOnFailure(FAPIBrazilValidateRegistrationClientUriQueryParams.class, Condition.ConditionResult.WARNING, "OIDCR-3.2", "OIDCR-4.1");


		call(exec().unmapKey("client_request"));

		call(exec().mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		call(exec().unmapKey("token_endpoint_request"));

		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "RFC7592-2.1");
		callAndContinueOnFailure(RequireBearerRegistrationAccessToken.class, Condition.ConditionResult.WARNING, "RFC7592-2.1");

		call(exec().unmapKey("incoming_request").endBlock());

		JsonObject clientInfo = env.getObject("client");

		// rotate registration access token
		callAndStopOnFailure(GenerateRegistrationAccessToken.class, "RFC7592-3");
		clientInfo.addProperty("registration_access_token", env.getString("registration_access_token"));

		if (clientConfigEndpointCalled) {
			fireTestFinished();
		} else {
			clientConfigEndpointCalled = true;
			setStatus(Status.WAITING);
		}

		return new ResponseEntity<Object>(clientInfo, HttpStatus.OK);
	}

	protected Object handleRegistrationEndpointRequest(String requestId) {
		setStatus(Status.RUNNING);
		call(exec().startBlock("Registration endpoint").mapKey("incoming_request", requestId));


		callAndStopOnFailure(OIDCCExtractDynamicRegistrationRequest.class);

		call(exec().mapKey("token_endpoint_request", requestId));
		//adding and removing a fake client object to env to make EnsureClientCertificateMatches
		// (called in checkMtlsCertificate) work
		JsonObject fakeClientObject = new JsonObject();
		fakeClientObject.addProperty("certificate", env.getString("config", "client.certificate"));
		env.putObject("client", fakeClientObject);
		//always requiring the configured certificate for client authentication and
		// not performing any additional verification
		checkMtlsCertificate();
		env.removeObject("client");
		call(exec().unmapKey("token_endpoint_request"));

		callAndStopOnFailure(FAPIBrazilExtractSSAFromDynamicRegistrationRequest.class);
		callAndStopOnFailure(FAPIBrazilFetchDirectorySSAJwks.class);
		callAndStopOnFailure(FAPIBrazilValidateSSASignature.class);
		callAndStopOnFailure(FAPIBrazilExtractSoftwareStatement.class);

		env.mapKey("client", "dynamic_registration_request");
		validateClientRegistrationMetadata();
		env.unmapKey("client");

		validateClientRegistrationBrazilSpecificChecks();

		JsonObject registeredClient = registerClient().deepCopy();

		//Note that we don't want to include the jwks in the returned response, that's why we have the deepCopy above
		callAndStopOnFailure(FetchClientKeys.class);
		validateClientJwks(false);

		call(exec().unmapKey("incoming_request").endBlock());

		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(registeredClient, HttpStatus.CREATED);
	}

	protected void validateClientRegistrationBrazilSpecificChecks() {
		//BrazilOBDCR- 7.1-3 shall validate that the software_statement was issued (iat) not more than 5 minutes prior to the request being received
		callAndContinueOnFailure(FAPIBrazilValidateSoftwareStatementIat.class, Condition.ConditionResult.FAILURE,"BrazilOBDCR-7.1-3");
		//BrazilOBDCR- 7.1-4 shall validate that a jwks (key set by value) was not included;
		callAndContinueOnFailure(FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks.class, Condition.ConditionResult.FAILURE,"BrazilOBDCR-7.1-4");
		//BrazilOBDCR- 7.1-5 shall require and validate that the jwks_uri matches the software_jwks_uri provided in the software statement;
		callAndContinueOnFailure(FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri.class, Condition.ConditionResult.FAILURE,"BrazilOBDCR-7.1-5");
		//BrazilOBDCR- 7.1-6 shall require and validate that redirect_uris match or contain a sub set of softwareredirecturis provided in the software statement;
		callAndContinueOnFailure(FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris.class, Condition.ConditionResult.FAILURE,"BrazilOBDCR-7.1-6");
		//BrazilOBDCR- 7.1-7 shall require and validate that all client authentication mechanism adhere to the requirements defined in Financial-grade API Security Profile 1.0 - Part 1: Advanced;
		callAndContinueOnFailure(FAPIBrazilValidateClientAuthenticationMethods.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-7");

		//TODO how do you validate this during registration? registration request does not contain scopes?
		//BrazilOBDCR- 7.1-9 shall shall validate that requested scopes are appropriate for the softwares authorized regulatory roles
		//software_statement_roles -> "role": "DADOS",

		//BrazilOBDCR- 7.1-10 should where possible validate client asserted metadata against metadata provided in the software_statement;
		callAndContinueOnFailure(FAPIBrazilEnsureClientMetadataMatchSoftwareStatement.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-10");

		//BrazilOBDCR- 7.1-11 shall accept all x.500 AttributeType name strings defined in the Distinguished Name of the
		// x.509 Certificate Profiles defined in Open Banking Brasil x.509 Certificate Standards;

		//BrazilOBDCR- 7.1-12 if supporting tls_client_auth client authentication mechanism as defined in RFC8705 shall
		// only accept tls_client_auth_subject_dn as an indication of the certificate subject value as defined in clause 2.1.2 RFC8705;
		callAndContinueOnFailure(FAPIBrazilEnsureTlsClientAuthSubjectDnOnly.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1-7");
	}

	protected void validateClientRegistrationMetadata(){
		//check response type - grant type consistency
		callAndContinueOnFailure(ValidateClientGrantTypes.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		//basic checks like fragments, https etc
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//check if logo is image
		callAndContinueOnFailure(ValidateClientLogoUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientPolicyUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");
		//check if uri is valid
		callAndContinueOnFailure(ValidateClientTosUris.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		callAndContinueOnFailure(ValidateClientSubjectType.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		skipIfElementMissing("client", "id_token_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateIdTokenSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//userinfo
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			ValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "BrazilOB-6.1");

		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");

		//request object
		skipIfElementMissing("client", "request_object_signing_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateRequestObjectSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(EnsureRequestObjectEncryptionAlgIsSetIfEncIsSet.class, Condition.ConditionResult.FAILURE,"OIDCR-2");


		skipIfElementMissing("client", "token_endpoint_auth_signing_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateTokenEndpointAuthSigningAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		callAndContinueOnFailure(ValidateDefaultMaxAge.class, Condition.ConditionResult.WARNING,"OIDCR-2");

		skipIfElementMissing("client", "require_auth_time", Condition.ConditionResult.INFO,
			ValidateRequireAuthTime.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "default_acr_values", Condition.ConditionResult.INFO,
			FAPIBrazilValidateDefaultAcrValues.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "initiate_login_uri", Condition.ConditionResult.INFO,
			ValidateInitiateLoginUri.class, Condition.ConditionResult.FAILURE, "OIDCR-2");

		//TODO not allow request_uris?
		/*
		skipIfElementMissing("client", "request_uris", Condition.ConditionResult.INFO,
			ValidateRequestUris.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		 */
	}

	protected JsonObject registerClient() {
		callAndStopOnFailure(GenerateRegistrationAccessToken.class, "RFC7592-3");
		callAndStopOnFailure(CreateRandomRegistrationClientUri.class, "RFC7592A-B");
		callAndStopOnFailure(FAPIBrazilRegisterClient.class);
		JsonObject client = env.getObject("client");
		return client;
	}

	@Override
	protected void validateRedirectUriInRequestObject() {
		callAndContinueOnFailure(EnsureRedirectUriInRequestObjectMatchesOneOfClientRedirectUris.class, Condition.ConditionResult.FAILURE);
	}

	@Override
	protected void validateRedirectUriForAuthorizationCodeGrantType() {
		callAndStopOnFailure(ValidateRedirectUriForTokenEndpointRequest.class);
	}
}
