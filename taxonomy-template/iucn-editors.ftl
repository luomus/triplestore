<#include "luomus-header.ftl">

<h1>Uhanalaisuusarvioinnin editorit - ${group.name.forLocale("fi")}</h1>

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<form action="${baseURL}/iucn/editors/${group.qname}" method="post" id="groupForm">
	<input type="hidden" name="editorsId" value="${(editors.id)!""}" />
	<ul>
	<#if editors??>
	<#list editors.editors as editor>
		<li>
			<select name="editor" data-placeholder="Select person" class="chosen">
				<option value=""></option>
				<#list persons?keys as personQname>
					<option value="${personQname}" <#if same(editor, personQname)>selected="selected"</#if> >${persons[personQname].fullname}</option>
				</#list>
			</select>
		</li>
	</#list>
	</#if>
	<li>
		<select name="editor" data-placeholder="Select person" class="chosen">
			<option value=""></option>
			<#list persons?keys as personQname>
				<option value="${personQname}">${persons[personQname].fullname}</option>
			</#list>
		</select>
	</li>
	</ul>
	<input type="submit" value="Save" />
</form>



<#include "luomus-footer.ftl">