package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureClientMetadataMatchSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilEnsureTlsClientAuthSubjectDnOnly;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSSAFromDynamicRegistrationRequest;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilExtractSoftwareStatement;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilFetchDirectorySSAJwks;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateClientAuthenticationMethods;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateIdTokenSignedResponseAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateRequestObjectSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSSASignature;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateSoftwareStatementIat;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateTokenEndpointAuthSigningAlg;
import net.openid.conformance.condition.as.dynregistration.FAPIBrazilValidateUserinfoSignedResponseAlg;
import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.AddClaimsParameterSupportedTrueToServerConfiguration;
import net.openid.conformance.condition.as.CreateFapiInteractionIdIfNeeded;
import net.openid.conformance.condition.as.EnsureScopeContainsAccounts;
import net.openid.conformance.condition.as.EnsureScopeContainsPayments;
import net.openid.conformance.condition.as.FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddCPFAndCPNJToUserInfoClaims;
import net.openid.conformance.condition.as.FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer;
import net.openid.conformance.condition.as.FAPIBrazilChangeConsentStatusToAuthorized;
import net.openid.conformance.condition.as.FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant;
import net.openid.conformance.condition.as.FAPIBrazilOBAddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.FAPIBrazilSetGrantTypesSupportedInServerConfiguration;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentConsentResponse;
import net.openid.conformance.condition.as.FAPIBrazilSignPaymentInitiationResponse;
import net.openid.conformance.condition.as.FAPIBrazilValidateConsentScope;
import net.openid.conformance.condition.as.SetServerSigningAlgToPS256;
import net.openid.conformance.condition.client.FAPIBrazilValidateRequestObjectIdTokenACRClaims;
import net.openid.conformance.condition.rs.ClearAccessTokenFromRequest;
import net.openid.conformance.condition.rs.EnsureIncomingRequestContentTypeIsApplicationJwt;
import net.openid.conformance.condition.rs.EnsureIncomingRequestMethodIsPost;
import net.openid.conformance.condition.rs.ExtractXIdempotencyKeyHeader;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureAuthorizationRequestScopesContainPayments;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureClientCredentialsScopeContainedConsents;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureClientCredentialsScopeContainedPayments;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId;
import net.openid.conformance.condition.rs.FAPIBrazilEnsureConsentRequestJtiIsUUIDv4;
import net.openid.conformance.condition.rs.FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId;
import net.openid.conformance.condition.rs.FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4;
import net.openid.conformance.condition.rs.FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate;
import net.openid.conformance.condition.rs.FAPIBrazilExtractCertificateSubjectFromServerJwks;
import net.openid.conformance.condition.as.FAPIBrazilExtractConsentRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentInitiationRequest;
import net.openid.conformance.condition.as.FAPIBrazilExtractPaymentsConsentRequest;
import net.openid.conformance.condition.rs.FAPIBrazilFetchClientOrganizationJwksFromDirectory;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateGetConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateGetPaymentConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewPaymentInitiationResponse;
import net.openid.conformance.condition.rs.FAPIBrazilGenerateNewPaymentsConsentResponse;
import net.openid.conformance.condition.rs.FAPIBrazilRsPathConstants;
import net.openid.conformance.condition.rs.FAPIBrazilValidateConsentRequestIat;
import net.openid.conformance.condition.rs.FAPIBrazilValidateJwtSignatureUsingOrganizationJwks;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentConsentRequestAud;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentInitiationRequestAud;
import net.openid.conformance.condition.rs.FAPIBrazilValidatePaymentInitiationRequestIat;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.as.GenerateOpenBankingBrazilAccountsEndpointResponse;
import net.openid.conformance.testmodule.DataUtils;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule.Status;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Profile behavior for OpenBanking Brazil client tests.
 * Requires mTLS everywhere; uses Brazil-specific server config (PS256, claims_supported,
 * Brazil grant types, Brazil settings); exposes Brazil mtls paths (accounts, consents,
 * payments-consents, payment-initiation); validates consent scope and CPF/CPNJ claims;
 * implements the Brazil-specific consent / payments / payment-initiation endpoints called
 * over mTLS.
 */
