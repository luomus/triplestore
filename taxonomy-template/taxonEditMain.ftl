<#include "luomus-header.ftl">

	<div class="ui-widget-content">
	
		<div id="toolbox" class="ui-widget ui-corner-all">
			<div class="ui-widget-header noselect" id="toolboxToggle">Tools <span style="float: right;" class="ui-icon ui-icon-carat-1-s"></span></div>
			<div id="toolBoxContent" class="ui-widget-content">
			
			<div id="checklistSelectorTool">
				Working with checklist: 
				<select name="checklistSelector" id="checklistSelector">
					<option value=""></option>
					<#list checklists?values as checklist>
						<#if checklist.rootTaxon.set>
							<option value="${checklist.qname}" <#if same(root.checklist, checklist.qname)> selected="selected" </#if> >
								${checklist.getFullname("en")}
							</option>
						</#if>
					</#list>
				</select>
				<button id="changeChecklistButton" onclick="changeChecklist(); return false;">Change</button>
			</div>
		
			<table>
				<tr>
					<td>Synonyms:</td>
					<td>
						<div id="synonymModeSelectorTool" class="switchContainer"><input type="checkbox" id="synonymMode" name="synonymMode" onchange="changeSynonymMode();" /></div>
						<span class="ui-icon ui-icon-info" title="Showing synonyms makes browsing slower"></span>
					</td>
				</tr>
				<tr>
					<td>Taxon dragging:</td> 
					<td><div id="taxonDragModeSelectorTool" class="switchContainer"><input type="checkbox" id="taxonDragMode" name="taxonDragMode" onchange="changeTaxonDragMode();"/></div></td>
				</tr>
			</table>
			
			<#if user.admin>
				<p><button id="clearCaches" onclick="clearCaches(); return false;">Clear all caches (admin only)</button></p>
			</#if>
						
			<div id="taxonSearch">
				Search taxon:
				<form onsubmit="searchTaxon(this); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="taxonTree">
					<input type="text" placeholder="Type name of a taxon"/> <input type="submit" value="Search" />
					<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
				</form>
			</div>
			
			</div>
		</div>
		
		<div id="taxonTree">
			<#include "taxonTreeContent.ftl">
		</div>
		
		<div class="clear"></div>
		
	</div>
	
	<div class="clear"></div>
	
	<div id="editTaxon" class="ui-widget-content">
			<h3 class="ui-widget-header ui-state-active">Edit taxon</h3>
			<div id="editTaxonContent">
				<#include "api-taxonedit.ftl" />
			</div>
	</div>
	
	<div id="addNewTaxonDialog" title="Add new taxon">
		<form id="addNewTaxonDialogForm" onsubmit="addNewChildDialogSubmit(); return false;">
			<input type="hidden" name="newTaxonParent" id="newTaxonParent" />
			
			<label>Parent</label>
			<span id="newTaxonParentName">parent</span>
			<br />
			<label for="newTaxonScientificName">Scientific name</label>
			<input type="text" id="newTaxonScientificName" name="newTaxonScientificName" /> 
			<br />
			<label for="newTaxonAuthor">Author</label>
			<input type="text" id="newTaxonAuthor" name="newTaxonAuthor" />
			<br />
			<label for="newTaxonTaxonrank">Taxon rank</label>
			<select id="allTaxonRanksSelect" class="hidden"> 
				<option value=""></option>
				<#list properties.getProperty("dwc:taxonRank").range.values as taxonRank>
						<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
				</#list>
			</select>
			<span id="taxonRankSelectPlaceholder">&nbsp;</span>
			<br />
			<input type="submit" class="button addButton" value="Add" />
			<p class="important info">IMPORTANT: Before adding a new taxon into the checklist, make sure it is not present by some other name!</p>
		</form>
	</div>
	
	<div id="addNewSynonymDialog" title="Add new synonym">
		<form id="addNewSynonymDialogForm" onsubmit="addNewSynonymDialogSubmit(); return false;">
			<input type="hidden" name="synonymOfTaxon" id="synonymOfTaxon" />
			<label>Synonym of</label>
			<span id="synonymOfTaxonName">parent</span>
			<br />
			<label for="newSynonymScientificName">Scientific name</label>
			<input type="text" id="newSynonymScientificName" name="newSynonymScientificName" /> 
			<br />
			<label for="newSynonymAuthor">Author</label>
			<input type="text" id="newSynonymAuthor" name="newSynonymAuthor" />
			<br />
			<label for="newSynonymTaxonrank">Taxon rank</label>
			<select id="newSynonymTaxonrank" name="newSynonymTaxonrank"> 
				<option value=""></option>
				<#list properties.getProperty("dwc:taxonRank").range.values as taxonRank>
						<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
				</#list>
			</select> 
			<br />
			<input type="submit" class="button addButton" value="Add"  />
		</form>
	</div>
	
<#include "luomus-footer.ftl">