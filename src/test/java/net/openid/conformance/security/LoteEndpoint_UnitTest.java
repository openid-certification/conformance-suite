package net.openid.conformance.security;

import com.nimbusds.jose.JWSObject;
import net.openid.conformance.util.LoteList;
import net.openid.conformance.util.TestKeysAndCerts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the /lote payloads: a compact JWS of the named list, sealed by the LoTE signer, served as
 * application/jwt and stable across requests. Pure unit test by repo convention; anonymous
 * reachability of the routes is covered by scripts/run-security-tests.py.
 */
public class LoteEndpoint_UnitTest {

	private final LoteEndpoint endpoint = new LoteEndpoint();

	@ParameterizedTest
	@EnumSource(LoteList.class)
	public void servesEachListAsAJwsSealedByTheLoteSigner(LoteList list) throws Exception {
		ResponseEntity<String> response = endpoint.getList(list.slug);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.parseMediaType(LoteEndpoint.CONTENT_TYPE));
		assertThat(response.getHeaders().getCacheControl()).contains("max-age=3600");
		JWSObject jws = JWSObject.parse(response.getBody());
		assertThat(jws.getHeader().getX509CertChain().get(0).decode()).isEqualTo(TestKeysAndCerts.getLoteSignerCert().getEncoded());
		assertThat(jws.getPayload().toJSONObject().get("LoTE")).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(LoteList.class)
	public void servesAnEmptyVariantOfEachList(LoteList list) throws Exception {
		ResponseEntity<String> response = endpoint.getEmptyList(list.slug);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		JWSObject jws = JWSObject.parse(response.getBody());
		@SuppressWarnings("unchecked")
		var lote = (java.util.Map<String, Object>) jws.getPayload().toJSONObject().get("LoTE");
		assertThat(lote).doesNotContainKey("TrustedEntitiesList");
	}

	@Test
	public void repeatedRequestsSeeTheSameList() {
		assertThat(endpoint.getList("pid-providers").getBody()).isEqualTo(endpoint.getList("pid-providers").getBody());
	}

	@Test
	public void unknownNameIs404() {
		assertThat(endpoint.getList("no-such-list").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(endpoint.getEmptyList("no-such-list").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}
}
