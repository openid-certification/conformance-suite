package net.openid.conformance.info;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.CollapsingGsonHttpMessageConverter;
import net.openid.conformance.testmodule.OIDFJSON;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMigratingResponse_UnitTest {

	private final Gson gson = CollapsingGsonHttpMessageConverter.getDbObjectCollapsingGson();

	@Test
	public void wrapperTriggersMigrationOnNestedConfigInJsonObject() {
		JsonObject inner = new JsonObject();
		JsonObject config = new JsonObject();
		JsonObject vci = new JsonObject();
		vci.addProperty("client_attestation_issuer", "https://attester.example.org/");
		config.add("vci", vci);
		inner.add("config", config);
		inner.addProperty("planName", "some-plan");

		String json = gson.toJson(new ConfigMigratingResponse(inner));
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		JsonObject migratedConfig = result.getAsJsonObject("config");
		assertThat(migratedConfig.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(migratedConfig.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
		assertThat(OIDFJSON.getString(result.get("planName"))).isEqualTo("some-plan");
	}

	@Test
	public void wrapperTriggersMigrationOnNestedConfigInBsonDocument() {
		// SavedConfigurationApi returns a raw org.bson.Document; ensure the migration
		// runs after the Document-collapsing serializer has already lowered the BSON
		// shape into JsonObject.
		Document inner = new Document();
		Document config = new Document();
		Document vci = new Document();
		vci.append("client_attestation_issuer", "https://attester.example.org/");
		config.append("vci", vci);
		inner.append("config", config);
		inner.append("planName", "some-plan");

		String json = gson.toJson(new ConfigMigratingResponse(inner));
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		JsonObject migratedConfig = result.getAsJsonObject("config");
		assertThat(migratedConfig.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(migratedConfig.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
	}

	@Test
	public void wrapperWithNoConfigFieldPassesThroughUnchanged() {
		JsonObject inner = new JsonObject();
		inner.addProperty("planName", "no-config-here");

		String json = gson.toJson(new ConfigMigratingResponse(inner));
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		assertThat(OIDFJSON.getString(result.get("planName"))).isEqualTo("no-config-here");
		assertThat(result.has("config")).isFalse();
	}

	@Test
	public void wrapperWithNoLegacyKeysIsNoOp() {
		JsonObject inner = new JsonObject();
		JsonObject config = new JsonObject();
		JsonObject ca = new JsonObject();
		ca.addProperty("issuer", "https://attester.example.org/");
		config.add("client_attestation", ca);
		inner.add("config", config);

		String json = gson.toJson(new ConfigMigratingResponse(inner));
		JsonObject result = JsonParser.parseString(json).getAsJsonObject();

		JsonObject resultConfig = result.getAsJsonObject("config");
		assertThat(resultConfig.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(resultConfig.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
	}
}
