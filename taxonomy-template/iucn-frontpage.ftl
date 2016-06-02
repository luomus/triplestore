<#include "luomus-header.ftl">

<h1>Uhanalaisuusarviointi - ${selectedYear} <#if draftYear == selectedYear>(LUONNOS)</#if></h1>

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
		
<table class="resourceListTable">
	<thead>
		<tr>
			<th>Eliöryhmä</th>
			<th>Tila</th>
			<th>Uhanalaisuusarvioijat</th>
			<#if user.isAdmin??><th></th></#if>
		</tr>
	</thead>
	<tbody>
		<#list taxonGroups?values as taxonGroup>
		<tr>
			<td>${taxonGroup.name.forLocale("fi")}</td>
			<td>tila..</td>
			<td>henkilöt...</td>
			<#if user.isAdmin??><td><button>Modify editors (admin only)</button></td></#if>
		</tr>
		</#list>
	</tbody>
</table>

<p class="info">Voit siirtyä arvioitavien lajien luetteloon valitsemalla eliöryhmän.</p>

<#include "luomus-footer.ftl">