package net.openid.conformance.extensions;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.produce.JWSSignerFactory;

public interface AlternateJWSSignerFactory extends JWSSignerFactory {

	boolean canSignWith(JWK jwk);

}
