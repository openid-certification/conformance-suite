package net.openid.conformance.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServerInfoUIController {

	@Autowired
	private ServerInfoTemplate serverInfoTemplate;
	/**
	 * Provide a JSON result that represents the currently Server Info.
	 *
	 * @return the info of server
	 */
	@GetMapping(value = "/api/server", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Get server information")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "Retrieved successfully")
	})
	public ResponseEntity<Object> getServerInfo() {
		return new ResponseEntity<>(serverInfoTemplate.getServerInfo(), HttpStatus.OK);
	}
}
