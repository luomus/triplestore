<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<h1>Create and modify informal groups</h1>

<p><a href="${baseURL}/informalGroups/add">&raquo; Create new group</a></p>
 
<table class="resourceListTable informalGroupsTable">
	<thead>
		<tr>
			<th>Finnish</th>
			<th>English</th>
			<th>Swedish</th>
			<th>&nbsp;</th>
		</tr>
	</thead>
	<tbody>
	<#list roots as rootQname>
		<#if (rootQname_index) % 2 == 0>
			<@printGroup informalGroups[rootQname] 0 "even" />
		<#else>
			<@printGroup informalGroups[rootQname] 0 "odd" />
		</#if>
	</#list>
	</tbody>
</table>

<#macro printGroup group indent evenOdd>
	<tr class="indent_${indent} ${evenOdd}">
		<td> <span class="indent">&mdash;</span> <a href="${baseURL}/informalGroups/${group.qname}"> ${group.getName("fi")!""}</a> </td>
		<td> <span class="indent">&mdash;</span> <a href="${baseURL}/informalGroups/${group.qname}"> ${group.getName("en")!""}</a> </td>
		<td> <span class="indent">&mdash;</span> <a href="${baseURL}/informalGroups/${group.qname}"> ${group.getName("sv")!""}</a> </td>
		<td><a class="button" href="${baseURL}/informalGroups/${group.qname}">Modify</a></td>
	</tr>
	<#list informalGroups?values as subGroupCandidate>
		<#if group.hasSubGroup(subGroupCandidate.qname)>
			<@printGroup subGroupCandidate indent + 1 evenOdd />
		</#if>
	</#list>
</#macro>

<#include "luomus-footer.ftl">