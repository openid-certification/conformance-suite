package net.openid.conformance.condition.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ExtractMTLSCertificatesFromConfiguration;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MtlsKeystoreBuilder_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private MtlsKeystoreBuilder cond;
	private ExtractMTLSCertificatesFromConfiguration extractMtlsCond;

	{
		Security.addProvider(new BouncyCastleProvider());

	}

	@BeforeEach
	void setUp() {
		cond = new MtlsKeystoreBuilder();
		extractMtlsCond = new ExtractMTLSCertificatesFromConfiguration();
		extractMtlsCond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);;
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	public void testRSA_Pkcs8_noError() throws Exception{

		String cert =  "-----BEGIN CERTIFICATE-----\n" +
			"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\n" +
			"jj64\n" +
			"-----END CERTIFICATE-----";

		String key = "-----BEGIN PRIVATE KEY-----\n" +
			"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\n" +
			"o5R0y60o/eUPQzh+e/Xjo2tAB6tujQ==\n" +
			"-----END PRIVATE KEY-----";
		String ca = "";

		JsonObject config = JsonParser.parseString("{\"mtls\":{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"" + key + "\","
			+ "\"ca\":\"" + ca + "\""
			+ "}}").getAsJsonObject();

		env.putObject("config", config);

		extractMtlsCond.execute(env);

		KeyManager[] km = MtlsKeystoreBuilder.configureMtls(env);

		assertNotNull(km);
	}

	@Test
	public void testRSA_Pkcs_noError() throws Exception{
		String cert =  "-----BEGIN CERTIFICATE-----\n" +
			"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\n" +
			"jj64\n" +
			"-----END CERTIFICATE-----";

		String key = "-----BEGIN RSA PRIVATE KEY-----\n" +
			"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\n" +
			"-----END RSA PRIVATE KEY-----";
		String ca = "";

		JsonObject config = JsonParser.parseString("{\"mtls\":{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"" + key + "\","
			+ "\"ca\":\"" + ca + "\""
			+ "}}").getAsJsonObject();

		env.putObject("config", config);
		extractMtlsCond.execute(env);
		KeyManager[] km = MtlsKeystoreBuilder.configureMtls(env);
		assertNotNull(km);
	}

	@Test
	public void testEC_Pkcs1_noError() throws Exception{

		String cert =  "-----BEGIN CERTIFICATE-----\n" +
			"MIIB4jCCAYmgAwIBAgIUNUXl1CHXlpHJ7u2SQb4nQYhcQ9cwCgYIKoZIzj0EAwIw\n" +
			"RzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExDTALBgNVBAoMBEFD\n" +
			"TUUxFDASBgNVBAMMC1Rlc3QgU2VydmVyMB4XDTI1MDcxNjAxNTUxN1oXDTI2MDcx\n" +
			"NjAxNTUxN1owRzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExDTAL\n" +
			"BgNVBAoMBEFDTUUxFDASBgNVBAMMC1Rlc3QgU2VydmVyMFkwEwYHKoZIzj0CAQYI\n" +
			"KoZIzj0DAQcDQgAEqyL9zqzjD1zRqCQV60u83eXBMo40TRBFv6kQvG9ryqbXTb+9\n" +
			"BDC2HChuJ7iQuYb7LZC4L6EJVEUlRbCynP8KfaNTMFEwHQYDVR0OBBYEFLkIhHPw\n" +
			"ror3rzeuXp9uIwvORUAkMB8GA1UdIwQYMBaAFLkIhHPwror3rzeuXp9uIwvORUAk\n" +
			"MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgSv0+1PHEIhSn1OI8\n" +
			"biqY4Dli51WwsYPTCDWTHon1NIYCIHKZp4dmUFtZ6+3TuZ9et/zVNutBnaROsy3k\n" +
			"EDHB4SHz\n" +
			"-----END CERTIFICATE-----";

		String key = "-----BEGIN EC PRIVATE KEY-----\n" +
			"MHcCAQEEILb8pKg4bWzI2tj+r7ncVh9BoEz0lWgO6oRMbSSHAhXEoAoGCCqGSM49\n" +
			"AwEHoUQDQgAEqyL9zqzjD1zRqCQV60u83eXBMo40TRBFv6kQvG9ryqbXTb+9BDC2\n" +
			"HChuJ7iQuYb7LZC4L6EJVEUlRbCynP8KfQ==\n" +
			"-----END EC PRIVATE KEY-----";
		String ca = "";

		JsonObject config = JsonParser.parseString("{\"mtls\":{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"" + key + "\","
			+ "\"ca\":\"" + ca + "\""
			+ "}}").getAsJsonObject();

		env.putObject("config", config);

		extractMtlsCond.execute(env);

		KeyManager[] km = MtlsKeystoreBuilder.configureMtls(env);

		assertNotNull(km);
	}

	@Test
	public void testEC_Pkcs8_noError() throws Exception{

		String cert =  "-----BEGIN CERTIFICATE-----\n" +
			"MIIB4jCCAYmgAwIBAgIUNUXl1CHXlpHJ7u2SQb4nQYhcQ9cwCgYIKoZIzj0EAwIw\n" +
			"RzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExDTALBgNVBAoMBEFD\n" +
			"TUUxFDASBgNVBAMMC1Rlc3QgU2VydmVyMB4XDTI1MDcxNjAxNTUxN1oXDTI2MDcx\n" +
			"NjAxNTUxN1owRzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3JuaWExDTAL\n" +
			"BgNVBAoMBEFDTUUxFDASBgNVBAMMC1Rlc3QgU2VydmVyMFkwEwYHKoZIzj0CAQYI\n" +
			"KoZIzj0DAQcDQgAEqyL9zqzjD1zRqCQV60u83eXBMo40TRBFv6kQvG9ryqbXTb+9\n" +
			"BDC2HChuJ7iQuYb7LZC4L6EJVEUlRbCynP8KfaNTMFEwHQYDVR0OBBYEFLkIhHPw\n" +
			"ror3rzeuXp9uIwvORUAkMB8GA1UdIwQYMBaAFLkIhHPwror3rzeuXp9uIwvORUAk\n" +
			"MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDRwAwRAIgSv0+1PHEIhSn1OI8\n" +
			"biqY4Dli51WwsYPTCDWTHon1NIYCIHKZp4dmUFtZ6+3TuZ9et/zVNutBnaROsy3k\n" +
			"EDHB4SHz\n" +
			"-----END CERTIFICATE-----";

		String key = "-----BEGIN PRIVATE KEY-----\n" +
			"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgtvykqDhtbMja2P6v\n" +
			"udxWH0GgTPSVaA7qhExtJIcCFcShRANCAASrIv3OrOMPXNGoJBXrS7zd5cEyjjRN\n" +
			"EEW/qRC8b2vKptdNv70EMLYcKG4nuJC5hvstkLgvoQlURSVFsLKc/wp9\n" +
			"-----END PRIVATE KEY-----";
		String ca = "";

		JsonObject config = JsonParser.parseString("{\"mtls\":{"
			+ "\"cert\":\"" + cert + "\","
			+ "\"key\":\"" + key + "\","
			+ "\"ca\":\"" + ca + "\""
			+ "}}").getAsJsonObject();

		env.putObject("config", config);

		extractMtlsCond.execute(env);

		KeyManager[] km = MtlsKeystoreBuilder.configureMtls(env);

		assertNotNull(km);
	}

}
