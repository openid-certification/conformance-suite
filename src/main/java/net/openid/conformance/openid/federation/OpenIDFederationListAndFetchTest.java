package net.openid.conformance.openid.federation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs200;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import static net.openid.conformance.openid.federation.EntityUtils.appendWellKnown;

@PublishTestModule(
	testName = "openid-federation-list-and-fetch",
	displayName = "OpenID Federation: List and fetch",
	summary = "This test validates the List and Fetch endpoints provided in the entity's `federation_entity` metadata. " +
		"The test will call the List endpoint, followed by a request to the Fetch endpoint for each of its subordinates, " +
		"followed by validation of the corresponding Subordinate statements.",
	profile = "OIDFED",
	configurationFields = {
		"federation.entity_identifier",
		"federation.trust_anchor_jwks"
	}
)
public class OpenIDFederationListAndFetchTest extends AbstractOpenIDFederationTest {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndContinueOnFailure(ExtractFederationEntityMetadataUrls.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
		String listEndpoint = env.getString("federation_list_endpoint");
		if (listEndpoint == null) {
			fireTestSkipped("Entity metadata does not contain a federation_list_endpoint.");
		}

		String fetchEndpoint = env.getString("federation_fetch_endpoint");
		JsonArray listedEntities = validateListEndpoint(listEndpoint);
		validateFetchEndpoint(fetchEndpoint, listedEntities);

		fireTestFinished();
	}

	protected JsonArray validateListEndpoint(String listEndpoint) {
		JsonArray listEndpointResponse;
		if (listEndpoint != null) {
			eventLog.startBlock(String.format("Retrieving entities from federation_list_endpoint %s", listEndpoint));
			callAndContinueOnFailure(CallListEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.1");
			env.mapKey("endpoint_response", "federation_list_endpoint_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.2");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.2");
			callAndContinueOnFailure(EnsureResponseIsJsonArray.class, Condition.ConditionResult.FAILURE, "OIDFED-8.2.2");
			env.unmapKey("endpoint_response");
			eventLog.endBlock();

			listEndpointResponse = JsonParser.parseString(env.getString("endpoint_response_body")).getAsJsonArray();
			for (JsonElement listElement : listEndpointResponse) {
				String entityIdentifier = OIDFJSON.getString(listElement);
				eventLog.startBlock(String.format("Validating entity statement for %s", entityIdentifier));
				env.putString("federation_endpoint_url", appendWellKnown(entityIdentifier));
				callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-9");
				validateEntityStatementResponse();
				validateEntityStatement();
				eventLog.endBlock();
			}
		} else {
			listEndpointResponse = new JsonArray();
		}
		return listEndpointResponse;
	}

	protected void validateFetchEndpoint(String fetchEndpoint, JsonArray entities) {
		env.putString("expected_iss", env.getString("primary_entity_statement_iss"));
		for (JsonElement listElement : entities) {
			String entityIdentifier = OIDFJSON.getString(listElement);

			env.putString("federation_endpoint_url", fetchEndpoint);
			env.putString("expected_sub", entityIdentifier);
			callAndContinueOnFailure(AppendSubToFederationEndpointUrl.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");

			eventLog.startBlock(String.format("Fetching subordinate statement from %s", env.getString("federation_endpoint_url")));

			callAndContinueOnFailure(CallFederationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.1");

			env.mapKey("endpoint_response", "federation_http_response");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs200.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
			callAndContinueOnFailure(EnsureContentTypeEntityStatementJwt.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1.2");
			env.unmapKey("endpoint_response");

			callAndContinueOnFailure(ExtractBasicClaimsFromEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
			call(sequence(ValidateEntityStatementBasicClaimsSequence.class));

			callAndContinueOnFailure(ExtractJWKsFromPrimaryEntityStatement.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
			call(sequence(ValidateEntityStatementSignatureSequence.class));

			callAndContinueOnFailure(ValidateEntityStatementMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-5.1.1");

			callAndContinueOnFailure(ValidateAbsenceOfAuthorityHints.class, Condition.ConditionResult.FAILURE, "OIDFED-3");
			callAndContinueOnFailure(ValidateAbsenceOfFederationEntityMetadata.class, Condition.ConditionResult.FAILURE, "OIDFED-8.1");

			callAndContinueOnFailure(ValidateEntityStatementMetadataPolicy.class, Condition.ConditionResult.FAILURE, "OIDFED-6.1.2");

			eventLog.endBlock();
		}
	}

}
