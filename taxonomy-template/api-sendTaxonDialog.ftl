<#include "macro.ftl">

	<div id="sendTaxonDialog" class="taxonDialog" title="Move taxon">
		<form id="sendTaxonDialogForm" onsubmit="sendTaxonDialogSubmit(); return false;">
			<input type="hidden" name="taxonToSendID" id="taxonToSendID" value="${taxon.qname}" />
			<input type="hidden" name="newParentID" id="newParentID" />
						
			<label>Move</label>
			<b><@printScientificNameAndAuthor taxon /></b> &nbsp; 
			<#if taxon.synonym>
				which is synonym of <@printScientificNameAndAuthor taxon.synonymParent />
			<#else>
				of ${checklists[taxon.checklist.toString()].getFullname("en")!taxon.checklist}
			</#if>
			<br />
			
			<label>As</label>
			<select name="sendAsType" id="sendAsType">
				<#if !taxon.hasTreeRelatedCriticalData()>
					<option value="CHILD">child</option>
				<#else>
					<option disabled="disabled" value="CHILD">child</option>
				</#if>

				<#assign disabled=""/><#if taxon.hasCriticalData()><#assign disabled=" disabled=\"disabled\" "/></#if>
				<option ${disabled} value="BASIONYM">basionym</option>
				<option ${disabled} value="OBJECTIVE">objective synonym</option>
				<option ${disabled} value="SUBJECTIVE">subjective synonym</option>
				<option ${disabled} value="HOMOTYPIC">homotypic synonym</option>
				<option ${disabled} value="HETEROTYPIC">heterotypic synonym</option>
				<option ${disabled} value="SYNONYM">synonym</option>
				<option ${disabled} value="MISSPELLED">misspelled name</option>
				<option ${disabled} value="ORTOGRAPHIC">ortographic synonym</option>
				<option ${disabled} value="UNCERTAIN">uncertain synonym</option>
				<option ${disabled} value="MISAPPLIED">misapplied name</option>
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
