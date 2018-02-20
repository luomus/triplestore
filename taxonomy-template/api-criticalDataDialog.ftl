<div id="criticalDataDialog" class="taxonDialog" title="Manage critical data">
	<table>
		<tr>
			<th>Cause</th><th>Fix</th>
		</tr>
		<#if taxon.hasChildren()>
			<tr>
				<td>This taxon has children</td>
				<td>To MOVE as SYNONYM or DELETE/DETACH you must first <b>move/detach/delete the children</b>.</td>
			</tr>
		</#if>
		<#if taxon.secureLevel?has_content>
			<tr>
				<td>This taxon has observation secure level</td>
				<td>To MOVE as SYNONYM or DELETE/DETACH you must first <b>contact an admin</b>.</td>
			</tr>
		</#if>

		<tr>
			<td>This taxon has IUCN evaluation</td>
			<td>To MOVE as SYNONYM or DELETE/DETACH you must first <button>Move evaluation</button>.</td>
		</tr>
	
	</table>
</div>