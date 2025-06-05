package net.openid.conformance.util;

import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;


public class FAPITLSClient extends DefaultTlsClient {

	private String targetHost;
	private boolean allowOnlyFAPICiphers;
	private boolean useBCP195Ciphers = false;
	private ProtocolVersion[] allowedProtocolVersion;

	// List of ciphers on mandatory to implement Cipher Suite of TLS 1.3
	private static final int[] TLS_1_3_CIPHERS = {
			CipherSuite.TLS_AES_256_GCM_SHA384,
			CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
			CipherSuite.TLS_AES_128_GCM_SHA256

	};

	// List of ciphers permitted in FAPI specs for TLS 1.2.
	private static final int[] FAPI_TLS_1_2_CIPHERS = {
			CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
	};

	// List of ciphers recommended in BCP195 spec for TLS 1.2.
	private static final int[] BCP195_TLS_1_2_CIPHERS = {
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
			CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384
	};

	public FAPITLSClient(String tlsTestHost, boolean useOnlyFAPICiphers, boolean useBCP195Ciphers, ProtocolVersion... protocolVersion) {
		super(new BcTlsCrypto(new SecureRandom()));
		this.targetHost = tlsTestHost;
		this.allowOnlyFAPICiphers = useOnlyFAPICiphers;
		this.allowedProtocolVersion = protocolVersion;

		this.useBCP195Ciphers = useBCP195Ciphers;
	}

	public static int[] getTLS12Ciphers(boolean useBCP195Ciphers) {
		if (useBCP195Ciphers) {
			return BCP195_TLS_1_2_CIPHERS;
		}
		else {
			return FAPI_TLS_1_2_CIPHERS;
		}
	}

	@Override
	public int[] getCipherSuites() {
		int[] fapiCiphers;

		// Construct the fapiCiphers list.
		if (useBCP195Ciphers) {
			// BCP195 TLS 1.2 recommended ciphers + TLS 1.3 mandatory ciphers.
			fapiCiphers = IntStream.concat(Arrays.stream(BCP195_TLS_1_2_CIPHERS), Arrays.stream(TLS_1_3_CIPHERS)) .toArray();
		}
		else {
			// FAPI TLS 1.2 ciphers + TLS 1.3 mandatory ciphers.
			fapiCiphers = IntStream.concat(Arrays.stream(FAPI_TLS_1_2_CIPHERS), Arrays.stream(TLS_1_3_CIPHERS)) .toArray();
		}

		if(allowOnlyFAPICiphers) {
			return fapiCiphers;
		} else {
			int[] defaultCiphers = super.getCipherSuites();
			int[] allowedCiphers = new int[defaultCiphers.length + fapiCiphers.length];
			System.arraycopy(fapiCiphers, 0, allowedCiphers, 0, fapiCiphers.length);
			System.arraycopy(defaultCiphers, 0, allowedCiphers, fapiCiphers.length, defaultCiphers.length);
			return allowedCiphers;
		}
	}


	@Override
	public TlsAuthentication getAuthentication() {
		return new TlsAuthentication() {

			@Override
			public TlsCredentials getClientCredentials(CertificateRequest certificateRequest) throws IOException
			{
				return null;
			}

			@Override
			public void notifyServerCertificate(TlsServerCertificate serverCertificate) throws IOException {
				// even though we make a TLS connection we ignore the server cert validation here
			}
		};
	}

	@Override
	protected ProtocolVersion[] getSupportedVersions() {
		return this.allowedProtocolVersion;
	}

	@Override
	protected Vector<ServerName> getSNIServerNames() {
		return new Vector<ServerName>(List.of(new ServerName(NameType.host_name, this.targetHost.getBytes(StandardCharsets.UTF_8))));
	}

	@Override
	public void notifyServerVersion(ProtocolVersion serverVersion) throws IOException {
		// don't need to proceed further
		throw new ServerHelloReceived(serverVersion);
	}

	// Signals that the connection was aborted after discovering the server version
	@SuppressWarnings("serial")
	public static class ServerHelloReceived extends IOException {

		private ProtocolVersion serverVersion;

		public ServerHelloReceived(ProtocolVersion serverVersion) {
			this.serverVersion = serverVersion;
		}

		public ProtocolVersion getServerVersion() {
			return serverVersion;
		}

	}
}
