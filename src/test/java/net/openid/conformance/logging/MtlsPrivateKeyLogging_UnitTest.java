package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.ExtractMTLSCertificates2FromConfiguration;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.condition.client.GenerateMTLSCertificateFromJWKs;
import net.openid.conformance.condition.client.SaveMutualTLsAuthenticationToConfig;
import net.openid.conformance.condition.client.ValidateMTLSCertificatesAsX509;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import java.net.URI;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class MtlsPrivateKeyLogging_UnitTest {

	private static final String PRIVATE_KEY = "cHJpdmF0ZS1rZXktdGVzdA==";
	private static final String CERTIFICATE = "Y2VydGlmaWNhdGU=";
	private final List<String> entries = new ArrayList<>();

	@ParameterizedTest
	@ValueSource(strings = { "mtls", "mtls2" })
	public void credentialExtractionLogsCertificateButNotPrivateKey(String section) {
		Environment env = environment(section);
		JsonObject originalConfig = env.getObject("config").deepCopy();
		extractor(section).execute(env);

		assertThat(String.join("\n", entries)).contains(CERTIFICATE).doesNotContain(PRIVATE_KEY);
		assertThat(env.getObject("config")).isEqualTo(originalConfig);
	}

	@ParameterizedTest
	@ValueSource(strings = { "mtls", "mtls2" })
	public void invalidCertificateChainDoesNotExposePrivateKey(String section) {
		Environment env = environment(section);
		env.putString("config", section + ".ca", "!invalid-base64!");
		AbstractCondition condition = extractor(section);

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
		assertThat(String.join("\n", entries)).contains("Couldn't decode").doesNotContain(PRIVATE_KEY);
	}

	@Test
	public void outboundRequestLogsCertificateWithoutMutatingCredentials() throws Exception {
		JsonObject credentials = credentials();
		var interceptor = new LoggingRequestInterceptor("UNIT-TEST", capturingLog(), credentials);
		var request = new MockClientHttpRequest(HttpMethod.POST, URI.create("https://client.example/notify"));

		interceptor.intercept(request, new byte[0], (sentRequest, body) ->
			new MockClientHttpResponse(new byte[0], HttpStatus.NO_CONTENT));

		assertThat(String.join("\n", entries)).contains(CERTIFICATE).doesNotContain(PRIVATE_KEY);
		assertThat(credentials).isEqualTo(credentials());
	}

	@Test
	public void finalEnvironmentLogOmitsTlsKeysButPreservesOperationalState() {
		Environment env = environment("mtls");
		env.getObject("config").add("mtls2", credentials());
		env.putObject("mutual_tls_authentication", credentials());
		env.putObjectFromJsonString("client", "{\"key\":\"public-metadata\"}");

		assertThat(env.toString()).contains(CERTIFICATE, "public-metadata").doesNotContain(PRIVATE_KEY);
		assertThat(env.getString("config", "mtls.key")).isEqualTo(PRIVATE_KEY);
		assertThat(env.getString("mutual_tls_authentication", "key")).isEqualTo(PRIVATE_KEY);
	}

	@Test
	public void invalidConfigWithoutCertificateStillHidesItsPrivateKey() {
		Environment env = environment("mtls");
		env.getObject("config").getAsJsonObject("mtls").remove("cert");
		assertThat(env.toString()).doesNotContain(PRIVATE_KEY);
		assertThat(env.getString("config", "mtls.key")).isEqualTo(PRIVATE_KEY);
	}

	@Test
	public void generatedCertificateLogOmitsPrivateKey() throws Exception {
		Environment env = generatedCredentials();
		assertThat(String.join("\n", entries))
			.contains(env.getString("mutual_tls_authentication", "cert"))
			.doesNotContain(env.getString("mutual_tls_authentication", "key"));
	}

	@Test
	public void savingCredentialsKeepsKeyInConfigButNotInLog() {
		Environment env = environment("mtls");
		env.putObject("mutual_tls_authentication", credentials());
		execute(new SaveMutualTLsAuthenticationToConfig(), env);
		assertThat(env.getString("config", "mtls.key")).isEqualTo(PRIVATE_KEY);
		assertThat(String.join("\n", entries)).contains(CERTIFICATE).doesNotContain(PRIVATE_KEY);
	}

	@ParameterizedTest
	@ValueSource(strings = { "!invalid-private-key!", PRIVATE_KEY })
	public void invalidPrivateKeyValidationDoesNotLogKey(String invalidKey) throws Exception {
		Environment env = generatedCredentials();
		env.putString("mutual_tls_authentication", "key", invalidKey);
		entries.clear();
		assertThatThrownBy(() -> execute(new ValidateMTLSCertificatesAsX509(), env))
			.isInstanceOf(ConditionError.class);
		assertThat(String.join("\n", entries)).doesNotContain(invalidKey);
	}

	@Test
	public void mismatchedPrivateKeyValidationLogsOnlyCertificate() throws Exception {
		Environment env = generatedCredentials();
		String otherKey = generatedCredentials().getString("mutual_tls_authentication", "key");
		env.putString("mutual_tls_authentication", "key", otherKey);
		entries.clear();
		assertThatThrownBy(() -> execute(new ValidateMTLSCertificatesAsX509(), env))
			.isInstanceOf(ConditionError.class);
		assertThat(String.join("\n", entries))
			.contains("MTLS Private Key and Cert do not match", env.getString("mutual_tls_authentication", "cert"))
			.doesNotContain(otherKey);
	}

	private Environment generatedCredentials() throws Exception {
		Security.addProvider(BouncyCastleProviderSingleton.getInstance());
		var key = new RSAKeyGenerator(2048).algorithm(JWSAlgorithm.PS256).generate();
		Environment env = new Environment();
		env.putObject("client_jwks", JsonParser.parseString(new JWKSet(key).toString(false)).getAsJsonObject());
		env.putString("client_name", "Logging unit test");
		execute(new GenerateMTLSCertificateFromJWKs(), env);
		return env;
	}

	private void execute(AbstractCondition condition, Environment env) {
		condition.setProperties("UNIT-TEST", capturingLog(), ConditionResult.FAILURE);
		condition.execute(env);
	}

	private AbstractCondition extractor(String section) {
		AbstractCondition condition = "mtls".equals(section)
			? new ExtractMTLSCertificatesFromConfiguration() : new ExtractMTLSCertificates2FromConfiguration();
		condition.setProperties("UNIT-TEST", capturingLog(), ConditionResult.FAILURE);
		return condition;
	}

	private TestInstanceEventLog capturingLog() {
		TestInstanceEventLog log = mock(TestInstanceEventLog.class);
		doAnswer(invocation -> {
			entries.add(invocation.getArgument(1).toString());
			return null;
		}).when(log).log(anyString(), any(JsonObject.class));
		doAnswer(invocation -> {
			entries.add(invocation.getArgument(1).toString());
			return null;
		}).when(log).log(anyString(), anyMap());
		return log;
	}

	private static Environment environment(String section) {
		Environment env = new Environment();
		JsonObject config = new JsonObject();
		config.add(section, credentials());
		env.putObject("config", config);
		return env;
	}

	private static JsonObject credentials() {
		JsonObject credentials = new JsonObject();
		credentials.addProperty("cert", CERTIFICATE);
		credentials.addProperty("key", PRIVATE_KEY);
		return credentials;
	}
}
