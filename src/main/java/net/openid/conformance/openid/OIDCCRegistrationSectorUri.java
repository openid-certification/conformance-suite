package net.openid.conformance.openid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.openid.conformance.condition.client.AddSectorIdentifierUriToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.AddSubjectTypePairwiseToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CreateSectorRedirectUris;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@PublishTestModule(
	testName = "oidcc-registration-sector-uri",
	displayName = "OIDCC: dynamic registration",
	summary = "This test calls the dynamic registration endpoint with a sector_identifier_uri pointing to a document containing the test's redirect URI. This should result in a successful registration.",
	profile = "OIDCC"
)
public class OIDCCRegistrationSectorUri extends AbstractOIDCCDynamicRegistrationTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		JsonObject server = env.getObject("server");
		JsonElement subjectTypesSupported = server.get("subject_types_supported");
		if (subjectTypesSupported == null
				|| !subjectTypesSupported.isJsonArray()
				|| !subjectTypesSupported.getAsJsonArray().contains(new JsonPrimitive("pairwise"))) {
			fireTestSkipped("Server configuration does not explicitly support pairwise subject type");
		}

		callAndStopOnFailure(CreateSectorRedirectUris.class);
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
			return handleRedirectUrisRequest();
		} else {
			return super.handleHttp(path, req, res, session, requestParts);
		}
	}

	@Override
	protected void performAuthorizationFlow() {
		// Don't need to test authorization here.
		fireTestFinished();
	}

	private Object handleRedirectUrisRequest() {
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
