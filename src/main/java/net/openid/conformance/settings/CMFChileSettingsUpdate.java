package net.openid.conformance.settings;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * The body of a PUT to the Chile CMF settings section. It replaces the whole section, except that
 * secrets which are absent keep their stored values (see {@link SettingsSecretMerge}).
 *
 * @param clientSecret null or empty to keep the stored secret
 * @param clearClientSecret removes the stored secret, and wins over {@code clientSecret}
 * @param clientJwks the JWKS as a JSON string; null to keep the stored JWKS
 * @param clearClientJwks removes the stored JWKS, and wins over {@code clientJwks}
 */
public record CMFChileSettingsUpdate(
	String directoryTokenEndpoint,
	String softwareStatementEndpoint,
	String clientId,
	String clientSecret,
	boolean clearClientSecret,
	String clientJwks,
	boolean clearClientJwks,
	List<CertificateEntryUpdate> positiveCertificates,
	List<CertificateEntryUpdate> negativeCertificates) {

	public static final String CLEAR_CLIENT_SECRET = "clearClientSecret";
	public static final String CLEAR_CLIENT_JWKS = "clearClientJwks";

	public CMFChileSettingsUpdate {
		positiveCertificates = positiveCertificates == null ? List.of() : List.copyOf(positiveCertificates);
		negativeCertificates = negativeCertificates == null ? List.of() : List.copyOf(negativeCertificates);
	}

	/**
	 * @throws IllegalArgumentException naming the field whose JSON type is wrong
	 */
	public static CMFChileSettingsUpdate fromJson(JsonObject body) {
		return new CMFChileSettingsUpdate(
			optionalString(body, CMFChileSettings.DIRECTORY_TOKEN_ENDPOINT, CMFChileSettings.DIRECTORY_TOKEN_ENDPOINT),
			optionalString(body, CMFChileSettings.SOFTWARE_STATEMENT_ENDPOINT, CMFChileSettings.SOFTWARE_STATEMENT_ENDPOINT),
			optionalString(body, CMFChileSettings.CLIENT_ID, CMFChileSettings.CLIENT_ID),
			optionalString(body, CMFChileSettings.CLIENT_SECRET, CMFChileSettings.CLIENT_SECRET),
			optionalBoolean(body, CLEAR_CLIENT_SECRET),
			optionalJwks(body),
			optionalBoolean(body, CLEAR_CLIENT_JWKS),
			entries(body, CMFChileSettings.POSITIVE_CERTIFICATES),
			entries(body, CMFChileSettings.NEGATIVE_CERTIFICATES));
	}

	private static boolean isAbsent(JsonElement element) {
		return element == null || element.isJsonNull();
	}

	private static String optionalString(JsonObject object, String name, String path) {
		JsonElement element = object.get(name);
		if (isAbsent(element)) {
			return null;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
			throw new IllegalArgumentException("'" + path + "' must be a string");
		}
		return OIDFJSON.getString(element);
	}

	private static boolean optionalBoolean(JsonObject object, String name) {
		JsonElement element = object.get(name);
		if (isAbsent(element)) {
			return false;
		}
		if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
			throw new IllegalArgumentException("'" + name + "' must be a boolean");
		}
		return OIDFJSON.getBoolean(element);
	}

	private static String optionalJwks(JsonObject body) {
		JsonElement element = body.get(CMFChileSettings.CLIENT_JWKS);
		if (isAbsent(element)) {
			return null;
		}
		if (!element.isJsonObject()) {
			throw new IllegalArgumentException("'" + CMFChileSettings.CLIENT_JWKS + "' must be a JSON object");
		}
		return element.toString();
	}

	private static List<CertificateEntryUpdate> entries(JsonObject body, String name) {
		JsonElement element = body.get(name);
		if (isAbsent(element)) {
			return List.of();
		}
		if (!element.isJsonArray()) {
			throw new IllegalArgumentException("'" + name + "' must be an array");
		}
		JsonArray array = element.getAsJsonArray();
		List<CertificateEntryUpdate> entries = new ArrayList<>();
		for (int i = 0; i < array.size(); i++) {
			String path = name + "[" + i + "]";
			if (!array.get(i).isJsonObject()) {
				throw new IllegalArgumentException("'" + path + "' must be an object");
			}
			JsonObject entry = array.get(i).getAsJsonObject();
			entries.add(new CertificateEntryUpdate(
				optionalString(entry, "id", path + ".id"),
				optionalString(entry, "label", path + ".label"),
				optionalString(entry, "certificateChainPem", path + ".certificateChainPem"),
				optionalString(entry, "privateKeyPem", path + ".privateKeyPem")));
		}
		return entries;
	}

	@Override
	public String toString() {
		// the generated toString would print the client secret, the JWKS and private keys
		return "CMFChileSettingsUpdate[directoryTokenEndpoint=" + directoryTokenEndpoint
			+ ", softwareStatementEndpoint=" + softwareStatementEndpoint
			+ ", clientId=" + clientId
			+ ", clearClientSecret=" + clearClientSecret
			+ ", clearClientJwks=" + clearClientJwks
			+ ", positiveCertificates=" + positiveCertificates
			+ ", negativeCertificates=" + negativeCertificates + "]";
	}
}
