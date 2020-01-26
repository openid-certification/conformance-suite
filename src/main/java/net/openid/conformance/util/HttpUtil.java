package net.openid.conformance.util;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Use only for retrieving things that don't need proper security, e.g logo_uri
 * Does not restrict tls versions or ciphers
 * Does not perform certificate or hostname checks
 * @return
 */
public class HttpUtil {

	/**
	 * Catch this to catch exceptions thrown by methods in this class
	 */
	@SuppressWarnings("serial")
	public static class HttpUtilException extends RuntimeException{
		public HttpUtilException(Throwable other) {
			super(other);
		}
	}

	/**
	 * send a HEAD request and return the response
	 * @param url
	 * @return
	 */
	public static HttpResponse headRequest(String url) {
		try {
			HttpClient httpClient = createHttpClient();
			HttpHead headRequest = new HttpHead(url);
			HttpResponse response = httpClient.execute(headRequest);
			return response;
		}
		catch (Exception e) {
			throw new HttpUtilException(e);
		}
	}

	/**
	 * send a GET request and return the response as string
	 * @param url
	 * @return
	 */
	public static String getAsString(String url) {
		try {
			HttpClient httpClient = createHttpClient();
			HttpGet getRequest = new HttpGet(url);
			HttpResponse response = httpClient.execute(getRequest);
			String body = EntityUtils.toString(response.getEntity());
			return body;
		}
		catch (Exception e) {
			throw new HttpUtilException(e);
		}
	}



	public static HttpClient createHttpClient() {
		try {
			HttpClientBuilder builder = HttpClientBuilder.create().useSystemProperties();
			TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager()
				{

					@Override
					public X509Certificate[] getAcceptedIssuers()
					{
						return new X509Certificate[0];
					}

					@Override
					public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
					{
					}

					@Override
					public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
					{
					}
				}
			};

			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, null);
			builder.setSSLContext(sc);
			builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);

			HttpClient httpClient = builder.build();
			return httpClient;
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
