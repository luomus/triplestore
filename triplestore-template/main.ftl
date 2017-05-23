<#include "luomus-header.ftl">
<#include "macro.ftl">

<h1>Triplestore Editor</h1>

<#if error??>
	<span class="errorMessage">${error}</span>
</#if>
<#if success??>
	<span class="successMessage">${success}</span>
</#if>

<@editorTools />

<ul class="servicelist">
	<li>
		<table id="resource">
			<caption>Resources</caption>
			<thead>
				<tr>
					<th>Name</th>
					<th>Count</th>
				</tr>
			</thead>
			<tbody>
			<#list resources as resource>
				<#if (resource.count > 0)>
					<tr>
						<td>
							<#if (resource.count < 2000)>
								<a href="#" onclick="listResources('${resource.name}'); return false;">${resource.name}</a>
							<#else>
								${resource.name}
							</#if>
						</td>
						<td>${resource.count}</td>
					</tr>
				</#if>
			</#list>
			</tbody>
		</table>
	</li>
	<li>
		<div id="resourceListingResponse"></div>
	</li>
</ul>

<script>
function listResources(className) {
	var responseElement = $('#resourceListingResponse'); 
	responseElement.html('');
	$("body").css("cursor", "progress");
	responseElement.hide();
	$.ajax({
    	type: 'GET',
    	url: '${baseURL}/search?predicate=rdf:type&object='+className+'&limit=2000',
    	dataType: 'xml',
    	success: function(xml) {
    	   	var count = 0;
			$(xml).find(":root").children().each(function() {
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
				$(this).find("dc\\:bibliographicCitation, bibliographicCitation").each(function() { instance.append($(this).text() + ' ') });
				$(this).find("rdfs\\:label, label").each(function() { instance.append($(this).text() + ' ') });
				
				responseElement.append(instance);
			});
			responseElement.prepend('<div>Found '+count+' records</div>');
			responseElement.fadeIn(500);
			$("body").css("cursor", "default");
		}
   	});
}
</script>

<#include "luomus-footer.ftl">