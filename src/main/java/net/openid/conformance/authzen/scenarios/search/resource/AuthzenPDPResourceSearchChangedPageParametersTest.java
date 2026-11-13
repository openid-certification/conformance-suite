package net.openid.conformance.authzen.scenarios.search.resource;

import net.openid.conformance.authzen.AbstractAuthzenPDPResourceSearchTest;
import net.openid.conformance.authzen.condition.AbstractChangeAuthzenSearchRequest;
import net.openid.conformance.authzen.condition.ChangeAuthzenSearchRequestPageLimit;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-resource-search-changed-page-parameters",
	displayName = "AuthZEN Resource Search API - Section 8.2: Page token sent with a changed limit",
	summary = "Section 8.2: when a request contains a page token, its parameters MUST be identical to the preceding request's. The published text says PDPs SHOULD return an error when any entity or parameter has been changed; the AuthZEN working group made this a MUST in openid/authzen#775. "
		+ "The harness requests the first page with `page.limit` 1, then sends the returned `next_token` with `page.limit` 2. A PDP that answers with an HTTP 4xx error passes; one that returns a page fails, as does any other status, such as a 5xx. "
		+ "Pagination is optional (Section 8.2), so the test is skipped when the PDP returns the whole result set in one response. The fixture guarantees only record-1 for this search (search requirement S2, certification scenario 4.5), so a PDP that paginates may still return it in one page.\n" + AuthzenPDPResourceSearchChangedPageParametersTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPResourceSearchChangedPageParametersTest extends AbstractAuthzenPDPResourceSearchTest {

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
		return new ChangeAuthzenSearchRequestPageLimit(2);
	}
}
