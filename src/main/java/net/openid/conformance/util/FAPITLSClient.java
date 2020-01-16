package net.openid.conformance.util;

import org.bouncycastle.crypto.tls.Certificate;
import org.bouncycastle.crypto.tls.CertificateRequest;
import org.bouncycastle.crypto.tls.CipherSuite;
import org.bouncycastle.crypto.tls.DefaultTlsClient;
import org.bouncycastle.crypto.tls.NameType;
import org.bouncycastle.crypto.tls.ProtocolVersion;
import org.bouncycastle.crypto.tls.ServerName;
import org.bouncycastle.crypto.tls.ServerNameList;
import org.bouncycastle.crypto.tls.TlsAuthentication;
import org.bouncycastle.crypto.tls.TlsCredentials;
import org.bouncycastle.crypto.tls.TlsExtensionsUtils;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class FAPITLSClient extends DefaultTlsClient {

	private Object targetHost;
	private boolean allowOnlyFAPICiphers;
	private ProtocolVersion allowedProtocolVersion;

	public static final int[] FAPI_CIPHERS = {
		CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
		CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
		CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
	};

	public FAPITLSClient(Object tlsTestHost, boolean useOnlyFAPICiphers, ProtocolVersion protocolVersion) {
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
			public void notifyServerCertificate(Certificate serverCertificate) throws IOException {
				// even though we make a TLS connection we ignore the server cert validation here
			}
		};
	}

		@Override
	public ProtocolVersion getMinimumVersion() {
		return allowedProtocolVersion;
	}

		@Override
	public ProtocolVersion getClientVersion() {
		return allowedProtocolVersion;
	}

	@Override
	@SuppressWarnings("rawtypes") // fit with the API
	public Hashtable getClientExtensions() throws IOException {
		Hashtable clientExtensions = super.getClientExtensions();
		Vector<ServerName> serverNameList = new Vector<>();
		serverNameList.addElement(new ServerName(NameType.host_name, targetHost));
		TlsExtensionsUtils.addServerNameExtension(clientExtensions, new ServerNameList(serverNameList));
		return clientExtensions;
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