public class OpenBankingBrazilClientProfileBehavior extends FAPI2ClientProfileBehavior implements DataUtils {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public ConditionSequence addProfileSpecificServerConfiguration() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetServerSigningAlgToPS256.class, "BrazilOB-6.1");
				// FIXME - BrazilOB-5.2.3-5 seems incorrect, but no obvious replacement.
				callAndStopOnFailure(FAPIBrazilSetGrantTypesSupportedInServerConfiguration.class);
				callAndStopOnFailure(AddClaimsParameterSupportedTrueToServerConfiguration.class, "BrazilOB-5.2.2-4");
				callAndStopOnFailure(FAPIBrazilAddBrazilSpecificSettingsToServerConfiguration.class, "BrazilOB-5.2.2");
			}
		};
	}

	@Override
	public ConditionSequence validateSoftwareStatement() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilExtractSSAFromDynamicRegistrationRequest.class);
				callAndStopOnFailure(FAPIBrazilFetchDirectorySSAJwks.class);
				callAndStopOnFailure(FAPIBrazilValidateSSASignature.class);
				callAndStopOnFailure(FAPIBrazilExtractSoftwareStatement.class);
			}
		};
	}

	@Override
	public ConditionSequence validateDynamicRegistrationRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				//BrazilOBDCR- 7.1-3 shall validate that the software_statement was issued (iat) not more than 5 minutes prior to the request being received
				callAndContinueOnFailure(FAPIBrazilValidateSoftwareStatementIat.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-3");
				//BrazilOBDCR- 7.1-4 shall validate that a jwks (key set by value) was not included;
				callAndContinueOnFailure(FAPIBrazilEnsureRegistrationRequestDoesNotIncludeJwks.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-4");
				//BrazilOBDCR- 7.1-5 shall require and validate that the jwks_uri matches the software_jwks_uri provided in the software statement;
				callAndContinueOnFailure(FAPIBrazilEnsureJwksUriMatchesSoftwareJwksUri.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-5");
				//BrazilOBDCR- 7.1-6 shall require and validate that redirect_uris match or contain a sub set of softwareredirecturis provided in the software statement;
				callAndContinueOnFailure(FAPIBrazilEnsureRedirectUrisMatchSoftwareRedirectUris.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-6");
				//BrazilOBDCR- 7.1-7 shall require and validate that all client authentication mechanism adhere to the requirements defined in Financial-grade API Security Profile 1.0 - Part 1: Advanced;
				callAndContinueOnFailure(FAPIBrazilValidateClientAuthenticationMethods.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-7");

				//TODO how do you validate this during registration? registration request does not contain scopes?
				//BrazilOBDCR- 7.1-9 shall shall validate that requested scopes are appropriate for the softwares authorized regulatory roles
				//software_statement_roles -> "role": "DADOS",

				//BrazilOBDCR- 7.1-10 should where possible validate client asserted metadata against metadata provided in the software_statement;
				callAndContinueOnFailure(FAPIBrazilEnsureClientMetadataMatchSoftwareStatement.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-10");

				//BrazilOBDCR- 7.1-11 shall accept all x.500 AttributeType name strings defined in the Distinguished Name of the
				// x.509 Certificate Profiles defined in Open Banking Brasil x.509 Certificate Standards;

				//BrazilOBDCR- 7.1-12 if supporting tls_client_auth client authentication mechanism as defined in RFC8705 shall
				// only accept tls_client_auth_subject_dn as an indication of the certificate subject value as defined in clause 2.1.2 RFC8705;
				callAndContinueOnFailure(FAPIBrazilEnsureTlsClientAuthSubjectDnOnly.class, ConditionResult.FAILURE, "BrazilOBDCR-7.1-7");
			}
		};
	}

	@Override
	public ConditionSequence validateDynamicRegistrationSigningAlgs() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateIdTokenSignedResponseAlg.class)
					.skipIfElementMissing("client", "id_token_signed_response_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("BrazilOB-6.1"));
				call(condition(FAPIBrazilValidateUserinfoSignedResponseAlg.class)
					.skipIfElementMissing("client", "userinfo_signed_response_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("BrazilOB-6.1"));
				call(condition(FAPIBrazilValidateRequestObjectSigningAlg.class)
					.skipIfElementMissing("client", "request_object_signing_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("OIDCR-2"));
				call(condition(FAPIBrazilValidateTokenEndpointAuthSigningAlg.class)
					.skipIfElementMissing("client", "token_endpoint_auth_signing_alg")
					.onSkip(ConditionResult.INFO).onFail(ConditionResult.FAILURE).dontStopOnFailure()
					.requirement("OIDCR-2"));
			}
		};
	}

	@Override
	public String dynamicRegistrationRequiresMtlsMessage() {
		return "As per https://openbanking-brasil.github.io/specs-seguranca/open-banking-brasil-dynamic-client-registration-1_ID1.html#name-authorization-server-2, " +
			" dynamic client registration requests must be performed over a connection secured with " +
			"mutual tls using certificates issued by Brazil ICP (production) or the Directory of Participants (sandbox)";
	}

	@Override
	public Class<? extends Condition> getTokenEndpointAuthSigningAlgsCondition() {
		return FAPIBrazilAddTokenEndpointAuthSigningAlgValuesSupportedToServer.class;
	}

	@Override
	public void exposeProfileEndpoints() {
		module.exposeMtlsPath("accounts_endpoint", FAPIBrazilRsPathConstants.BRAZIL_ACCOUNTS_PATH);
		module.exposeMtlsPath("consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH);
		module.exposeMtlsPath("payments_consents_endpoint", FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH);
		module.exposeMtlsPath("payment_initiation_path", FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH);
	}

	@Override
	public Class<? extends ConditionSequence> getAccountsEndpointProfileSteps() {
		return GenerateOpenBankingBrazilAccountsEndpointResponse.class;
	}

	@Override
	public boolean claimsHttpMtlsPath(String path) {
		return FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)
			|| path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")
			|| FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)
			|| path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")
			|| FAPIBrazilRsPathConstants.BRAZIL_PAYMENT_INITIATION_PATH.equals(path);
	}

	@Override
	public Object handleProfileSpecificMtlsPath(String requestId, String path) {
		// Dispatch via module wrappers so that test classes (e.g. FAPI2SPFinalClientRefreshTokenTest)
		// can override the wrapper to inject custom behavior and fall through via super.
		if (FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH.equals(path)) {
			return module.brazilHandleNewConsentRequest(requestId, false);
		}
		if (path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_CONSENTS_PATH + "/")) {
			return module.brazilHandleGetConsentRequest(requestId, path, false);
		}
		if (FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH.equals(path)) {
			return module.brazilHandleNewConsentRequest(requestId, true);
		}
		if (path.startsWith(FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH + "/")) {
			return module.brazilHandleGetConsentRequest(requestId, path, true);
		}
		// BRAZIL_PAYMENT_INITIATION_PATH
		return module.brazilHandleNewPaymentInitiationRequest(requestId);
	}

	@Override
	public boolean supportsClientCredentialsGrant() {
		return true;
	}

	@Override
	public ConditionSequence preClientCredentialsGrantSetup() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilExtractRequestedScopeFromClientCredentialsGrant.class);
			}
		};
	}

	@Override
	public ConditionSequence customizeAuthorizationEndpoint() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilChangeConsentStatusToAuthorized.class);
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenAcrClaims() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateRequestObjectIdTokenACRClaims.class)
					.onFail(ConditionResult.FAILURE)
					.requirements("OIDCC-5.5.1.1", "BrazilOB-5.2.2-5", "BrazilOB-5.2.2-6")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateAuthorizationRequestScope() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilValidateConsentScope.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint =
					module.getEnv().getBoolean("payments_consent_endpoint_called");
				if (Boolean.TRUE.equals(wasInitialConsentRequestToPaymentsEndpoint)) {
					callAndStopOnFailure(EnsureScopeContainsPayments.class);
				} else {
					callAndStopOnFailure(EnsureScopeContainsAccounts.class);
				}
			}
		};
	}

	@Override
	public ConditionSequence customizeIdTokenClaimsAfterGenerate() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToIdTokenClaims.class,
					"BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			}
		};
	}

	@Override
	public Class<? extends Condition> getAddAcrClaimToIdTokenClaimsCondition() {
		return FAPIBrazilOBAddACRClaimToIdTokenClaims.class;
	}

	@Override
	public ConditionSequence customizeUserInfoResponse() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilAddCPFAndCPNJToUserInfoClaims.class,
					"BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			}
		};
	}

	@Override
	public ConditionSequence validateAccountsEndpointRequest() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainAccounts.class);
				Boolean wasInitialConsentRequestToPaymentsEndpoint =
					module.getEnv().getBoolean("payments_consent_endpoint_called");
				if (Boolean.TRUE.equals(wasInitialConsentRequestToPaymentsEndpoint)) {
					throw new TestFailureException(module.getId(),
						FAPIBrazilRsPathConstants.BRAZIL_PAYMENTS_CONSENTS_PATH
							+ " was called. The test must end at the payment initiation endpoint");
				}
			}
		};
	}

	// --- Brazil mtls request handlers ---

	protected Object brazilHandleNewConsentRequest(String requestId, boolean isPayments) {
		module.doSetStatus(Status.RUNNING);
		module.doCall(module.doExec().startBlock("New consent endpoint").mapKey("incoming_request", requestId));
		module.getEnv().putBoolean("payments_consent_endpoint_called", isPayments);
		module.doCall(module.doExec().mapKey("token_endpoint_request", requestId));
		module.checkMtlsCertificate();
		module.doCall(module.doExec().unmapKey("token_endpoint_request"));

		//Requires method=POST. defined in API docs
		module.doCallAndStopOnFailure(EnsureIncomingRequestMethodIsPost.class);

		module.checkResourceEndpointRequest(true);

		if (isPayments) {
			module.doCallAndStopOnFailure(FAPIBrazilExtractCertificateSubjectFromServerJwks.class);
			module.doCallAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedPayments.class, ConditionResult.FAILURE);
			module.doCallAndContinueOnFailure(FAPIBrazilExtractPaymentsConsentRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			module.doCallAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			module.doCallAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);
			//ensure aud equals endpoint url	"BrazilOB-6.1"
			module.doCallAndContinueOnFailure(FAPIBrazilValidatePaymentConsentRequestAud.class, ConditionResult.FAILURE, "RFC7519-4.1.3", "BrazilOB-6.1");
			//ensure ISS equals TLS certificate organizational unit
			module.doCallAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			module.doCallAndContinueOnFailure(FAPIBrazilEnsureConsentRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			//ensure jti is uuid	"BrazilOB-6.1"
			module.doCallAndContinueOnFailure(FAPIBrazilEnsureConsentRequestJtiIsUUIDv4.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			module.doCallAndContinueOnFailure(FAPIBrazilValidateConsentRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1");

			module.doCallAndContinueOnFailure(FAPIBrazilFetchClientOrganizationJwksFromDirectory.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			module.getEnv().mapKey("parsed_client_request_jwt", "new_consent_request");
			module.doCallAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			module.getEnv().unmapKey("parsed_client_request_jwt");
		} else {
			module.doCallAndContinueOnFailure(FAPIBrazilEnsureClientCredentialsScopeContainedConsents.class, ConditionResult.FAILURE);
			module.doCallAndContinueOnFailure(FAPIBrazilExtractConsentRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
		}

		module.doCallAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		ResponseEntity<Object> responseEntity;
		if (module.isDpopConstrain() && !Strings.isNullOrEmpty(module.getEnv().getString("resource_endpoint_dpop_nonce_error"))) {
			module.createResourceEndpointDpopErrorResponse();
			responseEntity = new ResponseEntity<>(module.getEnv().getObject("resource_endpoint_response"),
				headersFromJson(module.getEnv().getObject("resource_endpoint_response_headers")),
				HttpStatus.valueOf(module.getEnv().getInteger("resource_endpoint_response_http_status")));
		} else {
			if (isPayments) {
				module.doCallAndContinueOnFailure(FAPIBrazilGenerateNewPaymentsConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				module.doCallAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1-2");
				String signedConsentResponse = module.getEnv().getString("signed_consent_response");
				JsonObject headerJson = module.getEnv().getObject("consent_response_headers");

				HttpHeaders headers = headersFromJson(headerJson);
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
				responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);
			} else {
				module.doCallAndContinueOnFailure(FAPIBrazilGenerateNewConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				JsonObject response = module.getEnv().getObject("consent_response");
				JsonObject headerJson = module.getEnv().getObject("consent_response_headers");
				responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.CREATED);
			}
			module.doCallAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
		}

		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());

		module.doSetStatus(Status.WAITING);

		return responseEntity;
	}

	protected Object brazilHandleGetConsentRequest(String requestId, String path, boolean isPayments) {
		module.doSetStatus(Status.RUNNING);
		module.doCall(module.doExec().startBlock("Get consent endpoint").mapKey("incoming_request", requestId));
		module.doCall(module.doExec().mapKey("token_endpoint_request", requestId));
		module.checkMtlsCertificate();
		module.doCall(module.doExec().unmapKey("token_endpoint_request"));


		module.checkResourceEndpointRequest(true);
		module.doCallAndContinueOnFailure(CreateFapiInteractionIdIfNeeded.class, ConditionResult.FAILURE, "FAPI2-IMP-2.1.1");

		String requestedConsentId = path.substring(path.lastIndexOf('/') + 1);
		module.getEnv().putString("requested_consent_id", requestedConsentId);

		ResponseEntity<Object> responseEntity;
		if (module.isDpopConstrain() && !Strings.isNullOrEmpty(module.getEnv().getString("resource_endpoint_dpop_nonce_error"))) {
			module.createResourceEndpointDpopErrorResponse();
			responseEntity = new ResponseEntity<>(module.getEnv().getObject("resource_endpoint_response"),
				headersFromJson(module.getEnv().getObject("resource_endpoint_response_headers")),
				HttpStatus.valueOf(module.getEnv().getInteger("resource_endpoint_response_http_status")));
		} else {
			if (isPayments) {
				module.doCallAndContinueOnFailure(FAPIBrazilGenerateGetPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
				module.getEnv().mapKey("consent_response", "get_consent_response");
				module.doCallAndContinueOnFailure(FAPIBrazilSignPaymentConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
				module.getEnv().unmapKey("consent_response");
				String signedConsentResponse = module.getEnv().getString("signed_consent_response");
				JsonObject headerJson = module.getEnv().getObject("get_consent_response_headers");

				HttpHeaders headers = headersFromJson(headerJson);
				headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
				responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.OK);
			} else {
				module.doCallAndContinueOnFailure(FAPIBrazilGenerateGetConsentResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
				JsonObject response = module.getEnv().getObject("consent_response");
				JsonObject headerJson = module.getEnv().getObject("consent_response_headers");
				responseEntity = new ResponseEntity<>(response, headersFromJson(headerJson), HttpStatus.OK);
			}

			module.doCallAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
		}

		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());

		module.doSetStatus(Status.WAITING);

		return responseEntity;
	}

	protected Object brazilHandleNewPaymentInitiationRequest(String requestId) {
		module.doSetStatus(Status.RUNNING);

		module.doCall(module.doExec().mapKey("token_endpoint_request", requestId));
		module.checkMtlsCertificate();
		module.doCall(module.doExec().unmapKey("token_endpoint_request"));

		module.doCall(module.doExec().startBlock("Payment initiation endpoint").mapKey("incoming_request", requestId));
		//Requires method=POST. defined in API docs
		module.doCallAndContinueOnFailure(EnsureIncomingRequestMethodIsPost.class, ConditionResult.FAILURE);

		module.checkResourceEndpointRequest(false);

		module.doCallAndContinueOnFailure(FAPIBrazilEnsureAuthorizationRequestScopesContainPayments.class, ConditionResult.FAILURE);

		module.doCallAndContinueOnFailure(FAPIBrazilExtractPaymentInitiationRequest.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
		module.getEnv().mapKey("parsed_client_request_jwt", "payment_initiation_request");
		module.doCallAndContinueOnFailure(FAPIBrazilValidateJwtSignatureUsingOrganizationJwks.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		module.getEnv().unmapKey("parsed_client_request_jwt");

		module.doCallAndContinueOnFailure(EnsureIncomingRequestContentTypeIsApplicationJwt.class, ConditionResult.FAILURE, "BrazilOB-6.1");

		module.doCallAndContinueOnFailure(ExtractXIdempotencyKeyHeader.class, ConditionResult.FAILURE);

		//ensure aud equals endpoint url	"BrazilOB-6.1"
		module.doCallAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestAud.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		//ensure ISS equals TLS certificate organizational unit
		module.doCallAndContinueOnFailure(FAPIBrazilExtractCertificateSubjectFromIncomingMTLSCertifiate.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		module.doCallAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestIssEqualsOrganizationId.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		module.doCallAndContinueOnFailure(FAPIBrazilEnsurePaymentInitiationRequestJtiIsUUIDv4.class, ConditionResult.FAILURE, "BrazilOB-6.1");
		module.doCallAndContinueOnFailure(FAPIBrazilValidatePaymentInitiationRequestIat.class, ConditionResult.FAILURE, "BrazilOB-6.1");


		ResponseEntity<Object> responseEntity;
		if (module.isDpopConstrain() && !Strings.isNullOrEmpty(module.getEnv().getString("resource_endpoint_dpop_nonce_error"))) {
			module.createResourceEndpointDpopErrorResponse();
			module.doSetStatus(Status.WAITING);
			responseEntity = new ResponseEntity<>(module.getEnv().getObject("resource_endpoint_response"),
				headersFromJson(module.getEnv().getObject("resource_endpoint_response_headers")),
				HttpStatus.valueOf(module.getEnv().getInteger("resource_endpoint_response_http_status")));
		} else {
			module.doCallAndContinueOnFailure(FAPIBrazilGenerateNewPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-8");
			module.doCallAndContinueOnFailure(FAPIBrazilSignPaymentInitiationResponse.class, ConditionResult.FAILURE, "BrazilOB-6.1");
			String signedConsentResponse = module.getEnv().getString("signed_payment_initiation_response");
			JsonObject headerJson = module.getEnv().getObject("payment_initiation_response_headers");

			HttpHeaders headers = headersFromJson(headerJson);
			headers.setContentType(DATAUTILS_MEDIATYPE_APPLICATION_JWT);
			responseEntity = new ResponseEntity<>(signedConsentResponse, headers, HttpStatus.CREATED);

			module.doCallAndContinueOnFailure(ClearAccessTokenFromRequest.class, ConditionResult.FAILURE);
			module.resourceEndpointCallComplete();
		}

		module.doCall(module.doExec().unmapKey("incoming_request").endBlock());
		return responseEntity;
	}
}
