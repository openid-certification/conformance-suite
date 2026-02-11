package net.openid.conformance.util;

import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.CipherSuite;
import org.bouncycastle.tls.DefaultTlsClient;
import org.bouncycastle.tls.NameType;
import org.bouncycastle.tls.ProtocolVersion;
import org.bouncycastle.tls.ServerName;
import org.bouncycastle.tls.TlsAuthentication;
import org.bouncycastle.tls.TlsCredentials;
import org.bouncycastle.tls.TlsServerCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.stream.IntStream;


public class FAPITLSClient extends DefaultTlsClient {

	private String targetHost;
	private boolean allowOnlyFAPICiphers;
	private boolean useBCP195Ciphers = false;
	private ProtocolVersion[] allowedProtocolVersion;

	private static final Logger logger = LoggerFactory.getLogger(FAPITLSClient.class);

	// List of ciphers on mandatory to implement Cipher Suite of TLS 1.3
	private static final int[] TLS_1_3_CIPHERS = {
			CipherSuite.TLS_AES_256_GCM_SHA384,
			CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
			CipherSuite.TLS_AES_128_GCM_SHA256

	};

	// List of ciphers permitted in FAPI specs for TLS 1.2 (which align with the older BCP195, RFC7525)
	private static final int[] FAPI_TLS_1_2_CIPHERS = {
			CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
			CipherSuite.TLS_DHE_RSA_WITH_AES_256_GCM_SHA384,
			CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
	};

	// List of ciphers recommended in BCP195 spec for TLS 1.2 (at the time of writing, BCP195 refers to RFC9325)
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

	// List of ciphers recommended in the IANA Transport Layer Security (TLS) Parameters document.
	private static int[] IANA_TLS_1_2_CIPHERS = null;

	// Return an array of non-deprecated ciphers parsed from the 'tls-parameters-4.csv' resource file.
	private static int[] getIANACiphers() {

		if (IANA_TLS_1_2_CIPHERS != null) {
			// The IANA_TLS_1_2_CIPHERS array has already been populated from a static file.
			// No need to parse the file again.
			return IANA_TLS_1_2_CIPHERS;
		}

		ArrayList<Integer> ciphers = new ArrayList<Integer>();
		// Non-deprecated ciphers which must not be offered.
		List<String> exclusions = Arrays.asList("TLS_NULL_WITH_NULL_NULL", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV", "TLS_FALLBACK_SCSV");

		InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("csv/tls-parameters-4.csv");

		if (inputStream != null) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

			try {
				while (true) {
					String line = reader.readLine();

					// EOF
					if (line == null) {
						break;
					}

					// Ignore fields that don't start with quoted values.
					// eg. "0x00,0x02"
					if (! line.matches("^\".*?\".*")) {
						continue;
					}

					// Remove the quoted value field.
					line = line.replaceFirst("\".*?\",", "");

					// Ignore entries with a "Description" field that doesn't
					// start with "TLS_". This ignores "Reserved" and "Unassigned"
					// entries.
					if (! line.startsWith("TLS_")) {
						continue;
					}

					String[] fields = line.split(",");

					// Ignore entries that don't contain the required number
					// of fields.
					if (fields.length < 3) {
						continue;
					}

					// Ignore deprecated entries.
					if (fields[2].equals("D")) {
						continue;
					}

					// Ignore ciphers in our exclusion list.
					if (exclusions.contains(fields[0])) {
						continue;
					}

					// Add the CipherSuite value of the cipher to the result array.
					try {
						ciphers.add(CipherSuite.class.getField(fields[0]).getInt(null));
					} catch (NoSuchFieldException | IllegalAccessException e) {
						// CipherSuite does not contain this cipher. Ignore and carry on.
					}
				}
			} catch (IOException e) {
				// readLine error
				logger.error("Failed to fully parse the 'tls-parameters-4.csv' resource file. Truncated list of IANA TLS ciphers returned.");
			} finally {
				try {
					// Ensure the resource is closed
					reader.close();
				}
				catch (IOException e) {
				}
			}
		}
		else {
			logger.error("Failed to open the 'tls-parameters-4.csv' resource file. No IANA TLS ciphers returned.");
		}

		IANA_TLS_1_2_CIPHERS = ciphers.stream().mapToInt(Integer::valueOf).toArray();

		// Return the cipher array as an int[]
		return IANA_TLS_1_2_CIPHERS;
	}

	public static int[] getTLS12Ciphers(boolean useBCP195Ciphers) {
		if (useBCP195Ciphers) {
			// BCP195 TLS 1.2 recommended ciphers + non-deprecated ciphers from https://www.iana.org/assignments/tls-parameters/tls-parameters-4.csv
			// as per https://bitbucket.org/openid/fapi/issues/847/52-network-layer-protections-are.
			return IntStream.concat(Arrays.stream(BCP195_TLS_1_2_CIPHERS), Arrays.stream(getIANACiphers())).distinct().toArray();
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
			// Additionally the non-deprecated ciphers from https://www.iana.org/assignments/tls-parameters/tls-parameters-4.csv
			// are included as per https://bitbucket.org/openid/fapi/issues/847/52-network-layer-protections-are.
			fapiCiphers = IntStream.concat(IntStream.concat(Arrays.stream(BCP195_TLS_1_2_CIPHERS), Arrays.stream(TLS_1_3_CIPHERS)),
							Arrays.stream(getIANACiphers())).distinct().toArray();

		}
		else {
			// FAPI TLS 1.2 ciphers + TLS 1.3 mandatory ciphers.
			fapiCiphers = IntStream.concat(Arrays.stream(FAPI_TLS_1_2_CIPHERS), Arrays.stream(TLS_1_3_CIPHERS)).distinct().toArray();
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
