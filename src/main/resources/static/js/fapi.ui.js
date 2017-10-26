var FAPI_UI = {

	logTemplates : {
					LOG_START:"LogStart",
					SOURCE:"Source", 
					MESSAGE:"Message", 
					REQUIREMENTS:"Requirements", 
					UPLOAD:"Upload", 
					RESULT:"Result", 
					TIME:"Time",
					MORE:"More"
					},

	visibleFields : ["msg", "src", "time", "result", "requirements", "upload"],
	
	/**
	 * 
	 */
	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadAvailableLogs : function() {
		$.ajax({ 
	        type: 'GET', 
	        url: "log", 
	        data: {}, 
	        success: render
	    });
	},

	/**
	 * 
	 */
	showLogDetail : function(fileId) {
		$.ajax({ 
	        type: 'GET', 
	        url: "log/" + encodeURIComponent(fileId), 
	        data: {}, 
	        success: function (data) { 

				render(FAPI_UI.logTemplates.LOG_START, null, null, fileId);

	            $.each(data, function(i, item) {

	            	if (item.src) {
	            		render(FAPI_UI.logTemplates.SOURCE,item);
	            	}

	            	if (item.msg) {
	            		render(FAPI_UI.logTemplates.MESSAGE,item);
	            	}

	            	if (item.requirements) {
	            		render(FAPI_UI.logTemplates.REQUIREMENTS,item);
	            	}

	            	if (item.upload) {
	            		render(FAPI_UI.logTemplates.UPLOAD,item);
	            	}

	            	if (item.result) {
	            		render(FAPI_UI.logTemplates.RESULT,item);
	            	}

	            	if (item.time) {
	            		render(FAPI_UI.logTemplates.TIME,item);
	            	}

	            	render(FAPI_UI.logTemplates.MORE, item, i);
					
	            });
	        }
	    });
	}
}

