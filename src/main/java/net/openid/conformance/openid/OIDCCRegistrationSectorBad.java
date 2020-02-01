package net.openid.conformance.openid;

import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddSectorIdentifierUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSubjectTypePairwiseToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpointExpectingError;
import net.openid.conformance.condition.client.CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata;
import net.openid.conformance.condition.client.CreateInvalidSectorRedirectUris;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to https://www.heenan.me.uk/~joseph/oidcc_test_desc-phase1.html#OP_Registration_Sector_Bad
@PublishTestModule(
	testName = "oidcc-registration-sector-bad",
	displayName = "OIDCC: dynamic registration with bad sector redirect URIs",
	summary = "This test calls the dynamic registration endpoint with a sector_identifier_uri pointing to a document not containing the test's redirect URI. This should result in an error from the dynamic registration endpoint.",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl"
	}
)
public class OIDCCRegistrationSectorBad extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		JsonObject server = env.getObject("server");
		JsonElement subjectTypesSupported = server.get("subject_types_supported");
		if (subjectTypesSupported == null
				|| !subjectTypesSupported.isJsonArray()
				|| !subjectTypesSupported.getAsJsonArray().contains(new JsonPrimitive("pairwise"))) {
			fireTestSkipped("Server configuration does not explicitly support pairwise subject type");
		}
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		configureDynamicClient();

		// No authorization flow in this test

		fireTestFinished();
	}

	@Override
	protected void configureDynamicClient() {

		callAndStopOnFailure(CreateInvalidSectorRedirectUris.class);

		createDynamicClientRegistrationRequest();

		expose("client_name", env.getString("dynamic_registration_request", "client_name"));

		callAndStopOnFailure(CallDynamicRegistrationEndpointExpectingError.class, "OIDCR-5");

		callAndContinueOnFailure(CheckErrorFromDynamicRegistrationEndpointIsInvalidClientMetadata.class, Condition.ConditionResult.WARNING, "OIDCR-3.3");
	}

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddSubjectTypePairwiseToDynamicRegistrationRequest.class);
		callAndStopOnFailure(AddSectorIdentifierUriToDynamicRegistrationRequest.class);
	}

	@Override
	public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res, HttpSession session, JsonObject requestParts) {
		if (path.equals("redirect_uris.json")) {
			return handleRedirectUrisRequest(requestParts);
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void performAuthorizationFlow() {
		// Not used in this test
	}

	private Object handleRedirectUrisRequest(JsonObject requestParts) {
		JsonArray value = env.getObject("sector_redirect_uris").get("value").getAsJsonArray();
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(StreamSupport.stream(
						Spliterators.spliterator(
								value.iterator(),
								value.size(),
								Spliterator.ORDERED),
						false)
						.map(OIDFJSON::getString)
						.collect(Collectors.toList()));
	}
}
