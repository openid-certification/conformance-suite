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
import java.util.List;
import java.util.Vector;


public class FAPITLSClient extends DefaultTlsClient {

	private String targetHost;
	private boolean allowOnlyFAPICiphers;
	private ProtocolVersion[] allowedProtocolVersion;

	private static final int[] FAPI_CIPHERS = {
			CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
//			List of ciphers on Mandatory to implement Cipher Suite of TLS1.3
			CipherSuite.TLS_AES_256_GCM_SHA384,
			CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
			CipherSuite.TLS_AES_128_GCM_SHA256

	};

	public FAPITLSClient(String tlsTestHost, boolean useOnlyFAPICiphers, ProtocolVersion... protocolVersion) {
		super(new BcTlsCrypto(new SecureRandom()));
		this.targetHost = tlsTestHost;
		this.allowOnlyFAPICiphers = useOnlyFAPICiphers;
		this.allowedProtocolVersion = protocolVersion;
	}

	@Override
	public int[] getCipherSuites() {
		if(allowOnlyFAPICiphers) {
			return FAPI_CIPHERS;
		} else {
			int[] defaultCiphers = super.getCipherSuites();
			int[] allowedCiphers = new int[defaultCiphers.length + FAPI_CIPHERS.length];
			System.arraycopy(FAPI_CIPHERS, 0, allowedCiphers, 0, FAPI_CIPHERS.length);
			System.arraycopy(defaultCiphers, 0, allowedCiphers, FAPI_CIPHERS.length, defaultCiphers.length);
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
