package net.openid.conformance.authzen.scenarios.search.action;

import net.openid.conformance.authzen.AbstractAuthzenPDPActionSearchTest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "authzen-pdp-action-search-accept-content-type-with-charset",
	displayName = "Authzen Action Search API - Spec 10.1-2: Accept Content-Type with charset",
	summary = "Per spec 10.1-2 and RFC 9110, `application/json; charset=utf-8` is a valid form of the JSON Content-Type. The PDP MUST accept it and return the expected response.",
	profile = "Authzen"
)
public class AuthzenPDPActionSearchAcceptContentTypeWithCharsetTest extends AbstractAuthzenPDPActionSearchTest {

	public static final String payload = """
		{
			"subject": { "type": "user", "id": "alice" },
			"resource": { "type": "record", "id": "record-1" }
		}
		""";

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getRequestContentTypeOverride() {
		return "application/json; charset=utf-8";
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
