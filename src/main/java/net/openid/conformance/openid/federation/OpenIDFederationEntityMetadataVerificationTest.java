package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "openid-federation-entity-metadata-verification",
	displayName = "OpenID Federation: Federation Entity metadata Verification",
	summary = "This test verifies the correctness of endpoints provided in the entity's federation_entity metadata. " +
		"The test is isolated to the provided entity and will not proceed to its superiors.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_statement_url",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationEntityMetadataVerificationTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

		String listEndpoint = env.getString("federation_list_endpoint");
		JsonArray listEndpointResponse;
		if (listEndpoint != null) {
			eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", listEndpoint));
			callAndContinueOnFailure(GetSubordinateListingResponse.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.mapKey("endpoint_response", "federation_list_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
			env.unmapKey("endpoint_response");
			eventLog.endBlock();

			listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				eventLog.startBlock(String.format("Validating entity statement for %s", entityIdentifier));
				env.putString("entity_statement_url", appendWellKnown(entityIdentifier));
				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				validateEntityStatementResponse();
				validateEntityStatement();
				eventLog.endBlock();
			}
		} else {
			 listEndpointResponse = new JsonArray();
		}

		String fetchEndpoint = env.getString("federation_fetch_endpoint");
		if (fetchEndpoint != null) {
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);

				env.putString("entity_statement_url", fetchEndpoint);
				env.putString("expected_sub", entityIdentifier);
				callAndContinueOnFailure(AppendSubToEntityStatementUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("entity_statement_url")));

				callAndContinueOnFailure(GetEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				env.mapKey("endpoint_response", "entity_statement_endpoint_response");
				callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				env.unmapKey("endpoint_response");

				callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

				env.mapKey("server_jwks", "primary_entity_statement_body.jwks"); //This didn't work, it returned null
				call(sequence(ValidateEntityStatementSignatureSequence.class));
				env.unmapKey("server_jwks");

				callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-?");
				callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-?");

				eventLog.endBlock();
			}
		}

		fireTestFinished();
	}

}
