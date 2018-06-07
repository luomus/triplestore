<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>


<#if user.admin>
	<h1>Create and modify IUCN Red List groups</h1>
	<p><a href="${baseURL}/iucn-groups/add">&raquo; Create a new group</a></p>
<#else>
	<h1>IUCN Red List groups</h1>	
 </#if>

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
			<@printGroup groups[rootQname] 0 "odd" />
		<#else>
			<@printGroup groups[rootQname] 0 "even" />
		</#if>
	</#list>
	</tbody>
</table>

<#macro printGroup group indent evenOdd>
	<tr class="indent_${indent} ${evenOdd}">
		<#if user.admin>
			<td> <span class="indent">&mdash;</span> <a href="${baseURL}/iucn-groups/${group.qname}"> ${group.getName("fi")!""}</a> </td>
			<td> <span class="indent">&mdash;</span> <a href="${baseURL}/iucn-groups/${group.qname}"> ${group.getName("en")!""}</a> </td>
			<td> <span class="indent">&mdash;</span> <a href="${baseURL}/iucn-groups/${group.qname}"> ${group.getName("sv")!""}</a> </td>
			<td> <a class="button" href="${baseURL}/iucn-groups/${group.qname}">Modify</a> </td>
		<#else>
			<td> <span class="indent">&mdash;</span> <span title="${group.qname}"> ${group.getName("fi")!""} </span> </td>
			<td> <span class="indent">&mdash;</span> <span title="${group.qname}"> ${group.getName("en")!""} </span> </td>
			<td> <span class="indent">&mdash;</span> <span title="${group.qname}"> ${group.getName("sv")!""} </span> </td>
			<td> &nbsp; </td>
		</#if>
	</tr>
	<#list groups?values as subGroupCandidate>
		<#if group.hasSubGroup(subGroupCandidate.qname)>
			<@printGroup subGroupCandidate indent + 1 evenOdd />
		</#if>
	</#list>
</#macro>

<#include "luomus-footer.ftl">