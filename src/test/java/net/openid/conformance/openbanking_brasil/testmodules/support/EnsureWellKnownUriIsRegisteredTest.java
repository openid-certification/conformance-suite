package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.openid.conformance.apis.AbstractJsonResponseConditionUnitTest;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.util.JsonLoadingJUnitRunner;
import net.openid.conformance.util.UseResurce;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsString;

public class EnsureWellKnownUriIsRegisteredTest extends AbstractJsonResponseConditionUnitTest {

	private EnsureWellKnownUriIsRegistered condition;

	@Before
	public void init() {
		condition = new EnsureWellKnownUriIsRegistered();
		environment.putObject("directory_participants_response_full", new JsonObject());

	}

	private void setupParticipantsResponse() {
		Gson gson = new GsonBuilder()
			.serializeNulls()
			.create();
		environment.putString("directory_participants_response_full", "body", gson.toJson(jsonObject.getAsJsonArray(JsonLoadingJUnitRunner.WRAPPED)));
	}


	@Test
	@UseResurce("jsonResponses/participants/goodParticipantsResponse.json")
	public void validateWellKnownWithGoodParticipantsResponseAndPresentWellKnown() {
		setupParticipantsResponse();
		List.of("WellKnown URL 1", "WellKnown URL 2", "WellKnown URL 3", "WellKnown URL 4").forEach(uri -> {
			environment.putString("config", "server.discoveryUrl", uri);
			run(condition);
		});

	}

	@Test
	@UseResurce("jsonResponses/participants/goodParticipantsResponse.json")
	public void validateWellKnownWithGoodParticipantsResponseAndPMissingWellKnown() {
		setupParticipantsResponse();
		environment.putString("config", "server.discoveryUrl", "Blah");
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(),
			containsString("Could not find Authorisation Server with provided Well-Known URL in the Directory Participants List"));
	}

	@Test
	@UseResurce("jsonResponses/participants/badParticipantsResponse1.json")
	public void validateWellKnownWithBadParticipantsResponse1() {
		setupParticipantsResponse();
		environment.putString("config", "server.discoveryUrl", "Blah");
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(),
			containsString("Could not find AuthorisationServers JSON array in the organisation object"));
	}

	@Test
	@UseResurce("jsonResponses/participants/badParticipantsResponse2.json")
	public void validateWellKnownWithBadParticipantsResponse2() {
		setupParticipantsResponse();
		environment.putString("config", "server.discoveryUrl", "Blah");
		ConditionError error = runAndFail(condition);
		Assert.assertThat(error.getMessage(),
			containsString("Could not find OpenIDDiscoveryDocument JSON element in the AuthorisationServer object"));
	}

}
