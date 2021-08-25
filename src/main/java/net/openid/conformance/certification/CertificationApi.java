package net.openid.conformance.certification;

import com.google.gson.JsonObject;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

@Controller
@RequestMapping(value = "/api")
public class CertificationApi {

	@PostMapping(value = "/plan/{id}/certificationofconformance", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
	@ApiOperation(value = "Get certification of conformance pdf template, with pre-populated fields")
	@ApiResponses(value = {
		@ApiResponse(code = 200, message = "Retrieved successfully"),
		@ApiResponse(code = 500, message = "An unexpected error occurred, the error should be reported to the developers")
	})
	public void getCertificationOfConformancePdfTemplate(
		HttpServletResponse response,
		@ApiParam(value = "Id of test plan") @PathVariable("id") String id,
		@ApiParam(value = "Name of Entity (Implementer) Making this Certification") @RequestParam(name = "nameOfImplementer", required = false) String nameOfImplementer,
		@ApiParam(value = "Software or Service (Deployment) Name & Version #") @RequestParam(name = "deploymentVersion", required = false) String deploymentVersion,
		@ApiParam(value = "OpenID Conformance Profile") @RequestParam(name = "conformanceProfile", required = false) String conformanceProfile,
		@ApiParam(value = "Conformance Test Suite Software") @RequestParam(name = "suiteSoftware", required = false) String suiteSoftware,
		@ApiParam(value = "Test Date") @RequestParam(name = "testDate", required = false) String testDate,
		@ApiParam(value = "URL at which people interested in using your implementation can learn about it and/or obtain it") @RequestParam(name = "moreInfoUrl", required = false) String moreInfoUrl,
		@ApiParam(value = "1-2 sentence description of the implementation") @RequestParam(name = "implementationDescription", required = false) String implementationDescription,
		@ApiParam(value = "The programming language of the software and deployment environment for it, if applicable") @RequestParam(name = "programmingLanguage", required = false) String programmingLanguage,
		@ApiParam(value = "Licensing terms of the software, if applicable") @RequestParam(name = "license", required = false) String license
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
			response.setContentLength((int)fileSystemResource.contentLength());
			response.setContentType(MediaType.APPLICATION_PDF_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=OpenID-Certification-of-Conformance.pdf");

			IOUtils.copy(fileSystemResource.getInputStream(), response.getOutputStream());
		} finally {
			if(filledPdfTemplateFile!=null) {
				filledPdfTemplateFile.delete();
			}
		}
	}
}
