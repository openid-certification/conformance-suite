package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddAudAsPaymentInitiationUriToRequestObject;
import net.openid.conformance.condition.client.AddEndToEndIdToPaymentRequestEntityClaims;
import net.openid.conformance.condition.client.AddIatToRequestObject;
import net.openid.conformance.condition.client.AddIdempotencyKeyHeader;
import net.openid.conformance.condition.client.AddIssAsCertificateOuToRequestObject;
import net.openid.conformance.condition.client.AddJtiAsUuidToRequestObject;
import net.openid.conformance.condition.client.CheckDiscEndpointAcrClaimSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointClaimsParameterSupported;
import net.openid.conformance.condition.client.CheckDiscEndpointUserinfoEndpoint;
import net.openid.conformance.condition.client.CreateIdempotencyKey;
import net.openid.conformance.condition.client.CreatePaymentRequestEntityClaims;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould;
import net.openid.conformance.condition.client.FAPIBrazilCheckDirectoryKeystore;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointScopesSupportedForNonPayments;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointScopesSupportedForPayments;
import net.openid.conformance.condition.client.FAPIBrazilCheckDiscEndpointCpfOrCnpjClaimSupported;
import net.openid.conformance.condition.client.FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode;
import net.openid.conformance.condition.client.FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken;
import net.openid.conformance.condition.client.FAPIBrazilSignPaymentInitiationRequest;
import net.openid.conformance.condition.client.FAPIBrazilValidateExpiresIn;
import net.openid.conformance.condition.client.FAPIBrazilValidateIdTokenSigningAlg;
import net.openid.conformance.condition.client.SetApplicationJwtAcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetApplicationJwtContentTypeHeaderForResourceEndpointRequest;
import net.openid.conformance.condition.client.SetConsentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetPaymentsScopeOnTokenEndpointRequest;
import net.openid.conformance.condition.client.SetResourceMethodToPost;
import net.openid.conformance.condition.common.FAPIBrazilCheckKeyAlgInClientJWKs;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.sequence.client.ValidateBrazilPaymentInitiationSignedResponse;

import java.util.function.Supplier;

/**
 * Profile behavior for OpenBanking Brazil.
 * Requires mTLS everywhere, Brazil-specific key algorithm validation, request object encryption,
 * Brazil-specific expires_in and id_token signing alg validation, and payments API support.
 */
public class OpenBankingBrazilProfileBehavior extends FAPI2ProfileBehavior {

	@Override
	public boolean requiresMtlsEverywhere() {
		return true;
	}

	@Override
	public Supplier<? extends ConditionSequence> getPreAuthorizationSteps() {
		return this::createOBBPreauthSteps;
	}

	protected ConditionSequence createOBBPreauthSteps() {
		boolean payments = module.scopeContains("payments");
		if (payments) {
			module.getEventLog().log(module.getName(), "Payments scope present - protected resource assumed to be a payments endpoint");
			module.updatePaymentConsent();
		}
		return createOpenBankingBrazilPreAuthorizationSteps(payments, false);
	}

	protected OpenBankingBrazilPreAuthorizationSteps createOpenBankingBrazilPreAuthorizationSteps(boolean payments, boolean stopAfterConsentEndpointCall) {
		return new OpenBankingBrazilPreAuthorizationSteps(
			module.isSecondClient(),
			module.isDpop(),
			module.addClientAuthentication,
			payments,
			false, // open insurance not yet supported in fapi2
			stopAfterConsentEndpointCall,
			false);
	}

	@Override
	public boolean shouldEncryptRequestObject(boolean isPar) {
		return !isPar;
	}

