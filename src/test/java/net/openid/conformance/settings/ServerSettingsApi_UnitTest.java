package net.openid.conformance.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.security.AuthenticationFacade;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ServerSettingsApi_UnitTest {

	private static final CMFChileSettings STORED = new CMFChileSettings("https://directory.example.cl/token", "abc",
		"the-client-secret", null, List.of(), List.of(), null, null);

	private ServerSettingsService service;
	private AuthenticationFacade authenticationFacade;
	private ServerSettingsApi api;

	@BeforeEach
	public void setUp() {
		service = Mockito.mock(ServerSettingsService.class);
		authenticationFacade = Mockito.mock(AuthenticationFacade.class);
		api = new ServerSettingsApi(service, authenticationFacade);
	}

	private void signedInAsAdmin() {
		Mockito.when(authenticationFacade.isAdmin()).thenReturn(true);
		Mockito.when(authenticationFacade.getDisplayName()).thenReturn("Admin User");
	}

	private static JsonObject body(ResponseEntity<Object> response) {
		return (JsonObject) response.getBody();
	}

	@Test
	public void aNonAdminCannotRead() {
		ResponseEntity<Object> response = api.getCMFChileSettings();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		Mockito.verifyNoInteractions(service);
	}

	@Test
	public void aNonAdminCannotSave() {
		ResponseEntity<Object> response = api.putCMFChileSettings(new JsonObject());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		Mockito.verifyNoInteractions(service);
	}

	@Test
	public void anAdminReadsTheRedactedView() {
		signedInAsAdmin();
		Mockito.when(service.getCMFChileSettings()).thenReturn(STORED);

		ResponseEntity<Object> response = api.getCMFChileSettings();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(OIDFJSON.getString(body(response).get("clientId"))).isEqualTo("abc");
		assertThat(OIDFJSON.getBoolean(body(response).get("clientSecretSet"))).isTrue();
		assertThat(body(response).toString()).doesNotContain("the-client-secret");
	}

	@Test
	public void aBodyWithAWrongTypeIsABadRequest() {
		signedInAsAdmin();

		ResponseEntity<Object> response = api.putCMFChileSettings(JsonParser.parseString("{\"clientId\": 7}").getAsJsonObject());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		JsonObject error = body(response).getAsJsonArray("errors").get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(error.get("field"))).isEmpty();
		assertThat(OIDFJSON.getString(error.get("message"))).contains("clientId");
		Mockito.verifyNoInteractions(service);
	}

	@Test
	public void validationErrorsAreABadRequestListingEachField() {
		signedInAsAdmin();
		Mockito.when(service.saveCMFChileSettings(any(), eq("Admin User"))).thenReturn(new ServerSettingsService.SaveResult(
			STORED, List.of(new SettingsError("directoryTokenEndpoint", "'Directory token endpoint URL' must be an absolute https:// URL"))));

		ResponseEntity<Object> response = api.putCMFChileSettings(new JsonObject());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		JsonObject error = body(response).getAsJsonArray("errors").get(0).getAsJsonObject();
		assertThat(OIDFJSON.getString(error.get("field"))).isEqualTo("directoryTokenEndpoint");
		assertThat(OIDFJSON.getString(error.get("message"))).contains("https://");
	}

	@Test
	public void aSuccessfulSaveReturnsTheRedactedViewOfWhatWasSaved() {
		signedInAsAdmin();
		Mockito.when(service.saveCMFChileSettings(any(), eq("Admin User")))
			.thenReturn(new ServerSettingsService.SaveResult(STORED, List.of()));

		ResponseEntity<Object> response = api.putCMFChileSettings(
			JsonParser.parseString("{\"clientId\": \"abc\", \"clientSecret\": \"the-client-secret\"}").getAsJsonObject());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(OIDFJSON.getString(body(response).get("clientId"))).isEqualTo("abc");
		assertThat(body(response).toString()).doesNotContain("the-client-secret");
	}
}
