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
	
	<div id="addNewTaxonDialog" class="taxonDialog" title="Add new taxon">
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
				<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
						<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
				</#list>
			</select>
			<span id="taxonRankSelectPlaceholder">&nbsp;</span>
			<br />
			<input type="submit" class="button addButton" value="Add" />
			<p class="important info">IMPORTANT: Before adding a new taxon into the checklist, make sure it is not present by some other name!</p>
		</form>
	</div>
	
	<div id="addNewSynonymDialog" class="taxonDialog" title="Add new synonym">
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
				<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
						<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
				</#list>
			</select> 
			<br />
			<input type="submit" class="button addButton" value="Add"  />
		</form>
	</div>
	
	<div id="sendTaxonDialog" class="taxonDialog" title="Send taxon as child">
		<form id="sendTaxonDialogForm" onsubmit="sendTaxonAsChildDialogSubmit(); return false;">
			<input type="hidden" name="taxonToSendID" id="taxonToSendID" />
			<input type="hidden" name="newParentID" id="newParentID" />
						
			<label>Taxon to send</label>
			<span id="taxonToSendName">name</span>
			<br />
			
			<label for="newParent">New parent</label>
			<input type="text" id="newParentIDSelector" name="newParentSelector" /> <span id="newParentIdDisplay"></span><br />
			<label>&nbsp;</label> (type name or part of name and select taxon)
			<br />
			
			<input type="submit" class="button addButton" value="Send"  />
			
			<p class="info">
				The normal way to move a taxon (for example from one genus to other genus) is to open the children of both genuses side-by-side, to enable taxon dragging mode
				and to drag the desired species to the other genus. However, when you have to move taxa somewhere that is very 'far' in the taxonomy tree, it can take a lot of clicks
				to get the children side-by-side. Alternative is to use this dialogue to send the taxon to a new parent. No synonyms will be created! To automatically create synonyms, 
				use taxon dragging. 
			</p>
			<p class="info">
				After performing a send, if the new parent's children are visible, you must close and re-open the new parent's children to be able to see the sent taxon.
			</p>
			<p class="info">
				You must have permissions to the taxon being send OR to the new parent.
			</p>
		</form>
	</div>

	<div id="splitTaxonDialog" class="taxonDialog" title="Split taxon">
		<form id="splitTaxonDialogForm" target="${baseURL}/split" method="POST">
			<input type="hidden" name="rootTaxonId" id="rootTaxonId" value="${root.qname}" />
			<input type="hidden" name="taxonToSplitID" id="taxonToSplitID" />
						
			<label>Taxon to split</label>
			<span id="taxonToSplitName">name</span>
			<br />
			
			<label for="newParent">New taxons</label>
			<table>
				<thead>
					<tr>
						<th>Scientific name</th>
						<th>Authors</th>
						<th>Rank</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td><input name="scientificName___1" /></td>
						<td><input name="authors___1" /></td>
						<td>
							<select name="rank___1"> 
								<option value=""></option>
								<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
									<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
								</#list>
							</select>
					 	</td>
					</tr>
				</tbody>
			</table>
			<a class="addNewItem">+ Add new</a>
			<br />
			
			<input type="submit" class="button addButton" value="Split"  />
			
			<p class="info">
				Taxon to split is removed from the checklist. The new taxons are added to the checklist and the splitted taxon is attached as a synonym (MANY:1) of all the new taxons.
			</p>
			<p>
				The entire taxonomy tree view is reloaded after the split has been completed to show updated data.
			</p>
		</form>
	</div>


<#include "luomus-footer.ftl">