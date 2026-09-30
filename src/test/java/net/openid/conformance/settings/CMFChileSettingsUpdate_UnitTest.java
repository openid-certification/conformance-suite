package net.openid.conformance.settings;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CMFChileSettingsUpdate_UnitTest {

	private static JsonObject json(String text) {
		return JsonParser.parseString(text).getAsJsonObject();
	}

	@Test
	public void readsEveryField() {
		CMFChileSettingsUpdate update = CMFChileSettingsUpdate.fromJson(json("""
			{
			  "directoryTokenEndpoint": "https://directory.example.cl/token",
			  "clientId": "abc",
			  "clientSecret": "s3cret",
			  "clearClientSecret": true,
			  "clientJwks": {"keys": []},
			  "positiveCertificates": [
			    {"id": "pos-1", "label": "primary", "certificateChainPem": "CERT", "privateKeyPem": "KEY"}
			  ],
			  "negativeCertificates": [
			    {"label": "expired", "certificateChainPem": "CERT2"}
			  ]
			}"""));

		assertThat(update.directoryTokenEndpoint()).isEqualTo("https://directory.example.cl/token");
		assertThat(update.clientId()).isEqualTo("abc");
		assertThat(update.clientSecret()).isEqualTo("s3cret");
		assertThat(update.clearClientSecret()).isTrue();
		assertThat(JsonParser.parseString(update.clientJwks())).isEqualTo(json("{\"keys\": []}"));
		assertThat(update.positiveCertificates())
			.containsExactly(new CertificateEntryUpdate("pos-1", "primary", "CERT", "KEY"));
		assertThat(update.negativeCertificates())
			.containsExactly(new CertificateEntryUpdate(null, "expired", "CERT2", null));
	}

	@Test
	public void absentFieldsAreNullFalseOrEmpty() {
		CMFChileSettingsUpdate update = CMFChileSettingsUpdate.fromJson(json("{}"));

		assertThat(update.directoryTokenEndpoint()).isNull();
		assertThat(update.clientSecret()).isNull();
		assertThat(update.clearClientSecret()).isFalse();
		assertThat(update.clientJwks()).isNull();
		assertThat(update.positiveCertificates()).isEmpty();
		assertThat(update.negativeCertificates()).isEmpty();
	}

	@Test
	public void jsonNullIsTreatedAsAbsent() {
		CMFChileSettingsUpdate update = CMFChileSettingsUpdate.fromJson(json(
			"{\"clientId\": null, \"clientJwks\": null, \"positiveCertificates\": null}"));

		assertThat(update.clientId()).isNull();
		assertThat(update.clientJwks()).isNull();
		assertThat(update.positiveCertificates()).isEmpty();
	}

	@Test
	public void rejectsANonStringField() {
		assertThatThrownBy(() -> CMFChileSettingsUpdate.fromJson(json("{\"clientId\": 7}")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("clientId");
	}

	@Test
	public void rejectsANonBooleanClearFlag() {
		assertThatThrownBy(() -> CMFChileSettingsUpdate.fromJson(json("{\"clearClientSecret\": \"yes\"}")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("clearClientSecret");
	}

	@Test
	public void rejectsAJwksThatIsNotAnObject() {
		assertThatThrownBy(() -> CMFChileSettingsUpdate.fromJson(json("{\"clientJwks\": \"{}\"}")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("clientJwks");
	}

	@Test
	public void rejectsACertificateListThatIsNotAnArray() {
		assertThatThrownBy(() -> CMFChileSettingsUpdate.fromJson(json("{\"negativeCertificates\": {}}")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("negativeCertificates");
	}

	@Test
	public void rejectsACertificateEntryThatIsNotAnObject() {
		assertThatThrownBy(() -> CMFChileSettingsUpdate.fromJson(json("{\"positiveCertificates\": [\"x\"]}")))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("positiveCertificates[0]");
	}

	@Test
	public void toStringOmitsSecrets() {
		CMFChileSettingsUpdate update = CMFChileSettingsUpdate.fromJson(json("""
			{"clientSecret": "s3cret", "positiveCertificates": [{"label": "a", "privateKeyPem": "THE-KEY"}]}"""));

		assertThat(update.toString()).doesNotContain("s3cret").doesNotContain("THE-KEY");
	}
}
