<#include "luomus-header.ftl">

<h1>Namespaces</h1>

<table id="namespaces" class="public">
	<tr>
		<th>Namespace</th>
		<th>Person in charge</th>
		<th>Purpose</th>
		<th>Type</th>
		<th>Qname prefix</th>
	</tr>
	<#list namespaces as n>
		<tr>
			<th>${n.namespace?html}</th>
			<td>${n.personInCharge?html}</td>
			<td>${n.purpose?html}</td>
			<td>${n.type?html}</td>
			<td>${n.qnamePrefix?html}</td>
		</tr>
	</#list>
</table>	

<div style="height: 400px;"></div>

<#include "luomus-footer.ftl">