package io.fintechlabs.testframework.ui;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
	@ApiOperation(value = "Get server information")
	@ApiResponses({
		@ApiResponse(code = 200, message = "Retrieved successfully")
	})
	public ResponseEntity<Object> getServerInfo() {
		return new ResponseEntity<>(serverInfoTemplate.getServerInfo(), HttpStatus.OK);
	}
}
