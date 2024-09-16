package net.openid.conformance.certification;

import com.google.gson.JsonObject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
@RequestMapping(value = "/api")
public class CertificationApi {

	@PostMapping(value = "/plan/{id}/certificationofconformance", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
	@Operation(summary = "Get certification of conformance pdf template, with pre-populated fields")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Retrieved successfully"),
			@ApiResponse(responseCode = "500", description = "An unexpected error occurred, the error should be reported to the developers")
	})
	public void getCertificationOfConformancePdfTemplate(
			HttpServletResponse response,
			@Parameter(description = "Id of test plan") @PathVariable String id,
			@Parameter(description = "Name of Entity (Implementer) Making this Certification") @RequestParam(required = false) String nameOfImplementer,
			@Parameter(description = "Software or Service (Deployment) Name & Version #") @RequestParam(required = false) String deploymentVersion,
			@Parameter(description = "OpenID Conformance Profile") @RequestParam(required = false) String conformanceProfile,
			@Parameter(description = "Conformance Test Suite Software") @RequestParam(required = false) String suiteSoftware,
			@Parameter(description = "Test Date") @RequestParam(required = false) String testDate,
			@Parameter(description = "URL at which people interested in using your implementation can learn about it and/or obtain it") @RequestParam(required = false) String moreInfoUrl,
			@Parameter(description = "1-2 sentence description of the implementation") @RequestParam(required = false) String implementationDescription,
			@Parameter(description = "The programming language of the software and deployment environment for it, if applicable") @RequestParam(required = false) String programmingLanguage,
			@Parameter(description = "Licensing terms of the software, if applicable") @RequestParam(required = false) String license
	) throws Exception {
		File filledPdfTemplateFile = null;
		try {
			JsonObject data = new JsonObject();
			data.addProperty("nameOfImplementer", nameOfImplementer);
			data.addProperty("deploymentVersion", deploymentVersion);
			data.addProperty("conformanceProfile", conformanceProfile);
			data.addProperty("suiteSoftware", suiteSoftware);
			data.addProperty("testDate", testDate);
			data.addProperty("moreInfoUrl", moreInfoUrl);
			data.addProperty("implementationDescription", implementationDescription);
			data.addProperty("programmingLanguage", programmingLanguage);
			data.addProperty("license", license);
			filledPdfTemplateFile = CertificationOfConformanceUtil.fillCertificationOfConformancePDFTemplate(data);

			FileSystemResource fileSystemResource = new FileSystemResource(filledPdfTemplateFile);
			response.setContentLength((int) fileSystemResource.contentLength());
			response.setContentType(MediaType.APPLICATION_PDF_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=OpenID-Certification-of-Conformance.pdf");

			IOUtils.copy(fileSystemResource.getInputStream(), response.getOutputStream());
		} finally {
			if (filledPdfTemplateFile != null) {
				filledPdfTemplateFile.delete();
			}
		}
	}
}
