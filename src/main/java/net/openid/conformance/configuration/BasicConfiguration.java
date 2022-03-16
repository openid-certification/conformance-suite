package net.openid.conformance.configuration;

import net.openid.conformance.logging.JsonObjectSanitiser;
import net.openid.conformance.logging.JwksLeafNodeVisitor;
import net.openid.conformance.logging.MapSanitiser;
import net.openid.conformance.logging.PrivateKeyLeafVisitor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class BasicConfiguration {

	@Bean
	public JsonObjectSanitiser jsonObjectSanitiser() {
		return new JsonObjectSanitiser(Set.of(new PrivateKeyLeafVisitor(), new JwksLeafNodeVisitor()));
	}

	@Bean
	public MapSanitiser mapSanitiser() {
		return new MapSanitiser(Set.of(new PrivateKeyLeafVisitor(), new JwksLeafNodeVisitor()));
	}

}
