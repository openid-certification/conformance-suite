package net.openid.conformance.condition.common.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.DefaultTlsServer;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerProtocol;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * In-process BouncyCastle-based TLS server fixture for unit tests.
 *
 * Listens on 127.0.0.1 on an ephemeral port. Accepts a configurable list of TLS protocol
 * versions and cipher suites. Uses a freshly-generated self-signed RSA certificate, signed
 * via RSA-PSS so the same credential works for both TLS 1.2 (with rsa_pss_rsae_sha256 in
 * the client's signature_algorithms extension) and TLS 1.3.
 *
 * Server-side handshake exceptions are intentionally swallowed; tests assert on the
 * client-side outcome only.
 */
public final class TestTlsServer implements AutoCloseable {

	private final ProtocolVersion[] supportedVersions;
	private final int[] supportedCipherSuites;
	private final ServerSocket serverSocket;
	private final ExecutorService executor;
	private final KeyPair keyPair;
	private final X509Certificate certificate;

	public TestTlsServer(ProtocolVersion[] supportedVersions, int[] supportedCipherSuites) throws Exception {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
		this.supportedVersions = supportedVersions.clone();
		this.supportedCipherSuites = supportedCipherSuites.clone();

		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
		kpg.initialize(2048);
		this.keyPair = kpg.generateKeyPair();
		this.certificate = generateSelfSignedCert(keyPair);

		this.serverSocket = new ServerSocket();
		serverSocket.bind(new InetSocketAddress("127.0.0.1", 0));

		this.executor = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "TestTlsServer-accept-" + getPort());
			t.setDaemon(true);
			return t;
		});

		executor.submit(this::acceptLoop);
	}

	public int getPort() {
		return serverSocket.getLocalPort();
	}

	private void acceptLoop() {
		while (!serverSocket.isClosed()) {
			try (Socket client = serverSocket.accept()) {
				BcTlsCrypto crypto = new BcTlsCrypto(new SecureRandom());

				TlsCertificate tlsCert = crypto.createCertificate(certificate.getEncoded());
				Certificate certChain = new Certificate(new TlsCertificate[]{tlsCert});
				AsymmetricKeyParameter privateKeyParam = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());

				DefaultTlsServer server = new DefaultTlsServer(crypto) {
					@Override
					public ProtocolVersion[] getProtocolVersions() {
						return supportedVersions.clone();
					}

					@Override
					public int[] getCipherSuites() {
						return supportedCipherSuites.clone();
					}

					@Override
					protected int[] getSupportedCipherSuites() {
						return supportedCipherSuites.clone();
					}

					@Override
					public TlsCredentials getCredentials() throws IOException {
						return new BcDefaultTlsCredentialedSigner(
							new TlsCryptoParameters(context),
							crypto,
							privateKeyParam,
							certChain,
							SignatureAndHashAlgorithm.rsa_pss_rsae_sha256);
					}
				};

				TlsServerProtocol protocol = new TlsServerProtocol(client.getInputStream(), client.getOutputStream());
				protocol.accept(server);
			} catch (IOException e) {
				// Either the socket was closed during shutdown, or the handshake failed on the
				// server side. Either way, the test asserts on the client-side outcome; loop
				// again to handle subsequent connections (the condition under test makes a
				// probe connection followed by a cipher-check connection).
			} catch (Exception e) {
				// Unexpected — swallow so the test doesn't deadlock; the client-side assertion
				// will surface the symptom.
			}
		}
	}

	@Override
	public void close() {
		try {
			serverSocket.close();
		} catch (IOException ignored) {
			// nothing to do
		}
		executor.shutdownNow();
		try {
			executor.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static X509Certificate generateSelfSignedCert(KeyPair kp) throws Exception {
		X500Name subject = new X500Name("CN=localhost");
		BigInteger serial = BigInteger.valueOf(System.nanoTime());
		Date notBefore = Date.from(Instant.now().minus(1, ChronoUnit.HOURS));
		Date notAfter = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
			subject, serial, notBefore, notAfter, subject, kp.getPublic());

		GeneralNames san = new GeneralNames(new GeneralName[]{
			new GeneralName(GeneralName.dNSName, "localhost"),
			new GeneralName(GeneralName.iPAddress, "127.0.0.1")
		});
		builder.addExtension(Extension.subjectAlternativeName, false, san);

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
			.setProvider("BC")
			.build(kp.getPrivate());

		X509CertificateHolder holder = builder.build(signer);
		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
	}
}
