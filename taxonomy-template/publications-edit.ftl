<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<#if action == "add">
	<h1>Create a new publication</h1>
	<form action="${baseURL}/publications/add" method="post" id="publicationForm">
<#else>
	<h1>Modify publication</h1>
	<form action="${baseURL}/publications/${publication.qname}" method="post" id="publicationForm">
</#if>

	<ul id="modifyCitation">
		<li>
			<label>Citation</label>
			<textarea name="citation" class="required citation">${publication.citation?html}</textarea>  
			<span class="requiredFieldMarker" title="Required">*</span>
			<span class="ui-icon ui-icon-info" title="For example: Silfverberg, H. 2007. Changes in the list of Finnish insects 2001-2005. - Entomol. Fenn. 18:82-101"></span>
		</li>
		<li>
			<label>Link to additional information</label>
			<input type="text" name="URI" value="${publication.getURI()?html}" />
			<span class="ui-icon ui-icon-info" title="For example: https://dx.doi.org/10.1000/111 or http://urn.fi/URN:ISBN:978-951-51-0337-6"></span>
		</li>
	</ul>
	<br />
	<#if action=="add">
		<input type="submit" value="Create" class="addButton" />
	<#else>
		<input type="submit" value="Modify" class="addButton" />
	</#if>
</form>

<script>
$(function() {
	$("#publicationForm").validate();
});
</script>
<#include "luomus-footer.ftl">