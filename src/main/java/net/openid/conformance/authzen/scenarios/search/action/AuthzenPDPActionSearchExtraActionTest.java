package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

import java.util.Set;

@PublishTestModule(
	testName = "authzen-pdp-action-search-extra-action",
	displayName = "AuthZEN Action Search API - Section 8.6.1: Action in an Action Search request is ignored or rejected",
	summary = "Section 8.6.1 omits `action` from the Action Search request, and this request carries one alongside the required `subject` and `resource`. "
		+ "The specification does not say how a PDP treats it. It may ignore it as a field the request does not define (Section 10.1.1, \"receivers MUST ignore unknown fields\") and return HTTP 200 with the actions it would return without it, which here MUST include read and write; "
		+ "or it may reject the request with HTTP 400 as one that does not conform to Section 8.6.1. "
		+ "It MUST NOT treat `action` as a filter, which nothing defines: a 200 response that leaves out write fails.\n" + AuthzenPDPActionSearchExtraActionTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPActionSearchExtraActionTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" },
			"action": { "name": "read" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected boolean sendRawRequest() {
		return true;
	}

	@Override
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return Set.of(200, 400);
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return """
			{
				"results": [
					{ "name": "read" },
					{ "name": "write" }
				]
			}
			""";
	}
}
