<#macro toolbox>
		<div id="toolbox" class="iucnToolbox ui-widget ui-corner-all">
			<div class="ui-widget-header noselect" id="toolboxToggle">Työkalut <span style="float: right;" class="ui-icon ui-icon-carat-1-s"></span></div>
			<div id="toolBoxContent" class="ui-widget-content">
			
			<div>
				Vaihda arviointikautta: 
				<select name="yearSelector" id="yearSelector" onchange="changeYear()">
					<#list evaluationYears?reverse as year>
						<option value="${year}" <#if selectedYear == year> selected="selected" </#if> >
							${year}
						</option>
					</#list>
				</select>
			</div>
			
			<div id="taxonSearch">
				Etsi lajilla:
				<form onsubmit="searchTaxon(this, true, true); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="iucnEdit">
					<input type="text" placeholder="Kirjoita nimi tai nimen osa"/> <input type="submit" value="Hae" />
					<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
				</form>
			</div>
			
			</div>
		</div>
</#macro>

<#macro editors groupQname>
	<#if taxonGroupEditors[groupQname]??>
		<#list taxonGroupEditors[groupQname].editors as editor>
			${persons[editor.toString()].fullname}<#if editor_has_next>, </#if>
		</#list>
	<#else>
		Ei vielä määritelty
	</#if>
</#macro>

<#macro speciesRow target year>
	<td>
		${target.orderAndFamily}
	</td>
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${year}">
			<span class="scientificName speciesName">${target.scientificName!target.qname}</span>
		</a>
		<span class="synonyms scientificName speciesName">${target.synonymNames}</span>
	</td>
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${year}">
			${target.vernacularNameFi!""}
		</a>
	</td>
	<#if target.hasEvaluation(year)>
		<#assign evaluation = target.getEvaluation(year)>
		<td>
			<#if evaluation.ready>
				<span class="state ready">Valmis</span>
			<#else>
				<span class="state started">Aloitettu</span>
			</#if>
		</td>
		<td>${(evaluation.lastModified?string("d.M.yyyy"))!"-"}</td>
		<td>
			<#if evaluation.lastModifiedBy??>
				${persons[evaluation.lastModifiedBy].fullname}
			<#else>
				-
			</#if>
		</td>
		<td>
			<#if evaluation.hasIucnStatus()>
				${statusProperty.range.getValueFor(evaluation.iucnStatus).label.forLocale("fi")}
		    <#else>
		    	-
		    </#if>
		</td>
		<td class="redListIndexTableField">
			<#if evaluation.hasIucnStatus()>
				<#if evaluation.hasCorrectedIndex()>
					${evaluation.correctedIucnIndex} <span class="correctedIndex">[KORJATTU]</span>
				<#else>
					${evaluation.calculatedIucnIndex!"-"}
				</#if>
			<#else>
				-
			</#if>
		</td>
	<#else>
		<td><span class="state notStarted">Ei aloitettu</span> <#if permissions><button class="markNEButton">NE</button></#if></td>
		<td>-</td>
		<td>-</td>
		<td>-</td>
		<td class="redListIndexTableField">-</td>
	</#if>
	<td>
		<#if target.hasPreviousEvaluation(year)>
			<#assign prevEvaluation = target.getPreviousEvaluation(year)>
			<#if prevEvaluation.hasIucnStatus()>
				${prevEvaluation.iucnStatus?replace("MX.iucn", "")}
				(${prevEvaluation.evaluationYear})
			<#else>
				-
			</#if>
		<#else>
			-
		</#if>
	</td>
	<td>
		<#if target.hasEvaluation(year)>
			<#if target.getEvaluation(year).hasRemarks()>
				<span class="ui-icon ui-icon-comment remarks" title="${target.getEvaluation(year).remarks?html}"></span>
			</#if> 
		</#if>
	</td>
</#macro>
