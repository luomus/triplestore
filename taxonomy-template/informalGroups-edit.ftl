<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<#if action == "add">
	<h1>Create a new informal group</h1>
	<form action="${baseURL}/informalGroups/add" method="post" id="groupForm">
<#else>
	<h1>Modify informal group "${group.name.forLocale("en")}"</h1>
	<form action="${baseURL}/informalGroups/${group.qname}" method="post" id="groupForm">
</#if>

	<ul>
		<li>
			<label>Name, english</label>
			<input type="text" name="name_en" class="required checklistName" value="${(group.name.forLocale("en")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, finnish</label>
			<input type="text" name="name_fi" class="required checklistName" value="${(group.name.forLocale("fi")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, swedish</label>
			<input type="text" name="name_sv" class="required checklistName" value="${(group.name.forLocale("sv")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, latin</label>
			<input type="text" name="name_la" class="required checklistName" value="${(group.name.forLocale("la")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
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
	$("#groupForm").validate();
});
</script>
<#include "luomus-footer.ftl">