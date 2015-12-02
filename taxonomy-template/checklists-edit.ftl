<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<#if action == "add">
	<h1>Create a new checklist</h1>
	<form action="${baseURL}/checklists/add" method="post" id="checklistForm">
<#else>
	<h1>Modify checklist "${checklist.fullname.forLocale("en")}"</h1>
	<form action="${baseURL}/checklists/${checklist.qname}" method="post" id="checklistForm">
</#if>

	<ul>
		<li>
			<label>Name, english</label>
			<input type="text" name="name_en" class="required checklistName" value="${(checklist.fullname.forLocale("en")!"")?html}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
		<li>
			<label>Name, finnish</label>
			<input type="text" name="name_fi" class="checklistName" value="${(checklist.fullname.forLocale("fi")!"")?html}" />
		</li>
		
		<li>
			<label>Owner</label>
			<select name="owner" class="chosen">
				<option value=""></option>
				<#list persons?keys as personQname>
    				<option value="${personQname}" <#if same(personQname, checklist.owner)>selected="selected"</#if> >${persons[personQname].fullname}</option>
				</#list>
			</select>
		</li>
		<li>
			<label>Publicity?</label>
			<select name="isPublic" class="chosen">
				<option value="true" <#if checklist.public>selected="selected"</#if>>Public</option>
				<option value="false"<#if !checklist.public>selected="selected"</#if>>Hidden</option>
			</select> 
			<span class="requiredFieldMarker" title="Required">*</span>
			<span class="ui-icon ui-icon-info" title="Public checklists are browsable for example using the Laji.fi -website."></span>
		</li>
		<#if action=="add">
		<li>
			<label>Create root taxon?</label>
			<select name="createRoot" class="chosen">
				<option value="yes">Yes</option>
				<option value="no">No</option>
			</select> 
			<span class="requiredFieldMarker" title="Required">*</span>
			<span class="ui-icon ui-icon-info" title="If you select 'Yes', a new taxon is created and set as the root of the checklist. This is what you usually want, so select 'Yes'. You can modify the taxon by going into taxonomy tree. "></span>
		</li>
		<#else>
			<input type="hidden" name="rootTaxonQname" value="${checklist.rootTaxon}" />
		</#if>
		
		<li>
			<label>Notes, english</label>
			<textarea name="notes_en" rows="3" cols="70">${(checklist.notes.forLocale("en")!"")?html}</textarea>
		</li>
		<li>
			<label>Notes, finnish</label>
			<textarea name="notes_fi" rows="3" cols="70">${(checklist.notes.forLocale("fi")!"")?html}</textarea>
		</li>
		
	</ul>
	<br />
	<#if action=="add">
		<input type="submit" value="Create" class="addButton" />
	<#else>
		<#if same(user.qname, checklist.owner) || user.isAdmin()><input type="submit" value="Modify" class="addButton" /></#if>
	</#if>
</form>

<script>
$(function() {
	$("#checklistForm").validate();
});
</script>
<#include "luomus-footer.ftl">