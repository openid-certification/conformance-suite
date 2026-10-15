package net.openid.conformance.util;

import com.google.common.base.Suppliers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.jwk.ECKey;
import net.openid.conformance.oauth.statuslists.JwtStatusListTokenClaimsBuilder;
import net.openid.conformance.oauth.statuslists.TokenStatusList;
import net.openid.conformance.testmodule.OIDFJSON;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * The suite's wallet-relying-party registration certificate (WRPRC, ETSI TS 119 475), which it sends
 * in {@code verifier_info} as an OpenID4VP verifier, and the Token Status List its {@code status}
 * points to. There is one certificate per deployment, registering the suite as a service provider
 * for one intended use whose {@code credentials} are the union of every built-in DCQL query the suite
 * sends, so a wallet's over-asking check passes for each of them. It is bound to the suite's access
 * certificate by {@code sub}, which equals that certificate's organizationIdentifier (TS 119 475
 * GEN-5.1.1-02), and signed by {@link TestKeysAndCerts#getWrprcSigner()}.
 *
 * <p>The supervisory authority is the OpenID Foundation's own contact: the Foundation is not
 * established in the EU, and the certificate exists for conformance testing.
 */
public final class SuiteWrprc {

	/** Where the suite serves the status list, under its https base URL. */
	public static final String STATUS_LIST_PATH = "/status-lists/wrprc.jwt";

	public static final String INTENDED_USE_ID = "oidf-conformance-testing";

	// TS 119 475 clause 6.1.3: the policy a registration certificate is issued under
	private static final String POLICY_ID = "0.4.0.19475.3.1";
	private static final String SERVICE_PROVIDER_ENTITLEMENT = "https://uri.etsi.org/19475/Entitlement/Service_Provider";
	private static final String LEI_IDENTIFIER_TYPE = "http://data.europa.eu/eudi/id/LEI";
	private static final String PRIVACY_POLICY_URI = "https://openid.net/policies/";
	private static final String TRADE_NAME = "OpenID Foundation Conformance Suite";
	private static final String SERVICE_DESCRIPTION = "OpenID Foundation conformance testing of wallets";
	private static final String PURPOSE = "Conformance testing of the presentation of credentials";

	private static final Duration VALIDITY = Duration.ofDays(30);
	private static final Duration STATUS_LIST_VALIDITY = Duration.ofHours(24);
	private static final long STATUS_LIST_TTL_SECONDS = TimeUnit.HOURS.toSeconds(1);

	private static final String BUILT_IN_DCQL_PATTERN = "classpath*:json/dcql/vp1final-wallet-*.json";

	private static final Supplier<String> JWT = Suppliers.memoizeWithExpiration(() -> jwt(Instant.now()), 1, TimeUnit.HOURS);
	private static final Supplier<String> STATUS_LIST = Suppliers.memoizeWithExpiration(() -> statusListJwt(Instant.now()), 1, TimeUnit.HOURS);
	// the built-in DCQL queries are classpath resources, so they cannot change while the JVM runs
	private static final Supplier<JsonArray> CREDENTIALS = Suppliers.memoize(SuiteWrprc::credentialsOfBuiltInQueries);

	private SuiteWrprc() {
	}

	/** The registration certificate in use, a compact JWS. */
	public static String jwt() {
		return JWT.get();
	}

	/** The status list in use, a compact JWS. */
	public static String statusListJwt() {
		return STATUS_LIST.get();
	}

	/** The URL the suite serves the status list at. */
	public static String statusListUrl() {
		return TestKeysAndCerts.getHttpsBaseUrl() + STATUS_LIST_PATH;
	}

	/**
	 * The {@code verifier_info} value carrying the certificate: the registrar dataset element of ETSI
	 * TS 119 472-2 OIDFVP-HAIP-COMMON-REQ-RO-02 to RO-12 and the registration certificate element of
	 * RO-13 to RO-16, whose data is the base64url encoding of the compact JWS.
	 */
	public static JsonArray verifierInfo() {
		JsonObject dataset = new JsonObject();
		JsonArray identifiers = new JsonArray();
		JsonObject identifier = new JsonObject();
		identifier.addProperty("type", LEI_IDENTIFIER_TYPE);
		identifier.addProperty("identifier", lei());
		identifiers.add(identifier);
		dataset.add("identifier", identifiers);
		dataset.add("srvDescription", LoteBuilder.inEnglish(SERVICE_DESCRIPTION, "content"));
		dataset.addProperty("registryURI", registryUri());
		dataset.addProperty("intendedUseIdentifier", INTENDED_USE_ID);
		dataset.add("purpose", LoteBuilder.inEnglish(PURPOSE, "content"));
		dataset.addProperty("policyURI", PRIVACY_POLICY_URI);
		dataset.add("credential", credentials());

		JsonObject datasetElement = new JsonObject();
		datasetElement.addProperty("format", "registrar_dataset");
		datasetElement.add("data", dataset);

		JsonObject certificateElement = new JsonObject();
		certificateElement.addProperty("format", "registration_cert");
		certificateElement.addProperty("data",
			Base64.getUrlEncoder().withoutPadding().encodeToString(jwt().getBytes(StandardCharsets.US_ASCII)));

		JsonArray verifierInfo = new JsonArray();
		verifierInfo.add(datasetElement);
		verifierInfo.add(certificateElement);
		return verifierInfo;
	}

	/** The registration certificate issued at {@code issued}. */
	public static String jwt(Instant issued) {
		return sign(new JOSEObjectType("rc-wrp+jwt"), payload(issued));
	}

	/** The payload of the registration certificate issued at {@code issued}: TS 119 475 Tables 7, 9 and 10. */
	public static JsonObject payload(Instant issued) {
		Instant iat = issued.truncatedTo(ChronoUnit.SECONDS);
		JsonObject payload = new JsonObject();
		payload.addProperty("name", TRADE_NAME);
		payload.addProperty("sub_ln", TestKeysAndCerts.ORGANIZATION_NAME);
		payload.addProperty("sub", TestKeysAndCerts.ORGANIZATION_IDENTIFIER);
		payload.addProperty("country", "US");
		payload.addProperty("registry_uri", registryUri());
		payload.add("srv_description", LoteBuilder.inEnglish(SERVICE_DESCRIPTION, "value"));
		JsonArray entitlements = new JsonArray();
		entitlements.add(SERVICE_PROVIDER_ENTITLEMENT);
		payload.add("entitlements", entitlements);
		payload.addProperty("privacy_policy", PRIVACY_POLICY_URI);
		payload.addProperty("info_uri", TestKeysAndCerts.CONTACT_URL);
		payload.addProperty("support_uri", TestKeysAndCerts.CONTACT_URL);
		JsonObject supervisoryAuthority = new JsonObject();
		supervisoryAuthority.addProperty("email", TestKeysAndCerts.CONTACT_EMAIL);
		supervisoryAuthority.addProperty("uri", TestKeysAndCerts.CONTACT_URL);
		payload.add("supervisory_authority", supervisoryAuthority);
		JsonArray policyIds = new JsonArray();
		policyIds.add(POLICY_ID);
		payload.add("policy_id", policyIds);
		payload.addProperty("certificate_policy", TestKeysAndCerts.CONTACT_URL);
		payload.addProperty("public_body", false);
		payload.addProperty("iat", iat.getEpochSecond());
		payload.addProperty("exp", iat.plus(VALIDITY).getEpochSecond());
		JsonObject statusList = new JsonObject();
		statusList.addProperty("idx", 0);
		statusList.addProperty("uri", statusListUrl());
		JsonObject status = new JsonObject();
		status.add("status_list", statusList);
		payload.add("status", status);
		payload.addProperty("intended_use_id", INTENDED_USE_ID);
		payload.add("purpose", LoteBuilder.inEnglish(PURPOSE, "value"));
		payload.add("credentials", credentials());
		return payload;
	}

	/** The status list issued at {@code issued}: one 1-bit entry, index 0, set to VALID. */
	public static String statusListJwt(Instant issued) {
		Instant iat = issued.truncatedTo(ChronoUnit.SECONDS);
		JsonObject payload = JwtStatusListTokenClaimsBuilder.build(statusListUrl(), iat, iat.plus(STATUS_LIST_VALIDITY),
			STATUS_LIST_TTL_SECONDS, 1, TokenStatusList.create(new byte[]{0}, 1).encodeStatusList(), null);
		return sign(new JOSEObjectType(JwtStatusListTokenClaimsBuilder.TYP), payload);
	}

	/**
	 * Every credential the built-in DCQL queries request, as TS 119 475 Credential objects: one per
	 * format and {@code meta}, with the union of the claim paths requested for it. A copy each time.
	 */
	public static JsonArray credentials() {
		return CREDENTIALS.get().deepCopy();
	}

	private static JsonArray credentialsOfBuiltInQueries() {
		Map<String, JsonObject> metaByKey = new LinkedHashMap<>();
		Map<String, String> formatByKey = new LinkedHashMap<>();
		Map<String, Set<String>> pathsByKey = new LinkedHashMap<>();
		for (JsonObject query : builtInDcqlQueries()) {
			for (JsonElement element : query.getAsJsonArray("credentials")) {
				JsonObject credential = element.getAsJsonObject();
				String format = OIDFJSON.getString(credential.get("format"));
				JsonObject meta = credential.getAsJsonObject("meta");
				String key = format + " " + meta;
				formatByKey.putIfAbsent(key, format);
				metaByKey.putIfAbsent(key, meta);
				Set<String> paths = pathsByKey.computeIfAbsent(key, k -> new LinkedHashSet<>());
				JsonArray claims = credential.getAsJsonArray("claims");
				if (claims != null) {
					for (JsonElement claim : claims) {
						paths.add(claim.getAsJsonObject().get("path").toString());
					}
				}
			}
		}

		JsonArray credentials = new JsonArray();
		for (Map.Entry<String, Set<String>> entry : pathsByKey.entrySet()) {
			JsonObject credential = new JsonObject();
			credential.addProperty("format", formatByKey.get(entry.getKey()));
			credential.add("meta", metaByKey.get(entry.getKey()).deepCopy());
			JsonArray claim = new JsonArray();
			for (String path : entry.getValue()) {
				JsonObject item = new JsonObject();
				item.add("path", JsonParser.parseString(path));
				claim.add(item);
			}
			credential.add("claim", claim);
			credentials.add(credential);
		}
		return credentials;
	}

	/** The built-in DCQL queries of the OpenID4VP wallet tests, from the classpath. */
	static List<JsonObject> builtInDcqlQueries() {
		try {
			Resource[] resources = new PathMatchingResourcePatternResolver().getResources(BUILT_IN_DCQL_PATTERN);
			List<JsonObject> queries = new ArrayList<>();
			Arrays.sort(resources, Comparator.comparing(Resource::getFilename));
			for (Resource resource : resources) {
				try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
					queries.add(JsonParser.parseReader(reader).getAsJsonObject());
				}
			}
			if (queries.isEmpty()) {
				throw new IllegalStateException("No built-in DCQL queries match " + BUILT_IN_DCQL_PATTERN);
			}
			return queries;
		} catch (IOException e) {
			throw new IllegalStateException("Failed to read the built-in DCQL queries", e);
		}
	}

	private static String sign(JOSEObjectType type, JsonObject payload) {
		ECKey key = TestKeysAndCerts.getWrprcSigner().getJwk();
		JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
			.type(type)
			.x509CertChain(key.getX509CertChain())
			.build();
		return JWTUtil.signCompact(header, payload, key);
	}

	private static String registryUri() {
		return TestKeysAndCerts.getTrustAnchorsUrl();
	}

	private static String lei() {
		String organizationIdentifier = TestKeysAndCerts.ORGANIZATION_IDENTIFIER;
		return organizationIdentifier.substring(organizationIdentifier.indexOf('-') + 1);
	}
}
