package net.openid.conformance.authzen.scenarios.search.subject;

import com.google.common.base.Strings;
import net.openid.conformance.authzen.AbstractAuthzenPDPSubjectSearchTest;
import net.openid.conformance.authzen.condition.ChangeAuthzenSearchRequestPageLimit;
import net.openid.conformance.authzen.condition.WarnChangedPageRequestAccepted;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@PublishTestModule(
	testName = "authzen-pdp-subject-search-changed-page-parameters",
	displayName = "AuthZEN Subject Search API - Section 8.2: Page token sent with a changed limit",
	summary = "Section 8.2: when a request contains a page token, its parameters MUST be identical to the preceding request's, and \"PDPs SHOULD return an error when any entity or parameter has been changed\". "
		+ "The harness requests the first page with `page.limit` 1, then sends the returned `next_token` with `page.limit` 2. A PDP that answers with an HTTP 4xx error passes; one that returns a page is reported as a warning, and any other status, such as a 5xx, fails. "
		+ "Pagination is optional (Section 8.2), so the test is skipped when the PDP returns the whole result set in one response.\n" + AuthzenPDPSubjectSearchChangedPageParametersTest.payload,
	profile = "AuthZEN"
)
public class AuthzenPDPSubjectSearchChangedPageParametersTest extends AbstractAuthzenPDPSubjectSearchTest {

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

	/**
	 * A 4xx is the error Section 8.2 asks for, and a 200 is checked by WarnChangedPageRequestAccepted;
	 * any other status fails the status check.
	 */
	private static final Set<Integer> CHANGED_REQUEST_STATUSES = IntStream.concat(IntStream.of(200), IntStream.range(400, 500))
		.boxed().collect(Collectors.toUnmodifiableSet());

	private boolean sendingChangedRequest;

	@Override
	protected String getPayload() {
		return payload;
	}

	@Override
	protected String getExpectedSearchResponseJson() {
		return "{ \"results\": [] }";
	}

	@Override
	protected void performAuthzenApiFlow() {
		eventLog.startBlock("Request the first page");
		try {
			createAuthzenApiRequest();
			performSingleApiRequest();
			if (!responseStatusIs200()) {
				// The status check has already failed, and there is no page token to send.
				throw new TestFailureException(getId(), "The PDP did not answer the first page with HTTP 200, so there is no page token to send with a changed limit");
			}
			processAuthApiEndpointResponse();
		} finally {
			eventLog.endBlock();
		}

		if (Strings.isNullOrEmpty(env.getString("authzen_search_endpoint_request_page_token"))) {
			fireTestSkipped("The PDP returned the whole result set in one response, so it does not paginate this search and there is no page token to send with a changed limit. Pagination is optional (Section 8.2).");
			return;
		}

		eventLog.startBlock("Send the page token with a changed limit");
		try {
			sendingChangedRequest = true;
			createAuthzenApiRequest();
			callAndStopOnFailure(new ChangeAuthzenSearchRequestPageLimit(2), "AUTHZEN-8.2");
			performSingleApiRequest();
			callAndContinueOnFailure(WarnChangedPageRequestAccepted.class, ConditionResult.WARNING, "AUTHZEN-8.2");
		} finally {
			eventLog.endBlock();
		}

		performPostApiFlow();
	}

	@Override
	protected Set<Integer> getAcceptableHttpStatusCodes() {
		return sendingChangedRequest ? CHANGED_REQUEST_STATUSES : Set.of(200);
	}

	@Override
	protected String[] expectedStatusRequirements(Set<Integer> acceptable) {
		return new String[] {"AUTHZEN-8.2", "AUTHZEN-10.1.2"};
	}
}
