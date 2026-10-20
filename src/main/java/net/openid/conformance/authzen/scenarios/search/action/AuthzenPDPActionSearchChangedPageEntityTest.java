package net.openid.conformance.authzen.scenarios.search.action;

import com.google.gson.JsonParser;
import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.authzen.condition.AbstractChangeAuthzenSearchRequest;
import net.openid.conformance.authzen.condition.ChangeAuthzenSearchRequestEntity;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-changed-page-entity",
	displayName = "AuthZEN Action Search API - Section 8.2: Page token sent with a changed resource",
	summary = "Section 8.2: when a request contains a page token, \"all entities (e.g., subject, resource, action, context) and pagination parameters (e.g., limit) MUST be identical to the preceding request. PDPs SHOULD return an error when any entity or parameter has been changed.\" "
		+ "The harness requests the first page with `page.limit` 1, then sends the returned `next_token` with the resource changed from record-1 to record-2. A PDP that answers with an HTTP 4xx error passes; one that returns a page is reported as a warning, and that page must still be a well-formed search response. Any other status, such as a 5xx, fails. "
		+ "Pagination is optional (Section 8.2), so the test is skipped when the PDP returns the whole result set in one response.\n" + AuthzenPDPActionSearchChangedPageEntityTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchChangedPageEntityTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
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
	protected AbstractChangeAuthzenSearchRequest changeRequestWithPageToken() {
		return new ChangeAuthzenSearchRequestEntity("resource", JsonParser.parseString("""
			{ "type": "record", "id": "record-2" }""").getAsJsonObject());
	}
}
