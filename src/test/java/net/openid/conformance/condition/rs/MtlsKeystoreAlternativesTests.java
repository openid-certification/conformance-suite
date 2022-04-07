package net.openid.conformance.condition.rs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.extensions.AlternateKeystoreRegistry;
import net.openid.conformance.extensions.KeystoreStrategy;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class MtlsKeystoreAlternativesTests {

	@Spy
	private AlternateKeystoreRegistry registry = AlternateKeystoreRegistry.getINSTANCE();

	private Environment env = new Environment();

	private CallProtectedResource cond = new CallProtectedResource();

	private static JsonObject bearerToken = JsonParser.parseString("{"
		+ "\"value\":\"mF_9.B5f-4.1JqM\","
		+ "\"type\":\"Bearer\""
		+ "}").getAsJsonObject();

	private ClientObservingHttpServer server;

	@Test
	public void testUseDefaultKeystorNoClientAuth() throws Exception, KeyManagementException {

		KeyPair keyPair = generateKeyPair();
		Certificate certificate = generate(keyPair, "localhost");
		KeyStore serverKeystore = KeyStore.getInstance("JKS");
		serverKeystore.load(null);
		serverKeystore.setKeyEntry("localhost", keyPair.getPrivate(),"changeit".toCharArray(), new Certificate[] {certificate});

		server = new ClientObservingHttpServer(serverKeystore, null);

		int localPort = server.start();

		env.putString("protected_resource_url", String.format("https://localhost:%d/hello", localPort));
		env.putObject("access_token", bearerToken);

		try {
			cond.execute(env);
		} catch (ConditionError ce) {
		}

		server.await();

		Certificate[] clientCerts = server.getClientCerts();

		assertNull(clientCerts);

	}

	@Test
	public void testUseDefaultKeystoreWithMtls() throws Exception {

		KeyPair serverPair = generateKeyPair();
		Certificate serverCert = generate(serverPair, "localhost");
		KeyStore serverKeystore = KeyStore.getInstance("JKS");
		serverKeystore.load(null);
		serverKeystore.setKeyEntry("localhost", serverPair.getPrivate(),"changeit".toCharArray(), new Certificate[] {serverCert});

		KeyPair clientPair = generateKeyPair();
		Certificate clientCert = generate(clientPair, "standardmtls");
		JsonObject mtlsConfig = buildStandardMtls(clientPair, clientCert);

		KeyStore trust = KeyStore.getInstance("JKS");
		trust.load(null);
		trust.setCertificateEntry("localhost", clientCert);

		server = new ClientObservingHttpServer(serverKeystore, trust);
		int localPort = server.start();

		env.putString("protected_resource_url", String.format("https://localhost:%d/hello", localPort));
		env.putObject("mutual_tls_authentication", mtlsConfig);
		env.putObject("access_token", bearerToken);

		try {
			cond.execute(env);
		} catch (ConditionError ce) {
		}

		server.await();

		Certificate[] clientCerts = server.getClientCerts();

		assertEquals(1, clientCerts.length);
		Certificate clientCertUsed = clientCerts[0];

		RSAPublicKey clientKeyUConfigured = (RSAPublicKey) clientCert.getPublicKey();
		RSAPublicKey clientKeyUsed = (RSAPublicKey) clientCertUsed.getPublicKey();

		// ensure client cert used for request is the one we configured in the environment
		assertEquals(clientKeyUsed.getPublicExponent(), clientKeyUConfigured.getPublicExponent());
		assertEquals(clientKeyUsed.getModulus(), clientKeyUConfigured.getModulus());
	}

	@Test
	public void testUseAlternateKeystoreWithMtls() throws Exception {

		KeyPair clientPair = generateKeyPair();
		Certificate clientCert = generate(clientPair, "standardmtls");
		JsonObject mtlsConfig = buildStandardMtls(clientPair, clientCert);

		// remove the key - we will get it from the alternate keystore
		mtlsConfig.addProperty("key", "notakey");

		// set up alternate key config - see ExtractMTLSCertificatesFromConfiguration
		JsonObject altKeyObject = new JsonObject();
		altKeyObject.addProperty("provider", "testAlternate");
		env.putObject("mtls_alternate_key", altKeyObject);

		KeyStore alternateKeyStre = KeyStore.getInstance("JKS");
		alternateKeyStre.load(null);
		alternateKeyStre.setKeyEntry("localhost", clientPair.getPrivate(),"changeit".toCharArray(), new Certificate[] {clientCert});

		// register a new alternate keystore - how this is loaded not our concern
		KeystoreStrategy keystoreStrategy = new SimpleAlternateKeystoreStrategy(alternateKeyStre);
		AlternateKeystoreRegistry.getINSTANCE().register("testAlternate", keystoreStrategy);

		KeyPair serverPair = generateKeyPair();
		Certificate serverCert = generate(serverPair, "localhost");
		KeyStore serverKeystore = KeyStore.getInstance("JKS");
		serverKeystore.load(null);
		serverKeystore.setKeyEntry("localhost", serverPair.getPrivate(),"changeit".toCharArray(), new Certificate[] {serverCert});

		KeyStore trust = KeyStore.getInstance("JKS");
		trust.load(null);
		trust.setCertificateEntry("localhost", clientCert);

		server = new ClientObservingHttpServer(serverKeystore, trust);
		int localPort = server.start();

		env.putString("protected_resource_url", String.format("https://localhost:%d/hello", localPort));
		env.putObject("mutual_tls_authentication", mtlsConfig);
		env.putObject("access_token", bearerToken);

		try {
			cond.execute(env);
		} catch (ConditionError ce) {
		}

		server.await();

		Certificate[] clientCerts = server.getClientCerts();

		assertEquals(1, clientCerts.length);
		Certificate clientCertUsed = clientCerts[0];

		RSAPublicKey clientKeyUConfigured = (RSAPublicKey) clientCert.getPublicKey();
		RSAPublicKey clientKeyUsed = (RSAPublicKey) clientCertUsed.getPublicKey();

		// ensure the key used in the request is indeed the one configured in our alternate keystore
		assertEquals(clientKeyUsed.getPublicExponent(), clientKeyUConfigured.getPublicExponent());
		assertEquals(clientKeyUsed.getModulus(), clientKeyUConfigured.getModulus());
	}


	private JsonObject buildStandardMtls(KeyPair keyPair, Certificate certificate) throws CertificateException, OperatorCreationException, CertIOException {
		PrivateKey privKey = keyPair.getPrivate();

		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("cert", Base64.getEncoder().encodeToString(certificate.getEncoded()));
		jsonObject.addProperty("key", Base64.getEncoder().encodeToString(privKey.getEncoded()));
		return jsonObject;
	}

	private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048, new SecureRandom());
		KeyPair keypair = keyGen.generateKeyPair();
		return keypair;
	}

	public X509Certificate generate(final KeyPair keyPair,
									final String cn)
		throws OperatorCreationException, CertificateException, CertIOException
	{
		final Instant now = Instant.now();
		final Date notBefore = Date.from(now);
		final Date notAfter = Date.from(now.plus(Duration.ofDays(1)));
		final ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
		final X500Name x500Name = new X500Name("CN=" + cn);

		ASN1EncodableVector purposes = new ASN1EncodableVector();
		purposes.add(KeyPurposeId.id_kp_serverAuth);
		purposes.add(KeyPurposeId.id_kp_clientAuth);
		purposes.add(KeyPurposeId.anyExtendedKeyUsage);

		final X509v3CertificateBuilder certificateBuilder =
			new JcaX509v3CertificateBuilder(x500Name,
				BigInteger.valueOf(now.toEpochMilli()),
				notBefore,
				notAfter,
				x500Name,
				keyPair.getPublic())
				.addExtension(Extension.basicConstraints, true, new BasicConstraints(false))
				.addExtension(Extension.extendedKeyUsage, false, new DERSequence(purposes));
		return new JcaX509CertificateConverter()
			.setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
	}


	@Before
	public void setup() {

		cond.setProperties("test", mock(TestInstanceEventLog.class), Condition.ConditionResult.FAILURE);

	}

	@After
	public void tearDown() throws IOException {
		server.stop();
	}

}
