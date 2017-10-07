var FAPI_UI = {

	goHome : function() {
		window.location.replace("logs.html");
	},

	/**
	 * 
	 */
	loadContentsFromURL : function(url) {
		$.ajax({ 
	        type: 'GET', 
	        url: url, 
	        data: {}, 
	        success: function (data) { 

	        	var dataObj = $.parseJSON(data);
	            var fileIds = [];
	            
	            for (var i=0;i<dataObj.length;i++) {
	            	fileIds.push(dataObj[i]);
	            }

	            FAPI_UI.renderTableOfContents(fileIds);

	        }
	    });
	},

	/**
	 * 
	 */
	renderTableOfContents : function(fileIds) {

		for (var i=0;i<fileIds.length;i++) {
			var item = fileIds[i];
			$("#logs .content").append("<div class='link'><a href='log-detail.html?log="+item+"' class='item'>"+item+"</a></div>");
		}
	},

	/**
	 * 
	 */
	showLogDetail : function(fileId) {

		// load the project JSON
		var file = "test-logs/"+fileId+".json";

		$.ajax({ 
	        type: 'GET', 
	        url: file, 
	        data: {}, 
	        success: function (data) { 

	        	var dataObj = $.parseJSON(data);

	            $.each(dataObj, function(i, item) {

	            	$("#logDetail .content").append("<div class='item'>");

	            	$.each(item, function(key, val) {
	 
	            		//filter out anything starting with an underscore as its injected by Mongo
	            		if (key.charAt(0) != "_") {
	            			var str = "<div class='wordwrap'>";
	            			str += "<span class='key'>"+key+"</span>";

	            			if (key=="time") {
	            				str += "<span class='hypen'> : </span><span class='value'>" + new Date(val) + "</span>";
	            			} else {
	            				str += "<span class='hypen'> : </span><span class='value'>" + val + "</span>";
	            			}

	            			str += "</div>";
	            			$("#logDetail .content").append(str);
	            		}
	            	});

	            	$("#logDetail .content").append("</div>");

	            });

	        }
	    });

	}
	
}

