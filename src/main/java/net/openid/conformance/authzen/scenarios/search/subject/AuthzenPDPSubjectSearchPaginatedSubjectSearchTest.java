package net.openid.conformance.authzen.scenarios.search.subject;

import com.google.gson.JsonObject;
import net.openid.conformance.authzen.AbstractAuthzenPDPPaginatedSearchTest;
import net.openid.conformance.authzen.condition.CreateAuthzenSubjectSearchApiRequestSteps;
import net.openid.conformance.authzen.condition.EnsureSearchResponsePageIsFirstKey;
import net.openid.conformance.authzen.condition.EnsureValidSearchResponsePage;
import net.openid.conformance.authzen.condition.EnsureValidSubjectSearchResponse;
import net.openid.conformance.authzen.condition.ExtractAuthzenApiEndpointSearchResponse;
import net.openid.conformance.authzen.condition.SetAuthzenApiEndpointToSubjectSearchEndpoint;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.PDPServerMetadata;
import net.openid.conformance.variant.VariantConfigurationFields;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-request-with-page-token",
	displayName = "Authzen Subject Search API - Section 4.5.2: Request with page token (paginated follow-up)",
	summary = "Section 4.5.2 a paginated subject search. When the PDP returns a non-empty next_token, the harness follows up with that token until all pages are returned.\n" + AuthzenPDPSubjectSearchPaginatedSubjectSearchTest.payload,
	profile = "Authzen"
)
@VariantConfigurationFields(parameter = PDPServerMetadata.class, value = "static", configurationFields = {
	"pdp.search_subject_endpoint"
})
public class AuthzenPDPSubjectSearchPaginatedSubjectSearchTest extends AbstractAuthzenPDPPaginatedSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user" },
			"action": { "name": "read" },
			"resource": { "type": "record", "id": "record-1" },
			"page": {
				"limit": 1
			}
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "type": "user", "id": "alice" },
					{ "type": "user", "id": "bob" }
				]
			}
			""";
	}

	@Override
	protected ConditionSequence createAuthzenApiRequestSequence() {
		JsonObject request = parseRequest();
		return new CreateAuthzenSubjectSearchApiRequestSteps(
			request.getAsJsonObject("subject"),
			request.getAsJsonObject("resource"),
			request.getAsJsonObject("action"),
			request.getAsJsonObject("context"),
			request.getAsJsonObject("page"));
	}

	@Override
	protected void setAuthzenApiEndpoint() {
		callAndStopOnFailure(SetAuthzenApiEndpointToSubjectSearchEndpoint.class);
	}

	@Override
	protected void processAuthApiEndpointResponse() {
		callAndStopOnFailure(ExtractAuthzenApiEndpointSearchResponse.class, "AUTHZEN-8.3");
		callAndStopOnFailure(EnsureValidSubjectSearchResponse.class, "AUTHZEN-8.3", "AUTHZEN-8.4");
		callAndStopOnFailure(EnsureValidSearchResponsePage.class, "AUTHZEN-8.2.2", "AUTHZEN-8.3");
		callAndContinueOnFailure(EnsureSearchResponsePageIsFirstKey.class, ConditionResult.WARNING, "AUTHZEN-8.3");
	}
}