	@Override
	public ConditionSequence validateKeyAlgorithms() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPIBrazilCheckKeyAlgInClientJWKs.class,
					ConditionResult.FAILURE, "BrazilOB-6.1");
			}
		};
	}

	@Override
	public ConditionSequence validateExpiresIn() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				call(condition(FAPIBrazilValidateExpiresIn.class)
					.skipIfObjectMissing("expires_in")
					.onSkip(ConditionResult.INFO)
					.onFail(ConditionResult.FAILURE)
					.requirement("BrazilOB-5.2.2-12")
					.dontStopOnFailure());
			}
		};
	}

	@Override
	public ConditionSequence validateIdTokenSigningAlg() {
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPIBrazilValidateIdTokenSigningAlg.class,
					ConditionResult.FAILURE, "BrazilOB-6.1");
			}
		};
	}

	@Override
	public ConditionSequence setupResourceEndpointRequestBody() {
		if (!module.scopeContains("payments")) {
			return null;
		}

		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				// setup to call the payments initiation API, which requires a signed jwt request body
				call(sequenceOf(
					condition(CreateIdempotencyKey.class),
					condition(AddIdempotencyKeyHeader.class)));
				callAndStopOnFailure(SetApplicationJwtContentTypeHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetApplicationJwtAcceptHeaderForResourceEndpointRequest.class);
				callAndStopOnFailure(SetResourceMethodToPost.class);
				callAndStopOnFailure(CreatePaymentRequestEntityClaims.class);
				callAndStopOnFailure(AddEndToEndIdToPaymentRequestEntityClaims.class);

				// we reuse the request object conditions to add various jwt claims
				call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));

				// aud: the Resource Provider must validate if the value of the aud field matches the endpoint being triggered
				callAndStopOnFailure(AddAudAsPaymentInitiationUriToRequestObject.class, "BrazilOB-6.1");

				// iss: the receiver of the message shall validate if the value of the iss field matches the organisationId of the sender
				callAndStopOnFailure(AddIssAsCertificateOuToRequestObject.class, "BrazilOB-6.1");

				// jti: the value of the jti field shall be filled with the UUID defined by the institution according to RFC4122 version 4
				callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");

				// iat: the iat field shall be filled with the message generation time
				callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");

				call(exec().unmapKey("request_object_claims"));

				callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
			}
		};
	}

	@Override
	public ConditionSequence validateResourceEndpointResponse() {
		if (module.scopeContains("payments")) {
			return new ValidateBrazilPaymentInitiationSignedResponse();
		}
		return null;
	}

	@Override
	public ConditionSequence setAlternateResourceEndpointContentHeaders() {
		if (module.scopeContains("payments")) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndStopOnFailure(SetApplicationJwtCharsetUtf8ContentTypeHeaderForResourceEndpointRequest.class);
					callAndStopOnFailure(SetApplicationJwtCharsetUtf8AcceptHeaderForResourceEndpointRequest.class);
				}
			};
		}
		return null;
	}

	@Override
	public ConditionSequence onConfigure() {
		if (module.scopeContains("payments")) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndContinueOnFailure(FAPIBrazilCheckDirectoryKeystore.class,
						ConditionResult.FAILURE);
				}
			};
		}
		return null;
	}

	@Override
	public ConditionSequence validateDiscoveryEndpointScopes() {
		if (module.scopeContains("payments")) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointScopesSupportedForPayments.class,
						ConditionResult.FAILURE);
				}
			};
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointScopesSupportedForNonPayments.class,
					ConditionResult.FAILURE);
			}
		};
	}

	@Override
	public ConditionSequence setTokenEndpointScopeForClientCredentials() {
		if (module.scopeContains("payments")) {
			return new AbstractConditionSequence() {
				@Override
				public void evaluate() {
					callAndStopOnFailure(SetPaymentsScopeOnTokenEndpointRequest.class);
				}
			};
		}
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				callAndStopOnFailure(SetConsentsScopeOnTokenEndpointRequest.class);
			}
		};
	}

	@Override
	public ConditionSequence createUpdateResourceRequestSteps(
			Supplier<? extends ConditionSequence> createDpopForResourceEndpointSteps) {
		boolean payments = module.scopeContains("payments");
		return new AbstractConditionSequence() {
			@Override
			public void evaluate() {
				if (createDpopForResourceEndpointSteps != null) {
					call(sequence(createDpopForResourceEndpointSteps));
				}
				if (payments) {
					// we use the idempotency header to allow us to make a request more than once; however it is required
					// that a new jwt is sent in each retry, so update jti/iat & resign
					call(exec().mapKey("request_object_claims", "resource_request_entity_claims"));
					callAndStopOnFailure(AddJtiAsUuidToRequestObject.class, "BrazilOB-6.1");
					callAndStopOnFailure(AddIatToRequestObject.class, "BrazilOB-6.1");
					call(exec().unmapKey("request_object_claims"));
					callAndStopOnFailure(FAPIBrazilSignPaymentInitiationRequest.class);
				}
			}
		};
	}

	@Override
	public Supplier<? extends ConditionSequence> getProfileSpecificDiscoveryChecks() {
		return DiscoveryEndpointChecks::new;
	}

	public static class DiscoveryEndpointChecks extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndContinueOnFailure(CheckDiscEndpointClaimsParameterSupported.class, ConditionResult.FAILURE,
				"OIDCD-3", "BrazilOB-5.2.2-4");

			callAndContinueOnFailure(CheckDiscEndpointAcrClaimSupported.class, ConditionResult.FAILURE,
				"BrazilOB-5.2.2-4", "BrazilOB-5.2.2-5", "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointCpfOrCnpjClaimSupported.class, ConditionResult.FAILURE,
				"BrazilOB-5.2.2-4", "BrazilOB-7.2.2-8", "BrazilOB-7.2.2-10");
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsAuthorizationCode.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPICheckDiscEndpointGrantTypesSupportedContainsClientCredentialsAndRefreshToken.class, ConditionResult.FAILURE);
			callAndContinueOnFailure(FAPIBrazilOpenBankingCheckDiscEndpointAcrValuesSupported.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-5");
			callAndContinueOnFailure(FAPIBrazilCheckDiscEndpointAcrValuesSupportedShould.class, ConditionResult.WARNING, "BrazilOB-5.2.2-6");
			callAndContinueOnFailure(CheckDiscEndpointUserinfoEndpoint.class, ConditionResult.FAILURE, "BrazilOB-5.2.2-7");
		}
	}
}
