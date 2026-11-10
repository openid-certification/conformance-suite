package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallJwtVcIssuerMetadataEndpoint;
import net.openid.conformance.condition.client.CallJwtVcIssuerMetadataJwksUri;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.condition.client.ExtractJwksFromJwtVcIssuerMetadata;
import net.openid.conformance.condition.client.ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse;
import net.openid.conformance.condition.client.ExtractJwtVcIssuerMetadata;
import net.openid.conformance.condition.client.FetchServerKeys;
import net.openid.conformance.condition.client.ResolveStatusListTokenKeyFromJwtOrCredential;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ValidateJwksSequence;

/**
 * Resolves the Token Status List JWT signing key outside HAIP, in the order
 * {@link net.openid.conformance.condition.client.VerifyStatusListTokenSignature} uses the sources,
 * fetching from the network only when the earlier sources yield no key:
 * <ol>
 *   <li>the token's JOSE header or the referenced credential (no request)</li>
 *   <li>the issuer's JWT VC Issuer Metadata, then its {@code jwks_uri} when it has no inline
 *       {@code jwks}</li>
 *   <li>the authorization server's {@code jwks_uri}, when the server metadata advertises one</li>
 * </ol>
 * Every step after the first skips when no status list token was fetched. The results are logged
 * in the caller's current log block.
 */
public class ResolveStatusListTokenVerificationKey extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndContinueOnFailure(ResolveStatusListTokenKeyFromJwtOrCredential.class,
			ConditionResult.FAILURE, "OTSL-11.3");

		call(condition(CallJwtVcIssuerMetadataEndpoint.class)
			.skipIfObjectMissing("status_list_token_jwt_vc_issuer")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.1")
			.dontStopOnFailure());

		call(exec().mapKey("endpoint_response", CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY));
		call(condition(EnsureHttpStatusCodeIs200.class)
			.skipIfObjectMissing(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());
		call(condition(EnsureContentTypeJson.class)
			.skipIfObjectMissing(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());
		call(exec().unmapKey("endpoint_response"));

		call(condition(ExtractJwtVcIssuerMetadata.class)
			.skipIfObjectMissing(CallJwtVcIssuerMetadataEndpoint.RESPONSE_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2", "SDJWTVC-5.3")
			.dontStopOnFailure());

		call(condition(ExtractJwksFromJwtVcIssuerMetadata.class)
			.skipIfObjectMissing("jwt_vc_issuer_metadata")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());

		call(condition(CallJwtVcIssuerMetadataJwksUri.class)
			.skipIfStringMissing("jwt_vc_issuer_metadata_jwks_uri")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());

		call(exec().mapKey("endpoint_response", CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY));
		call(condition(EnsureHttpStatusCodeIs200.class)
			.skipIfObjectMissing(CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());
		call(exec().unmapKey("endpoint_response"));

		call(condition(ExtractJwksFromJwtVcIssuerMetadataJwksUriResponse.class)
			.skipIfObjectMissing(CallJwtVcIssuerMetadataJwksUri.RESPONSE_KEY)
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("SDJWTVC-5.2")
			.dontStopOnFailure());

		call(sequence(() -> new ValidateJwksSequence("jwt_vc_issuer_metadata_jwks", null,
			"JWT VC Issuer Metadata", "SDJWTVC-5.2").withoutLogBlock()));

		// jwks_uri is OPTIONAL in RFC 8414 metadata; the authorization server keys are only
		// fetched when no earlier source yielded a key
		call(exec().mapKey("server_jwks", "status_list_token_server_jwks"));
		call(condition(FetchServerKeys.class)
			.skipIfObjectMissing("status_list_token")
			.skipIfElementPresent("status_list_token_verification_key", "jwks")
			.skipIfElementPresent("jwt_vc_issuer_metadata_jwks", "keys")
			.skipIfElementMissing("server", "jwks_uri")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.FAILURE)
			.requirements("RFC8414-2")
			.dontStopOnFailure());
		call(exec().unmapKey("server_jwks"));

		call(sequence(() -> new ValidateJwksSequence("status_list_token_server_jwks", null,
			"authorization server jwks_uri", "RFC8414-2").withoutLogBlock()));
	}
}
