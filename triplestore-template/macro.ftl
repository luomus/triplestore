<#macro booleanValue value langcode>
	<#assign found = false />
	<select class="objectLiteral">
		<option value=""></option>
		<option value="true" <#if value=="true">selected="selected"<#assign found = true /></#if>>true</option>
		<option value="false" <#if value=="false">selected="selected"<#assign found = true /></#if>>false</option>
	</select>
	<#if langcode?has_content>
		<@langcodeSelect langcode />
	</#if>
	<#if value?has_content && !found>
		<span class="error">INVALID VALUE: ${value?html}</span>
	</#if>
</#macro>

<#macro literalValue value langcode range="">
	<#if (value?length > 40)>
		<textarea class="objectLiteral">${value?html}</textarea>
	<#else>
		<input type="text" class="objectLiteral" value="${value?html}" />
	</#if>
	<#if langcode?has_content || range == "xsd:string">
		<@langcodeSelect langcode />
	</#if>
</#macro>

<#macro langcodeSelect langcode>
	<select class="langcode">
		<option value=""></option>
		<option value="fi" <#if langcode=="fi">selected="selected"</#if>>fi</option>
		<option value="en" <#if langcode=="en">selected="selected"</#if>>en</option>
		<option value="sv" <#if langcode=="sv">selected="selected"</#if>>sv</option>
	</select>
</#macro>

<#macro resourceValue value>
	<#if value?has_content>
		<input type="text" class="objectResource hidden" value="${value}" />
		<span class="objectResourceLink">
			<a href="${baseURL}/editor/${value}">${value}</a>
		</span> 
		<button class="changeObjectResourceButton">Change</button>
	<#else>
		<input type="text" class="objectResource" value="${value}" />
	</#if>
</#macro>

<#macro resourceValueRangeSelect value rangeValues>
	<#assign found = false />
	<select class="objectResource <#if (rangeValues?size > 20)>chosen</#if>">
		<option value=""></option>
		<#list rangeValues as rangeValue>
			<option value="${rangeValue.qname}" <#if value == rangeValue.qname.toString()>selected="selected" <#assign found = true /> </#if>>
				${rangeValue.qname} <#if rangeValue.label??> - ${(rangeValue.label.forLocale("fi")!rangeValue.label.forLocale("en")!"")?html}</#if>
			</option>
		</#list>
	</select>
	<#if value?has_content && !found>
		<span class="error">INVALID VALUE: ${value?html}</span>
	</#if>
</#macro>

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
	<li><a href="${baseURL}/editor/invalidate-caches">Clear caches</a></li>
</ul>
<div id="resourceListingResponse"></div>
<script>
$(function() {
	$.ajaxSetup({
  		headers: {
    		'Authorization': "Basic " + btoa('${TriplestoreSelf_Username}' + ":" + '${TriplestoreSelf_Password}')
  		}
	});
});
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
		if (!qname) {
			alert('Qname not given...');
			return;
		}
		var splitted = qname.split(".");
		if (splitted.length != 2) {
			alert('Invalid Qname...');
			return;
		}
		qname = splitted[0].toUpperCase() + "." + splitted[1];
		document.location.href = '${baseURL}/editor/'+qname;
	});
});
function toQname(uri) {
	return uri.replace('http://id.luomus.fi/', '').replace('http://tun.fi/', 'tun:');
}
</script>
</#macro>
