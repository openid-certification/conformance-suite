package net.openid.conformance.openbanking_brasil;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Map;

@Controller
public class SimpleApiController {

	@RequestMapping(method = RequestMethod.GET, path = "/api/v1/hello", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> sayHello() {

		return ResponseEntity.ok(Map.of("message", "Hello"));

	}

}
