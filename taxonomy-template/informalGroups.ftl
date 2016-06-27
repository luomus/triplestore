<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<h1>Create and modify informal groups</h1>

<p><a href="${baseURL}/informalGroups/add">&raquo; Create new group</a></p>
 
<table class="resourceListTable informalGroupsTable">
	<thead>
		<tr>
			<th>Id</th>
			<th>Finnish</th>
			<th>English</th>
			<th>Swedish</th>
			<th>&nbsp;</th>
		</tr>
	</thead>
	<tbody>
	<#list roots as rootQname>
		<@printGroup informalGroups[rootQname] />
	</#list>
	</tbody>
</table>

<#macro printGroup group indent=0>
	<tr>
		<td><a href="${baseURL}/informalGroups/${group.qname}">${group.qname}</a></td>
		<td> <span class="indent indent_${indent}">&mdash;</span> ${group.getName("fi")!""}</td>
		<td> <span class="indent indent_${indent}">&mdash;</span> ${group.getName("en")!""}</td>
		<td> <span class="indent indent_${indent}">&mdash;</span> ${group.getName("sv")!""}</td>
		<td><a class="button" href="${baseURL}/informalGroups/${group.qname}">Modify</button></td>
	</tr>
	<#list informalGroups?values as subGroupCandidate>
		<#if group.hasSubGroup(subGroupCandidate.qname)>
			<@printGroup subGroupCandidate indent + 1 />
		</#if>
	</#list>
</#macro>

<#include "luomus-footer.ftl">