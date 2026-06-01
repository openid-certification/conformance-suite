package net.openid.conformance.info;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SavedConfigurationApi_UnitTest {

	private SavedConfigurationService service;
	private SavedConfigurationApi api;
	private final Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	@BeforeEach
	public void setUp() {
		service = Mockito.mock(SavedConfigurationService.class);
		api = new SavedConfigurationApi();
		ReflectionTestUtils.setField(api, "savedConfigurationService", service);
	}

	@Test
	public void lastConfigWithNoStoredDocumentReturnsEmptyJsonObject() {
		Mockito.when(service.getLastConfigForCurrentUser()).thenReturn(null);

		ResponseEntity<Object> response = api.getLastConfig();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(JsonObject.class);
		assertThat(((JsonObject) response.getBody()).entrySet()).isEmpty();
	}

	@Test
	public void lastConfigWithLegacyKeysIsMigratedDuringSerialization() {
		Document config = new Document();
		Document vci = new Document();
		vci.append("client_attestation_issuer", "https://attester.example.org/");
		config.append("vci", vci);

		Document stored = new Document();
		stored.append("planName", "oid4vci-1_0-wallet-test-plan");
		stored.append("config", config);

		Mockito.when(service.getLastConfigForCurrentUser()).thenReturn(stored);

		ResponseEntity<Object> response = api.getLastConfig();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(ConfigMigratingResponse.class);

		// Drive the body through the same Gson that Spring uses for the response — the
		// migration only happens during this serialization pass.
		String json = gson.toJson(response.getBody());
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		JsonObject migratedConfig = result.getAsJsonObject("config");
		assertThat(migratedConfig.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(migratedConfig.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
		assertThat(OIDFJSON.getString(result.get("planName"))).isEqualTo("oid4vci-1_0-wallet-test-plan");
	}

	@Test
	public void lastConfigWithoutLegacyKeysIsUnchanged() {
		Document config = new Document();
		Document ca = new Document();
		ca.append("issuer", "https://attester.example.org/");
		config.append("client_attestation", ca);

		Document stored = new Document();
		stored.append("config", config);

		Mockito.when(service.getLastConfigForCurrentUser()).thenReturn(stored);

		ResponseEntity<Object> response = api.getLastConfig();

		String json = gson.toJson(response.getBody());
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		JsonObject resultConfig = result.getAsJsonObject("config");
		assertThat(resultConfig.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(resultConfig.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
	}
}
