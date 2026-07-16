package net.openid.conformance.fapiciba.rp;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.as.FAPIBrazilSetRequiredIdTokenEncryptionConfig;
import net.openid.conformance.condition.as.FetchClientKeys;
import net.openid.conformance.condition.as.GenerateRegistrationAccessToken;
import net.openid.conformance.condition.as.dynregistration.EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet;
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
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSSASignature;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSoftwareStatementIat;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateUserinfoSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.FAPICIBAEnsureRegistrationRequestContainsCibaGrantType;
import net.openid.conformance.condition.as.dynregistration.FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps;
import net.openid.conformance.condition.as.dynregistration.FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256;
import net.openid.conformance.condition.as.dynregistration.FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse;
import net.openid.conformance.condition.as.dynregistration.FAPICIBAEnsureRegistrationRequestUsesPingMode;
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
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.condition.common.CreateRandomRegistrationClientUri;
import net.openid.conformance.condition.common.EnsureIncomingTls12WithSecureCipherOrTls13;
import net.openid.conformance.condition.common.EnsureIncomingTls13;
import net.openid.conformance.condition.rs.ExtractBearerAccessTokenFromHeader;
import net.openid.conformance.condition.rs.RequireBearerRegistrationAccessToken;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.variant.CIBAMode;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-brazildcr-happypath-test",
	displayName = "FAPI-CIBA-ID1: Open Finance Brazil client DCR happy path test",
	summary = "Tests that an Open Finance Brazil CIBA client dynamically registers over mTLS using a valid "
		+ "Directory software statement, then completes the normal Data Consent, CIBA ping, token, encrypted "
		+ "ID Token, and resources flow.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.jwks"
	}
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = { "plain_fapi", "openbanking_uk", "connectid_au" })
@VariantNotApplicable(parameter = ClientAuthType.class, values = { "mtls" })
@VariantNotApplicable(parameter = CIBAMode.class, values = { "poll" })
public class FAPICIBAClientBrazilDCRHappyPathTest extends AbstractFAPICIBAClientTest {

	@Override
	protected void configureClient() {
		// The client is supplied by dynamic registration after configuration completes.
	}

	@Override
	protected boolean shouldValidateConfiguredNotificationEndpoint() {
		return false;
	}

	@Override
	protected void onConfigurationCompleted() {
		callAndStopOnFailure(AddDynamicRegistrationEndpointsToServerConfiguration.class);
		callAndStopOnFailure(FAPIBrazilCheckDirectoryKeystore.class);
	}

	@Override
	public Object handleHttpMtls(String path, HttpServletRequest req, HttpServletResponse res,
			HttpSession session, JsonObject requestParts) {
		if ("register".equals(path)) {
			return handleRegistrationEndpointRequest(storeIncomingRequestAndCheckTls(requestParts));
		}
		String registrationClientPath = env.getString("registration_client_uri", "path");
		if (registrationClientPath != null && registrationClientPath.equals(path)) {
			if (isCleanupDelete(requestParts)) {
				return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
			}
			return handleRegistrationClientUriRequest(requestParts);
		}
		return super.handleHttpMtls(path, req, res, session, requestParts);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res,
			HttpSession session, JsonObject requestParts) {
		if ("register".equals(path)) {
			setStatus(Status.RUNNING);
			throw new TestFailureException(getId(),
				"Open Finance Brazil dynamic client registration must use the mTLS registration endpoint");
		}
		String registrationClientPath = env.getString("registration_client_uri", "path");
		if (registrationClientPath != null && registrationClientPath.equals(path)
			&& isCleanupDelete(requestParts)) {
			return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
		}
		return super.handleHttp(path, req, res, session, requestParts);
	}

	private boolean isCleanupDelete(JsonObject requestParts) {
		return "DELETE".equals(OIDFJSON.getString(requestParts.get("method")));
	}

	private String storeIncomingRequestAndCheckTls(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		call(exec().mapKey("client_request", requestId));
		callAndContinueOnFailure(EnsureIncomingTls12WithSecureCipherOrTls13.class,
			Condition.ConditionResult.WARNING, "FAPI1-BASE-7.1", "FAPI1-ADV-8.5-1");
		callAndContinueOnFailure(EnsureIncomingTls13.class,
			Condition.ConditionResult.WARNING, "RFC9325-3.1.1");
		call(exec().unmapKey("client_request"));
		return requestId;
	}

