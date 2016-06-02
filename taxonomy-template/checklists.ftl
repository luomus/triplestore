<#include "luomus-header.ftl">

<#if successMessage?has_content>
	<p class="successMessage">${successMessage}</p>
</#if>

<h1>Create and modify checklists</h1>

<p><a href="${baseURL}/checklists/add">&raquo; Create new checklist</a></p>
 
<table class="resourceListTable">
	<thead>
		<tr>
			<th>Id</th>
			<th>Name</th>
			<th>&nbsp;</th>
			<th>&nbsp;</th>
			<th>Owner</th>
			<th>Public?</th>
			<th>&nbsp;</th>
		</tr>
	</thead>
	<tbody>
	<#list checklists?values as checklist>
		<tr>
			<td><a href="${baseURL}/checklists/${checklist.qname}">${checklist.qname}</a></td>
			<td>${checklist.getFullname("en")}</td>
			<td>
				<#if checklist.rootTaxon.set>
					<a href="${baseURL}/${checklist.qname}">Go to taxonomy tree</a>
				<#else>
					No root
				</#if>
			</td>
			<td><a href="${baseURL}/orphan/${checklist.qname}">Go to orphan taxa</a></td>
			<td>
				<#if checklist.owner?has_content>
					${persons[checklist.owner.toString()].fullname}
				</#if>
			</td>
			<td>${checklist.public?string("Public","Hidden")}</td>
			<td><a class="button" href="${baseURL}/checklists/${checklist.qname}">Modify</button></td>
		</tr>
	</#list>
	</tbody>
</table>

<#include "luomus-footer.ftl">