<#include "luomus-header.ftl">

	<div class="ui-widget-content">
	
		<div id="toolbox" class="ui-widget ui-corner-all">
			<div class="ui-widget-header noselect" id="toolboxToggle">Tools <span style="float: right;" class="ui-icon ui-icon-carat-1-s"></span></div>
			<div id="toolBoxContent" class="ui-widget-content">
			<#if user.admin>
				<p><button id="clearCaches" onclick="clearCaches(); return false;">Clear all caches (Admin only)</button></p>
				<div id="checklistSelectorTool">
					Working with checklist (Admin only): 
					<select name="checklistSelector" id="checklistSelector">
					<option value=""></option>
					<#list checklists?values as checklist>
						<#if checklist.rootTaxon.set>
							<option value="${checklist.qname}" <#if ((root.checklist.toString())!"X") == checklist.qname.toString()> selected="selected" </#if> >
								${checklist.getFullname("en")}
							</option>
						</#if>
					</#list>
					</select>
					<button id="changeChecklistButton" onclick="changeChecklist(); return false;">Change</button>
				</div>
				<br />
			</#if>
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
	
	<div id="addNewSynonymDialog" class="taxonDialog" title="Add synonyms">
		<form id="addNewSynonymDialogForm" onsubmit="addNewSynonymDialogSubmit(); return false;">
			<input type="hidden" name="synonymOfTaxon" id="synonymOfTaxon" />
			<label>Synonym of</label>
			<span id="synonymOfTaxonName">parent</span>
			<br />
			<label for="synonymType">Type of relationship</label>
			<select name="synonymType" id="synonymType">
				<option value="SYNONYM" selected="selected">Full synonym</option>
				<option value="MISAPPLIED">Misapplied</option>
				<option value="INCLUDES">Includes</option>
				<option value="INCLUDED_IN">Included in</option>
				<option value="UNCERTAIN">Uncertain</option>
			</select>
			<br />
			<br />
			<h4>Create new orphan synonyms</h4>
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
							<select name="rank___1" class="taxonRankSelect"> 
								<option value=""></option>
								<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
									<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
								</#list>
							</select>
					 	</td>
					</tr>
				</tbody>
			</table>
			<a class="addNewItem addNewSynonymRow">+ Add new</a>
			<input type="submit" class="button addButton" value="Create synonyms"  />
			<br /><br />
			<h4>Or select existing taxa from other checklist</h4>
			<label for="synonymChecklistSelector">Checklist</label>
			<br />
			<select id="synonymChecklistSelector">
				<option value=""></option>
				<#list checklists?values as checklist>
					<#if checklist.public && ((root.checklist.toString())!"") != checklist.qname.toString()>
						<option value="${checklist.qname}">${checklist.getFullname("en")}</option>
					</#if>
				</#list>
			</select>
			<br />
			<label>Taxon</label>
			<div class="synonymTaxonIdSelectorContainer">
				<input type="hidden" name="synonymTaxonId" class="synonymTaxonId" />
				<input type="text" class="synonymTaxonIdSelector" name="synonymTaxonIdSelector" /> <span class="synonymTaxonIdSelectorIdDisplay"></span><br />
			</div>
			<a class="addNewItem addNewSynonymId">+ Add new</a>
			<input type="submit" class="button addButton" value="Add synonyms"  />
			<br /><br />
			<ul class="info">
				<li><b>Synonym:</b> The synonym taxon and this taxon share the same taxonomic circumscription.</li>
				<li><b>Misapplied:</b> The misapplied taxon (scientific name) has been erroneously used in Finland to report observations of this taxon. (They do not share the same circumscription.)</li>
				<li><b>Includes:</b> This taxon concept includes the other taxas concepts.</li>
				<li><b>Included in:</b> This taxon concept is included in the other taxas concepts.</li>
				<li><b>Uncertain:</b> Same as synonym except it is not certain that the taxa share the same circumscription.</li>
			</ul>
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
				to get the children side-by-side. The alternative approach is to use this dialogue to send the taxon to a new parent. No synonyms will be created! To automatically 
				create synonyms, use taxon dragging. 
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
		<form id="splitTaxonDialogForm" action="${baseURL}/split" method="POST">
			<input type="hidden" name="rootTaxonId" class="rootTaxonId" value="${root.qname}" />
			<input type="hidden" name="taxonToSplitID" class="taxonToSplitID" />
						
			<label>Taxon to split</label>
			<span id="taxonToSplitName">name</span>
			<br />
			
			<label for="newParent">New taxa</label>
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
						<td><input name="scientificName___1" required /></td>
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
					<tr>
						<td><input name="scientificName___2" required /></td>
						<td><input name="authors___2" /></td>
						<td>
							<select name="rank___2"> 
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
				The taxon that is splitted is removed from the checklist. The new taxa are added to the checklist and their concepts are linked with the concept of the splitted taxon.
			</p>
			<p class="info">
				Note: Taxonomy tree view will be reloaded after the split has been completed to show updated data.
			</p>
		</form>
	</div>

	<div id="mergeTaxonDialog" class="taxonDialog" title="Merge taxa">
		<form id="mergeTaxonDialogForm" action="${baseURL}/merge" method="POST">
			<input type="hidden" name="rootTaxonId" id="rootTaxonId" value="${root.qname}" />
			<input type="hidden" name="taxonToMergeId" id="initialTaxonToMergeId" required />
						
			<label>Taxon to merge</label>
			<span id="initialTaxonToMergeName">name</span>
			<br />
			
			<label>Merge with taxa</label>
			(type name or part of name and select taxon)
			<div class="taxonToMergeIdSelectorContainer">
				<input type="hidden" name="taxonToMergeId" class="taxonToMergeId" required />
				<input type="text" class="taxonToMergeIdSelector" name="taxonToMergeIdSelector" /> <span class="taxonToMergeIdSelectorIdDisplay"></span><br />
			</div>
			<a class="addNewItem">+ Add new</a>
			
			<label for="newParent">New taxon</label>
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
						<td><input name="newTaxonScientificName" required /></td>
						<td><input name="newTaxonAuthors" /></td>
						<td>
							<select name="newTaxonRank"> 
								<option value=""></option>
								<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
									<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
								</#list>
							</select>
					 	</td>
					</tr>
				</tbody>
			</table>
			
			<input type="submit" class="button addButton" value="Merge"  />
			
			<p class="info">
				All merged taxa are removed from the checklist. The new taxon is added to the checklist and its concept is linked with the concepts of the merged taxa.
			</p>
			<p class="info">
				Note: Taxonomy tree view will be reloaded after the merge has been completed to show updated data.
			</p>
		</form>
	</div>
	
<#include "luomus-footer.ftl">