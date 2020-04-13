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
				<#if taxon.allowsMoveAsChild()>
					<option value="CHILD">child</option>
				</#if>
				
				<#if taxon.allowsMoveAsSynonym()>
				<option value="BASIONYM">Basionym / Original combination</option>
				<option value="OBJECTIVE">Objective synonym</option>
				<option value="SUBJECTIVE">Subjective synonym</option>
				<option value="HOMOTYPIC">Homotypic synonym</option>
				<option value="HETEROTYPIC">Heterotypic synonym</option>
				<option value="ALTERNATIVE">Alternative name</option>
				<option value="SYNONYM">Synonym</option>
				<option value="MISSPELLED">Misspelled name</option>
				<option value="ORTHOGRAPHIC">Orthographic variant</option>
				<option value="UNCERTAIN">Uncertain synonym</option>
				<option value="MISAPPLIED">Misapplied name</option>
				</#if>
			</select>
			
			<br />
			<label>Of</label>
			<input type="text" id="newParentIDSelector" name="newParentSelector" /> <span id="newParentIdDisplay"></span><br />
			<label>&nbsp;</label>Type name or part of name and select taxon (or type MX-id)
			<br />
			
			<div class="errorTxt"></div>
			
			<input type="submit" class="button addButton" value="Move"  />
			
			
		</form>
	</div>