	private Object handleRegistrationEndpointRequest(String requestId) {
		call(exec().startBlock("Dynamic registration endpoint").mapKey("incoming_request", requestId));
		callAndStopOnFailure(OIDCCExtractDynamicRegistrationRequest.class);
		validateRegistrationCertificate(requestId);
		validateSoftwareStatement();

		env.mapKey("client", "dynamic_registration_request");
		validateClientRegistrationMetadata();
		callAndStopOnFailure(FAPIBrazilSetRequiredIdTokenEncryptionConfig.class,
			"BrazilOB-5.1.1-1");
		env.unmapKey("client");

		validateBrazilRegistrationMetadata();
		validateCibaRegistrationMetadata();

		JsonObject registeredClient = registerClient().deepCopy();
		callAndStopOnFailure(FetchClientKeys.class);
		validateClientJwks();
		validateClientConfiguration();

		call(exec().unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>(registeredClient, HttpStatus.CREATED);
	}

	private void validateRegistrationCertificate(String requestId) {
		call(exec().mapKey("token_endpoint_request", requestId));
		JsonObject configuredClient = new JsonObject();
		configuredClient.addProperty("certificate", env.getString("config", "client.certificate"));
		env.putObject("client", configuredClient);
		checkMtlsCertificate();
		env.removeObject("client");
		call(exec().unmapKey("token_endpoint_request"));
	}

	private void validateSoftwareStatement() {
		callAndStopOnFailure(FAPIBrazilExtractSSAFromDynamicRegistrationRequest.class);
		callAndStopOnFailure(FAPIBrazilFetchDirectorySSAJwks.class);
		callAndStopOnFailure(FAPIBrazilValidateSSASignature.class);
		callAndStopOnFailure(FAPIBrazilExtractSoftwareStatement.class);
	}

	private void validateClientRegistrationMetadata() {
		callAndContinueOnFailure(ValidateClientGrantTypes.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(OIDCCValidateClientRedirectUris.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientLogoUris.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientUris.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientPolicyUris.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientTosUris.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		callAndContinueOnFailure(ValidateClientSubjectType.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");

		skipIfElementMissing("client", "id_token_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateIdTokenSignedResponseAlg.class, Condition.ConditionResult.FAILURE,
			"BrazilOB-6.2");
		callAndContinueOnFailure(EnsureIdTokenEncryptedResponseAlgIsSetIfEncIsSet.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			ValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "userinfo_signed_response_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateUserinfoSignedResponseAlg.class, Condition.ConditionResult.FAILURE,
			"BrazilOB-6.2");
		callAndContinueOnFailure(EnsureUserinfoEncryptedResponseAlgIsSetIfEncIsSet.class,
			Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "token_endpoint_auth_signing_alg", Condition.ConditionResult.INFO,
			FAPIBrazilValidateTokenEndpointAuthSigningAlg.class, Condition.ConditionResult.FAILURE,
			"OIDCR-2");
		callAndContinueOnFailure(ValidateDefaultMaxAge.class,
			Condition.ConditionResult.WARNING, "OIDCR-2");
		skipIfElementMissing("client", "require_auth_time", Condition.ConditionResult.INFO,
			ValidateRequireAuthTime.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "default_acr_values", Condition.ConditionResult.INFO,
			FAPIBrazilValidateDefaultAcrValues.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
		skipIfElementMissing("client", "initiate_login_uri", Condition.ConditionResult.INFO,
			ValidateInitiateLoginUri.class, Condition.ConditionResult.FAILURE, "OIDCR-2");
	}

	private void validateBrazilRegistrationMetadata() {
		callAndStopOnFailure(FAPIBrazilValidateSoftwareStatementIat.class, "BrazilOBDCR-7.1-3");
		callAndStopOnFailure(FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks.class,
			"BrazilOBDCR-7.1-4");
		callAndStopOnFailure(FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri.class,
			"BrazilOBDCR-7.1-5");
		callAndStopOnFailure(FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris.class,
			"BrazilOBDCR-7.1-6");
		callAndStopOnFailure(FAPIBrazilValidateClientAuthenticationMethods.class,
			"BrazilOBDCR-7.1-7");
		callAndStopOnFailure(FAPIBrazilEnsureClientMetadataMatchSoftwareStatement.class,
			"BrazilOBDCR-7.1-10");
		callAndStopOnFailure(FAPIBrazilEnsureTlsClientAuthSubjectDnOnly.class,
			"BrazilOBDCR-7.1-7");
	}

	private void validateCibaRegistrationMetadata() {
		callAndStopOnFailure(FAPICIBAEnsureRegistrationRequestContainsCibaGrantType.class, "CIBA-4");
		callAndStopOnFailure(FAPICIBAEnsureRegistrationRequestUsesPingMode.class,
			"BrazilCIBA-6.2.2");
		callAndStopOnFailure(FAPICIBAEnsureRegistrationRequestNotificationEndpointIsHttps.class,
			"CIBA-4");
		callAndStopOnFailure(FAPICIBAEnsureRegistrationRequestSigningAlgIsPS256.class,
			"BrazilCIBA-6.2.4");
		callAndStopOnFailure(FAPICIBAEnsureRegistrationRequestUserCodeIsAbsentOrFalse.class,
			"BrazilCIBA-6.2.4");
	}

	private JsonObject registerClient() {
		callAndStopOnFailure(GenerateRegistrationAccessToken.class, "RFC7592-3");
		callAndStopOnFailure(CreateRandomRegistrationClientUri.class, "RFC7592A-B");
		callAndStopOnFailure(FAPIBrazilRegisterClient.class);
		return env.getObject("client");
	}

	private Object handleRegistrationClientUriRequest(JsonObject requestParts) {
		setStatus(Status.RUNNING);
		if (!"DELETE".equals(OIDFJSON.getString(requestParts.get("method")))) {
			throw new TestFailureException(getId(),
				"Only cleanup DELETE is supported at the registration client URI in this test");
		}

		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, requestParts);
		call(exec().startBlock("Registration client URI cleanup")
			.mapKey("incoming_request", requestId)
			.mapKey("token_endpoint_request", requestId));
		checkMtlsCertificate();
		callAndStopOnFailure(ExtractBearerAccessTokenFromHeader.class, "RFC7592-2.1");
		callAndStopOnFailure(RequireBearerRegistrationAccessToken.class, "RFC7592-2.1");
		call(exec().unmapKey("token_endpoint_request").unmapKey("incoming_request").endBlock());
		setStatus(Status.WAITING);
		return new ResponseEntity<Object>("", HttpStatus.NO_CONTENT);
	}
}
