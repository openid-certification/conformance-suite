package net.openid.conformance.authzen.scenarios.search.resource;

import com.google.gson.JsonParser;
import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.authzen.condition.AbstractChangeAuthzenSearchRequest;
import net.openid.conformance.authzen.condition.ChangeAuthzenSearchRequestEntity;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-changed-page-entity",
	displayName = "AuthZEN Resource Search API - Section 8.2: Page token sent with a changed subject",
	summary = "Section 8.2: when a request contains a page token, \"all entities (e.g., subject, resource, action, context) and pagination parameters (e.g., limit) MUST be identical to the preceding request. PDPs SHOULD return an error when any entity or parameter has been changed.\" "
		+ "The harness requests the first page with `page.limit` 1, then sends the returned `next_token` with the subject changed from alice to bob. A PDP that answers with an HTTP 4xx error passes; one that returns a page is reported as a warning, and any other status, such as a 5xx, fails. "
		+ "Pagination is optional (Section 8.2), so the test is skipped when the PDP returns the whole result set in one response.\n" + AuthzenPDPResourceSearchChangedPageEntityTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchChangedPageEntityTest extends AbstractAuthzenPDPResourceSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"action": { "name": "read" },
			"resource": { "type": "record" },
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
	protected AbstractChangeAuthzenSearchRequest changeRequestWithPageToken() {
		return new ChangeAuthzenSearchRequestEntity("subject", JsonParser.parseString("""
			{ "type": "user", "id": "bob" }""").getAsJsonObject());
	}
}
