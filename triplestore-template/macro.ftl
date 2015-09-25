<#macro editorTools>
<#if history??>
	Previously edited: 
	<#list history.getPrevious(7) as qname>
		<a href="${baseURL}/editor/${qname}">${qname}</a> <#if qname_has_next> | </#if>
	</#list>
</#if>
<ul class="servicelist">
	<li>
		Edit by Qname <input id="editByQname" type="text" name="search" placeholder="Qname" /> <button id="editByQnameButton">Go</button>
	</li>
	<li>
		Search <input type="text" id="searchWord" placeholder="Object or literal" /> <button id="searchButton">Search</button>
	</li>
	<li>
		Create new 
			<select id="createNewResourceSelect">
				<option value="">Select type...</option>
				<#list creatableResources as r>
					<option value="${r.namespacePrefix}">${r.description}</option>
				</#list>
			</select>
			<button id="createNewResourceButton">Create</button>
	</li>
</ul>
</#macro>

<script>
$(function() {
	$("#searchButton").on('click', function() {
		var searchWord = $("#searchWord").val();
		if (searchWord.length < 3) {
			alert('Give a longer search word!');
		} else {
			var responseElement = $('#resourceListingResponse'); 
			responseElement.html('');
			$("body").css("cursor", "progress");
			responseElement.hide();
			$.ajax({
    			type: 'GET',
    			url: '${baseURL}/search?objectliteral=%25'+searchWord+'%25&limit=50',
    			dataType: 'xml',
    			success: function(xml) {
	    			var count = 0;
					$(xml).find('rdf\\:RDF').first().children().each(function() {
						count++;
						var instance = $('<div>');
						var resourceURI = $(this).attr('rdf:about');
						var resourceQname = toQname(resourceURI);
						instance.append('<a href="${baseURL}/editor/'+resourceQname+'">' + resourceQname + '</a> - ');
						$(this).find('MA\\.fullName').each(function() { instance.append($(this).text()) });
						$(this).find('KE\\.name').each(function() { instance.append($(this).text()) });
						$(this).find('GX\\.datasetName').each(function() { instance.append($(this).text()) });
						$(this).find('MOS\\.organizationLevel1').each(function() { instance.append($(this).text() + ' ') });
						$(this).find('MOS\\.organizationLevel2').each(function() { instance.append($(this).text() + ' ') });
						$(this).find('MOS\\.organizationLevel3').each(function() { instance.append($(this).text() + ' ') });
						$(this).find('MOS\\.organizationLevel4').each(function() { instance.append($(this).text() + ' ') });
						$(this).find('dc\\:bibliographicCitation').each(function() { instance.append($(this).text() + ' ') });
						$(this).find('rdfs\\:label').each(function() { instance.append($(this).text() + ' ') });
						responseElement.append(instance);
					});
					responseElement.prepend('<div>Found '+count+' records</div>');
					responseElement.fadeIn(500);
					$("body").css("cursor", "default");
				}
   			});
		}
	});	
	$("#createNewResourceButton").on('click', function() {
		var qnamePrefix = $("#createNewResourceSelect").val();
		if (qnamePrefix) {
			var createNewForm = $('<form>', {
    			'action': '${baseURL}/editor/create/'+qnamePrefix,
    			'method': 'POST'
		    });
    		$('body').append(createNewForm);
			createNewForm.submit();
		} else {
			alert('Please select type of resource to create');
		}
	});
	$("#editByQnameButton").on('click', function() {
		var qname = $("#editByQname").val();
		if (qname) {
			document.location.href = '${baseURL}/editor/'+qname.toUpperCase();
		} else {
			alert('Qname not given...');
		}
	});
});
function toQname(uri) {
	return uri.replace('http://id.luomus.fi/', '').replace('http://tun.fi/', 'tun:');
}
</script>