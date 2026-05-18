package net.openid.conformance.info;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMigration_UnitTest {

	@Test
	public void nullConfigIsNoOp() {
		ConfigMigration.migrateLegacyClientAttestationKeys(null);
	}

	@Test
	public void emptyConfigIsUntouched() {
		JsonObject config = new JsonObject();
		ConfigMigration.migrateLegacyClientAttestationKeys(config);
		assertThat(config.entrySet()).isEmpty();
	}

	@Test
	public void legacyKeyIsMovedToNewNamespace() {
		JsonObject config = parse("""
			{
				"vci": {
					"client_attestation_issuer": "https://attester.example.org/"
				}
			}
			""");

		ConfigMigration.migrateLegacyClientAttestationKeys(config);

		assertThat(config.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(config.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
	}

	@Test
	public void unrelatedVciKeysAreKept() {
		JsonObject config = parse("""
			{
				"vci": {
					"credential_offer_endpoint": "haip-vci://",
					"client_attestation_issuer": "https://attester.example.org/"
				}
			}
			""");

		ConfigMigration.migrateLegacyClientAttestationKeys(config);

		assertThat(OIDFJSON.getString(config.getAsJsonObject("vci")
			.get("credential_offer_endpoint"))).isEqualTo("haip-vci://");
		assertThat(OIDFJSON.getString(config.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://attester.example.org/");
	}

	@Test
	public void existingNewKeyWinsAndLegacyIsDropped() {
		JsonObject config = parse("""
			{
				"vci": {
					"client_attestation_issuer": "https://legacy.example/"
				},
				"client_attestation": {
					"issuer": "https://new.example/"
				}
			}
			""");

		ConfigMigration.migrateLegacyClientAttestationKeys(config);

		assertThat(config.has("vci")).isFalse();
		assertThat(OIDFJSON.getString(config.getAsJsonObject("client_attestation")
			.get("issuer"))).isEqualTo("https://new.example/");
	}

	@Test
	public void allFiveLegacyKeysAreMigrated() {
		JsonObject config = parse("""
			{
				"vci": {
					"client_attestation_issuer": "issuer",
					"client_attestation_trust_anchor": "trust",
					"key_attestation_trust_anchor_pem": "key-trust",
					"client_attester_keys_jwks": {"keys": []},
					"key_attestation_jwks": {"keys": [{"kid": "k"}]}
				}
			}
			""");

		ConfigMigration.migrateLegacyClientAttestationKeys(config);

		assertThat(config.has("vci")).isFalse();
		JsonObject ca = config.getAsJsonObject("client_attestation");
		assertThat(OIDFJSON.getString(ca.get("issuer"))).isEqualTo("issuer");
		assertThat(OIDFJSON.getString(ca.get("trust_anchor"))).isEqualTo("trust");
		assertThat(OIDFJSON.getString(ca.get("key_attestation_trust_anchor_pem"))).isEqualTo("key-trust");
		assertThat(ca.getAsJsonObject("attester_jwks").has("keys")).isTrue();
		assertThat(ca.getAsJsonObject("key_attestation_jwks").getAsJsonArray("keys")).hasSize(1);
	}

	@Test
	public void vciAsNonObjectIsLeftAlone() {
		JsonObject config = parse("""
			{ "vci": "not-an-object" }
			""");
		ConfigMigration.migrateLegacyClientAttestationKeys(config);
		assertThat(OIDFJSON.getString(config.get("vci"))).isEqualTo("not-an-object");
	}

	private static JsonObject parse(String json) {
		return JsonParser.parseString(json).getAsJsonObject();
	}
}
