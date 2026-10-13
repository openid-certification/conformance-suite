package net.openid.conformance.condition.client;

import org.apache.hc.core5.http.NoHttpResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketException;

/**
 * Recognises a call that the server refused below the HTTP layer, as negative tests that drop the
 * mTLS client certificate allow.
 */
public class TlsConnectionFailure {

	private TlsConnectionFailure() {
	}

	/**
	 * True if the request failed because the TLS handshake was rejected, the connection was reset, or
	 * the server completed the handshake then closed the connection without sending an HTTP response.
	 * The last is how a Google Cloud load balancer in mTLS REJECT_INVALID mode refuses a request
	 * without a valid client certificate; it sends no TLS alert.
	 * A refused or unroutable connection is not a rejection: no request reached the server.
	 */
	public static boolean isTlsConnectionFailure(RestClientException e) {
		if (!(e instanceof ResourceAccessException)) {
			return false;
		}
		Throwable cause = e.getCause();
		return cause instanceof SSLException
			|| (cause instanceof SocketException
				&& !(cause instanceof ConnectException) && !(cause instanceof NoRouteToHostException))
			|| cause instanceof NoHttpResponseException;
	}
}
