package net.openid.conformance.openid.client;

import net.openid.conformance.condition.as.dynregistration.ValidateClientInitiateLoginUri;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.testmodule.TestFailureException;
import org.springframework.web.util.UriComponentsBuilder;

@PublishTestModule(
	testName = "oidcc-client-test-3rd-party-init-login",
	displayName = "OIDCC: Relying party test, 3rd party initiated login",
	summary = "The client is expected to register with a valid 'initiate_login_uri'. The user is sent to that url, which should result in the RP redirecting the user to the authorization endpoint and the normal 'happy path' sequence completing.",
	profile = "OIDCC",
	configurationFields = {
	}
)
public class OIDCCClient3rdPartyInitiatedLoginTest extends AbstractOIDCCClientTest {

	@Override
	protected void validateClientMetadata() {
		callAndStopOnFailure(ValidateClientInitiateLoginUri.class,"OIDCR-2");

		// run in background so it gets recorded in the log after the response is sent to the client
		getTestExecutionManager().runInBackground(() -> {
			setStatus(Status.RUNNING);
			String initiateLoginUri = env.getString("client", "initiate_login_uri");
			String issuer = env.getString("issuer");

			UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(initiateLoginUri);

			builder.queryParam("iss", issuer);

			String redirectTo = builder.toUriString();

			eventLog.log(getName(), args("msg", "Redirecting user to initiate_login_uri - press 'Proceed with test' to continue",
				"redirect_to", redirectTo,
				"http", "redirect"));

			setStatus(Status.WAITING);

			browser.goToUrl(redirectTo);

			return "done";
		});
	}

	@Override
	protected Object handleAuthorizationEndpointRequest(String requestId) {
		if (browser.getVisited().size() == 0) {
			throw new TestFailureException(getId(), "Authorization endpoint called before user has been sent to initiate_login_uri");
		}
		return super.handleAuthorizationEndpointRequest(requestId);
	}
}
