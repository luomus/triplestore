<#macro actions>
	To <span class="criticalAction">move as synonym</span> or to <span class="criticalAction">detach</span> you must first
</#macro>

<div id="criticalDataDialog" class="taxonDialog" title="Manage critical data">
	<table>
		<#if taxon.hasChildren()>
			<tr>
				<td>This taxon has children:</td>
				<td><@actions/> <b>move, detach or delete the children</b></td>
			</tr>
		</#if>
		<#if taxon.secureLevel?has_content>
			<tr>
				<td>This taxon has observation secure level:</td>
				<td><@actions/> <b>contact an admin</b></td>
			</tr>
		</#if>
		<#if taxon.hasIUCNEvaluation()>
			<tr>
				<td>This taxon has IUCN evaluations:</td>
				<td><@actions/> <button id="moveEvaluationButton">Move evaluations</button></td>
			</tr>
		</#if>
		<#if taxon.hasAdministrativeStatuses()>
			<tr>
				<td>This taxon has administrative statuses:</td>
				<td><@actions/> <b>contact an admin</b></td>
			</tr>
		</#if>
		<#if taxon.hasDescriptions()>
			<tr>
				<td>This taxon has descriptions in ${taxon.contextNamesWithDescriptions}:</td>
				<td><@actions/> <button>Move descriptions</button></td>
			</tr>
		</#if>
		<#if taxon.hasTaxonImages()>
			<tr>
				<td>This taxon has taxon images:</td>
				<td><@actions/> <button>Move images</button></td>
			</tr>
		</#if>
		<#if taxon.identifierUsedInDataWarehouse>
			<tr>
				<td>This taxon's ID is used in FinBIF Data Warehouse to report observations, annotations, etc:</td>
				<td><@actions/> <b>contact an admin</b></td>
			</tr>		
		</#if>
		<#if taxon.hasExplicitlySetHigherInformalTaxonGroup()>
			<tr>
				<td>This taxon is used to define root of an informal group:</td>
				<td>Before you can do <span class="criticalAction">anything</span> you must <b>clear informal groups</b> and make appropriate changes</td>
			</tr>
		</#if>
	</table>
</div>