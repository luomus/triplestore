<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<h1>Create and modify informal groups</h1>

<p><a href="${baseURL}/informalGroups/add">&raquo; Create new group</a></p>
 
<table class="checklistTable">
	<thead>
		<tr>
			<td>Id</td>
			<td>Finnish</td>
			<td>English</td>
			<td>Swedish</td>
			<td>&nbsp;</td>
		</tr>
	</thead>
	<tbody>
	<#list informalGroups?values as group>
		<tr>
			<td><a href="${baseURL}/informalGroups/${group.qname}">${group.qname}</a></td>
			<td>${group.getName("fi")}</td>
			<td>${group.getName("en")}</td>
			<td>${group.getName("sv")}</td>
			<td><a class="button" href="${baseURL}/informalGroups/${group.qname}">Modify</button></td>
		</tr>
	</#list>
	</tbody>
</table>

<#include "luomus-footer.ftl">