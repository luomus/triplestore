<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<#if action == "add">
	<h1>Create a new informal group</h1>
	<form action="${baseURL}/informalGroups/add" method="post" id="groupForm">
<#else>
	<h1>Modify informal group "${group.name.forLocale("fi")!""} - ${group.name.forLocale("en")!groupQnameString}"</h1>
	<form action="${baseURL}/informalGroups/${group.qname}" method="post" id="groupForm">
</#if>

	<ul>
		<li>
			<label>Name, finnish</label>
			<input type="text" name="name_fi" class="required checklistName" value="${(group.name.forLocale("fi")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, english</label>
			<input type="text" name="name_en" class="required checklistName" value="${(group.name.forLocale("en")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, swedish</label>
			<input type="text" name="name_sv" class="required checklistName" value="${(group.name.forLocale("sv")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Has sub groups</label>
			<select name="MVL.hasSubGroup" data-placeholder="Select group" class="chosen" multiple="multiple">
				<option value=""></option>
				<#list informalGroups?keys as groupQnameString>
					<option value="${groupQnameString}" <#if group.hasSubGroup(groupQnameString)>selected="selected"</#if> >${informalGroups[groupQnameString].name.forLocale("fi")!""} - ${informalGroups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
				</#list>
			</select>
		</li>
	</ul>
	<br />
	<#if action=="add">
		<input type="submit" value="Create" class="addButton" />
	<#else>
		<input type="submit" value="Modify" class="addButton" />
	</#if>
</form>

<form action="${baseURL}/informalGroups/delete/${group.qname}" method="post" id="groupDeleteForm">
	<input style="margin-left: 50%" type="submit" class="button deleteButton" value="Delete" />
</form>

<script>
$(function() {
	$("#groupForm").validate();
});
</script>
<#include "luomus-footer.ftl">