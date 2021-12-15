<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<#if action == "add">
	<h1>Create a new IUCN Red List group</h1>
	<form action="${baseURL}/iucn-groups/add" method="post" id="groupForm">
<#else>
	<h1>Modify group "${group.name.forLocale("fi")!""} - ${group.name.forLocale("en")!groupQnameString}"</h1>
	<form action="${baseURL}/iucn-groups/${group.qname}" method="post" id="groupForm">
</#if>

	<ul>
		<li>
			<label>Has sub groups</label>
			<select name="MVL.hasIucnSubGroup" data-placeholder="Select group" class="chosen" multiple="multiple">
				<option value=""></option>
				<#list groups?keys as groupQnameString>
					<option value="${groupQnameString}" <#if group.hasSubGroup(groupQnameString)>selected="selected"</#if> >${groups[groupQnameString].name.forLocale("fi")!""} - ${groups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
				</#list>
			</select>
		</li>
	</ul>
	<hr />
	<ul>
		<li>
			<label>Includes informal taxon groups</label>
			<select name="MVL.includesInformalTaxonGroup" data-placeholder="Select group" class="chosen" multiple="multiple">
				<option value=""></option>
				<#list informalGroups?keys as groupQnameString>
					<option value="${groupQnameString}" <#if group.hasInformalGroup(groupQnameString)>selected="selected"</#if> >${informalGroups[groupQnameString].name.forLocale("fi")!""} - ${informalGroups[groupQnameString].name.forLocale("en")!groupQnameString}</option>
				</#list>
			</select>
		</li>
		<li>
			<label>Includes taxa</label>
			<textarea placeholder="Comma separated list of MX codes" name="MVL.includesTaxon" class="checklistName"><#list group.taxa as taxonId>${taxonId}<#if taxonId_has_next>,</#if></#list></textarea>
		</li>
		<li>
			<label>Order (number 1-n)</label>
			<input type="text" name="sortOrder" value="${group.order}" />  <span class="requiredFieldMarker" title="Required">*</span>
		</li>
	</ul>
	<hr>
	<p class="info">Note: The name is not required. If the group consists of one taxon or one informal group, the name of this Red List group is generated from the name of the taxon/informal group.</p>
	<ul>
		<li>
			<label>Name, finnish</label>
			<input type="text" name="name_fi" class="checklistName" value="${(group.name.forLocale("fi")!"")?html}" />  
		</li>
		<li>
			<label>Name, english</label>
			<input type="text" name="name_en" class="checklistName" value="${(group.name.forLocale("en")!"")?html}" />  
		</li>
		<li>
			<label>Name, swedish</label>
			<input type="text" name="name_sv" class="checklistName" value="${(group.name.forLocale("sv")!"")?html}" />  
		</li>
	</ul>
	
	<br />
	
	<#if action=="add">
		<input type="submit" value="Create" class="addButton" />
	<#else>
		<input type="submit" value="Modify" class="addButton" />
	</#if>

</form>

<form action="${baseURL}/iucn-groups/delete/${group.qname}" method="post" id="groupDeleteForm">
<input style="margin-left: 50%" type="submit" class="button deleteButton" value="Delete" onclick="return confirm('Really delete?')"/>
</form>


<#include "luomus-footer.ftl">