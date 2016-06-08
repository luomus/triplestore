<#macro toolbox>
		<div id="toolbox" class="ui-widget ui-corner-all">
			<div class="ui-widget-header noselect" id="toolboxToggle">Tools <span style="float: right;" class="ui-icon ui-icon-carat-1-s"></span></div>
			<div id="toolBoxContent" class="ui-widget-content">
			
			<div>
				Change year: 
				<select name="yearSelector" id="yearSelector" onchange="changeYear()">
					<#list evaluationYears as year>
						<option value="${year}" <#if selectedYear == year> selected="selected" </#if> >
							${year}
						</option>
					</#list>
				</select>
			</div>
			
			<div id="taxonSearch">
				Search taxon:
				<form onsubmit="searchTaxon(this); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="iucnEdit">
					<input type="text" placeholder="Type name of a taxon"/> <input type="submit" value="Search" />
					<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
				</form>
			</div>
			
			</div>
		</div>
</#macro>

<#macro editors groupQname>
	<#if taxonGroupEditors[groupQname]?? && taxonGroupEditors[groupQname].editors?has_content>
		<#list taxonGroupEditors[groupQname].editors as editor>
			${persons[editor.toString()].fullname}<#if editor_has_next>, </#if>
		</#list>
	<#else>
		Ei vielä määritelty
	</#if>
</#macro>

<#macro speciesRow target evaluation="NOT STARTED">
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${selectedYear}">
			<span class="scientificName speciesName">${target.scientificName!target.qname}</span>
		</a>
	</td>
	<td>
		<a href="${baseURL}/iucn/species/${target.qname}/${selectedYear}">
			${target.vernacularNameFi!""}
		</a>
	</td>
	<#if evaluation != "NOT STARTED">
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
		<td>
			<#if evaluation.hasIucnStatus()>
				<#if evaluation.hasCorrectedIndex()>
					${evaluation.correctedIucnIndex}<span class="correctedIndex">[KORJATTU]</span>
				<#else>
					${evaluation.calculatedIucnIndex}
				</#if>
			<#else>
				-
			</#if>
		</td>
	<#else>
		<td><span class="state notStarted">Ei aloitettu</span> <button class="markNEButton">NE</button></td>
		<td>-</td>
		<td>-</td>
		<td>-</td>
		<td>-</td>
	</#if>
</#macro>
