<#include "macro.ftl">

	<div id="sendTaxonDialog" class="taxonDialog" title="Move taxon">
		<form id="sendTaxonDialogForm" onsubmit="sendTaxonDialogSubmit(); return false;">
			<input type="hidden" name="taxonToSendID" id="taxonToSendID" value="${taxon.qname}" />
			<input type="hidden" name="newParentID" id="newParentID" />
						
			<label>Move</label>
			<b><@printScientificNameAndAuthor taxon /></b> &nbsp; 
			<#if taxon.synonym>
				<#if taxon.synonymParent??>
					which is synonym of <@printScientificNameAndAuthor taxon.synonymParent />
				<#else>
					which is an orphan taxa
				</#if>
			<#else>
				of ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
			</#if>
			<br />
			
			<label>As</label>
			<select name="sendAsType" id="sendAsType">
				<#if !taxon.hasTreeRelatedCriticalData()>
					<option value="CHILD">child</option>
				<#else>
					<option disabled="disabled" value="CHILD">Child</option>
				</#if>

				<#assign disabled=""/><#if taxon.hasCriticalData()><#assign disabled=" disabled=\"disabled\" "/></#if>
				<option ${disabled} value="BASIONYM">Basionym</option>
				<option ${disabled} value="OBJECTIVE">Objective synonym</option>
				<option ${disabled} value="SUBJECTIVE">Subjective synonym</option>
				<option ${disabled} value="HOMOTYPIC">Homotypic synonym</option>
				<option ${disabled} value="HETEROTYPIC">Heterotypic synonym</option>
				<option ${disabled} value="SYNONYM">Synonym</option>
				<option ${disabled} value="MISSPELLED">Misspelled name</option>
				<option ${disabled} value="ORTHOGRAPHIC">Orthographic variant</option>
				<option ${disabled} value="UNCERTAIN">Uncertain synonym</option>
				<option ${disabled} value="MISAPPLIED">Misapplied name</option>
			</select>
			
			<#if taxon.hasCriticalData()>
				<div style="display: inline-block; margin-left: 1em;">
					<span class="ui-icon ui-icon-alert"></span> 
					Some operations are not permitted 
					<button id="sendTaxonManageCriticalButton">Manage critical data</button>
				</div>
			</#if>

			<br />
			<label>Of</label>
			<input type="text" id="newParentIDSelector" name="newParentSelector" /> <span id="newParentIdDisplay"></span><br />
			<label>&nbsp;</label>Type name or part of name and select taxon (or type MX-id)
			<br />
			
			<div class="errorTxt"></div>
			
			<input type="submit" class="button addButton" value="Move"  />
			
			
		</form>
	</div>
