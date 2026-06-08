package net.openid.conformance.logging;

import com.google.gson.JsonObject;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.EndpointDetails;
import org.apache.hc.core5.http.HttpException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.net.ssl.SSLSession;

/**
 * Diagnostic exec-chain interceptor for the pooled HTTP path. Unlike a request interceptor (which runs
 * in the {@code PROTOCOL} element, before a connection is leased, so {@code getEndpointDetails()} is
 * null), this is inserted <em>after</em> the {@code CONNECT} element, so the connection is already
 * leased and its details are available.
 *
 * <p>For every request it logs one event-log entry with the leased connection's <b>local port</b> (a
 * stable per-TCP-connection id), whether it was <b>reused</b> ({@link EndpointDetails#getRequestCount()}
 * &gt; 0 at lease time = prior requests on this connection), the remote address, the TLS identity, the
 * exchange id, and whether the request then failed. Correlating local ports across entries (with the
 * existing per-entry timestamps) reconstructs which physical connection each call used and how long it
 * had been idle before a failing reuse — the data the {@code Keep-Alive: max} header alone cannot give.
 */
public class ConnectionReuseLoggingExec implements ExecChainHandler {

	/** Session id (prefix) -&gt; the local port it was first seen on. Lets us tell TLS session reuse across a
	 *  NEW connection (a resumption indicator) apart from the same id on the same live connection (which is
	 *  just TCP/connection reuse, not resumption). Heuristic only: TLS 1.3 may not reuse the id this way. */
	private static final Map<String, Integer> SESSION_ID_FIRST_PORT = new ConcurrentHashMap<>();

	private final TestInstanceEventLog log;
	private final String source;
	private final String tlsIdentity;

	public ConnectionReuseLoggingExec(String source, TestInstanceEventLog log, String tlsIdentity) {
		this.source = source;
		this.log = log;
		this.tlsIdentity = tlsIdentity;
	}

	@Override
	public ClassicHttpResponse execute(ClassicHttpRequest request, ExecChain.Scope scope, ExecChain chain)
			throws IOException, HttpException {
		EndpointDetails leased = scope.clientContext.getEndpointDetails();
		long priorRequests = leased != null ? leased.getRequestCount() : -1;
		Throwable error = null;
		try {
			return chain.proceed(request, scope);
		} catch (IOException | HttpException | RuntimeException e) {
			error = e;
			throw e;
		} finally {
			// The connection may only be populated in the context once execution has started, so prefer
			// the post-execution view for the local port but keep the pre-execution request count (which
			// tells us whether this request reused an existing connection).
			EndpointDetails ed = scope.clientContext.getEndpointDetails();
			if (ed == null) {
				ed = leased;
			}
			logConnection(scope.exchangeId, ed, priorRequests, scope.clientContext.getSSLSession(), request, error);
		}
	}

	private void logConnection(String exchangeId, EndpointDetails ed, long priorRequests,
			SSLSession ssl, ClassicHttpRequest request, Throwable error) {
		try {
			int port = localPort(ed);
			// -1 = we couldn't read the request count at this exec stage; report reuse as unknown rather
			// than falsely "new" (the reliable reuse signal is local-port repetition across entries).
			String reuse = priorRequests > 0 ? "reused" : (priorRequests == 0 ? "new" : "unknown");
			JsonObject o = new JsonObject();
			o.addProperty("msg", "Pooled connection " + reuse + (error != null ? " - request FAILED" : ""));
			o.addProperty("connection_reuse", reuse);
			o.addProperty("prior_requests_on_connection", priorRequests);
			o.addProperty("connection_local_port", port);
			o.addProperty("connection_remote", ed != null ? String.valueOf(ed.getRemoteAddress()) : "?");
			o.addProperty("tls_identity", tlsIdentity);
			o.addProperty("exchange_id", exchangeId);
			o.addProperty("request_uri", request.getRequestUri());
			// TLS handshake correlation data (NOT a definitive resumption detector). protocol/cipher, the
			// session id, whether that id was seen before at all, and - the useful bit - whether it was
			// seen on a DIFFERENT connection (port), which points to session resumption across a new
			// connection rather than plain TCP reuse of one live connection.
			if (ssl != null) {
				byte[] sid = ssl.getId();
				String sidHex = sid == null || sid.length == 0 ? "" : HexFormat.of().formatHex(sid);
				o.addProperty("tls_protocol", ssl.getProtocol());
				o.addProperty("tls_cipher", ssl.getCipherSuite());
				if (sidHex.isEmpty()) {
					o.addProperty("tls_session_id", "(empty)");
				} else {
					Integer firstPort = SESSION_ID_FIRST_PORT.putIfAbsent(sidHex, port);
					o.addProperty("tls_session_id", sidHex.substring(0, Math.min(16, sidHex.length())));
					o.addProperty("tls_session_id_seen_before", firstPort != null);
					o.addProperty("tls_session_id_seen_on_other_connection",
						firstPort != null && port != -1 && firstPort != port);
				}
			}
			if (error != null) {
				o.addProperty("error", error.getClass().getSimpleName() + ": " + error.getMessage());
			}
			log.log(source, o);
		} catch (RuntimeException e) {
			// Diagnostic logging must never affect the request being executed.
		}
	}

	private static int localPort(EndpointDetails ed) {
		if (ed != null && ed.getLocalAddress() instanceof InetSocketAddress isa) {
			return isa.getPort();
		}
		SocketAddress local = ed != null ? ed.getLocalAddress() : null;
		return local == null ? -1 : local.hashCode();
	}
}
