<#include "luomus-header.ftl">

<h1>Namespaces - edit</h1>

<table id="namespaces">
	<tr>
		<th>Namespace</th>
		<th>Person in charge</th>
		<th>Purpose</th>
		<th>Type</th>
		<th>Qname prefix</th>
		<th>&nbsp;</th>
	</tr>
	<#list namespaces as n>
		<tr>
			<th>${n.namespace?html} <input type="hidden" name="namespace" value="${n.namespace?html}" /></th>
			<td><input type="text" name="personInCharge" value="${n.personInCharge?html}" /></td>
			<td><input type="text" name="purpose" value="${n.purpose?html}" size="50" /></td>
			<td><input type="text" name="type" value="${n.type?html}" /></td>
			<td><input type="text" name="qnamePrefix" value="${n.qnamePrefix?html}" /></td>
			<td><button class="upsert">Modify</button>
		</tr>
	</#list>
	<tr>
			<th><input type="text" name="namespace" value="" placeholder="Namespace" /></th>
			<td><input type="text" name="personInCharge" value="" placeholder="Person in charge" /></td>
			<td><input type="text" name="purpose" value="" placeholder="Purpose" size="50" /></td>
			<td><input type="text" name="type" value="" placeholder="Type" /></td>
			<td><input type="text" name="qnamePrefix" value="" placeholder="Qname prefix" /></td>
			<td><button class="upsert">Add new</button>
		</tr>
</table>	


<div style="height: 400px;"></div>

<script>
$(function() {
	$(".upsert").on('click', function() {
		var data = $(this).closest('tr').find('input').serialize();
		var form = $('<form>', {
        	action: '${baseURL}/namespaces-edit?'+data,
        	method: 'post'
    	});
   		form.appendTo($(document.body)).submit();
	});
});
</script>

<#include "luomus-footer.ftl">