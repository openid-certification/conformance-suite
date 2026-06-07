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
			logConnection(scope.exchangeId, ed, priorRequests, request, error);
		}
	}

	private void logConnection(String exchangeId, EndpointDetails ed, long priorRequests,
			ClassicHttpRequest request, Throwable error) {
		try {
			boolean reused = priorRequests > 0;
			JsonObject o = new JsonObject();
			o.addProperty("msg", "Pooled connection " + (reused ? "REUSED" : "new")
				+ (error != null ? " - request FAILED" : ""));
			o.addProperty("connection_reused", reused);
			o.addProperty("prior_requests_on_connection", priorRequests);
			o.addProperty("connection_local_port", localPort(ed));
			o.addProperty("connection_remote", ed != null ? String.valueOf(ed.getRemoteAddress()) : "?");
			o.addProperty("tls_identity", tlsIdentity);
			o.addProperty("exchange_id", exchangeId);
			o.addProperty("request_uri", request.getRequestUri());
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
