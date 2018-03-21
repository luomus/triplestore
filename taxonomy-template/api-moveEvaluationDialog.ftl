<div id="moveEvaluationDialog" class="taxonDialog" title="Move IUCN Red List evaluations">

	<form id="moveEvaluationDialogForm" onsubmit="moveEvaluationDialogSubmit(); return false;">
		<input type="hidden" name="taxonID" id="taxonID" value="${taxon.qname}" />
		<input type="hidden" name="newTargetID" id="newTargetID" />
		
		<table class="redListTable">
			<thead>
				<tr><th>Year</th><th>Status set to taxon</th><th>Status set to evaluation</th><th>Select years to move</th></tr>
			</thead>
			<tbody>
			<#list evaluationYears as year>
				<tr>
					<th>${year}</th>
					<td>
						<#if taxon.getRedListStatusForYear(year)??>
							${redListStatusProperty.range.getValueFor(taxon.getRedListStatusForYear(year)).label.forLocale("fi")}
						<#else>
							-
						</#if>
					</td>
					<td>
						<#if target.hasEvaluation(year)>
							${redListStatusProperty.range.getValueFor(target.getEvaluation(year).iucnStatus).label.forLocale("fi")}
						<#else>
							-
						</#if>
					</td>
					<td>
						<#if taxon.getRedListStatusForYear(year)?? || target.hasEvaluation(year)>
							<input type="checkbox" name="evaluationYears" value="${year}" />
						</#if>
					</td>
				</tr>
			</#list>
			</tbody>
		</table>
	
		<br />
		
		<label>Select target taxon</label>
		
		<input type="text" id="newTargetIDSelector" name="newTargetSelector" /> <span id="newTargetIdDisplay"></span><br />
		<label>&nbsp;</label>Type name or part of name and select taxon (or type MX-id)
		<br />
			
		<div class="errorTxt"></div>
			
		<input type="submit" class="button addButton" value="Move"  />
			
	</form>
</div>