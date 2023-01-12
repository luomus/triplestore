<#include "luomus-header.ftl">

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
					<td>Move taxa: &nbsp;</td> 
					<td><div id="taxonDragModeSelectorTool" class="switchContainer"><input type="checkbox" id="taxonDragMode" name="taxonDragMode" onchange="changeTaxonDragMode();"/></div></td>
				</tr>
			</table>
			<div id="taxonSearch">
				Search:
				<form onsubmit="searchTaxon(this); return false;" class="taxonomySearchForm" taxonpageBaseLinkType="taxonTree">
					<input type="text" placeholder="Type name or MX code"/> <input type="submit" value="Search" />
					<div class="taxonomySearchResultContainer" style="display: none;">&nbsp;</div>
				</form>
			</div>
		</div>
	</div>
		
	<div id="taxonTree">
		<#include "taxonTreeContent.ftl">
	</div>

	<div id="editTaxon" class="ui-widget-content" title="Edit taxon">
			<div id="editTaxonContent">	
			</div>
	</div>

	<div id="addNewTaxonDialog" class="taxonDialog" title="Add new taxon">
		<form id="addNewTaxonDialogForm" onsubmit="addNewChildDialogSubmit(); return false;">
			<input type="hidden" name="newTaxonParent" id="newTaxonParent" />
			<input type="hidden" name="insertNewTaxonBelow" id="insertNewTaxonBelow" />
			
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
			
			<hr />
			Primary vernacular name <br />
			<label for="newTaxonNameFi">FI</label>
			<input type="text" id="newTaxonNameFi" name="newTaxonNameFi" />
			<br />
			
			<label for="newTaxonNameSv">SV</label>
			<input type="text" id="newTaxonNameSv" name="newTaxonNameSv" />
			<br />
			
			<label for="newTaxonNameEn">EN</label>
			<input type="text" id="newTaxonNameEn" name="newTaxonNameEn" />
			<br />
			
			<hr />
			
			<@label "MX.finnish" />
	    	<select class="finnish" name="MX.finnish">
	    		<option value="">&nbsp;</option>
	    		<option value="true" <#if taxon.finnish>selected="selected"</#if>>Yes</option>
	    	</select>
			<@labeledSelect "MX.occurrenceInFinland" "" />
			<@labeledSelect "MX.typeOfOccurrenceInFinland" "" />
			
			<hr />
			
			<input type="submit" class="button addButton" value="Add" />
			<p class="important info">IMPORTANT: Before adding a new taxon into the checklist, make sure it is not present by some other name.</p>
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
				<option value="BASIONYM">Basionym / Original combination</option>
				<option value="OBJECTIVE">Objective synonym</option>
				<option value="SUBJECTIVE">Subjective synonym</option>
				<option value="HOMOTYPIC">Homotypic synonym</option>
				<option value="HETEROTYPIC">Heterotypic synonym</option>
				<option value="ALTERNATIVE">Alternative name</option>
				<option value="SYNONYM" selected="selected">Synonym</option>
				<option value="MISSPELLED">Misspelled name</option>
				<option value="ORTHOGRAPHIC">Orthographic variant</option>
				<option value="UNCERTAIN">Uncertain synonym</option>
				<option value="MISAPPLIED">Misapplied name</option>
			</select>
			<br />
			<br />
			<h4>Create new synonyms</h4>
			<table>
				<thead>
					<tr>
						<th>Scientific name</th>
						<th>Authors</th>
						<th>Rank</th>
						<th>Type of relationship notes</th>
					</tr>
				</thead>
				<tbody>
					<tr>
						<td><input name="scientificName___1" class="addNewSynonymScientificName" /></td>
						<td><input name="authors___1" /></td>
						<td>
							<select name="rank___1" class="taxonRankSelect"> 
								<option value=""></option>
								<#list properties.getProperty("MX.taxonRank").range.values as taxonRank>
									<option value="${taxonRank.qname}">${(taxonRank.label.forLocale("en"))!taxonRank.qname}</option>
								</#list>
							</select>
					 	</td>
					 	<td><input placeholder="In english" name="notes___1" class="addNewSynonymNotes" /></td>
					</tr>
				</tbody>
			</table>
			<a class="addNewItem addNewSynonymRow">+ Add new</a>
			<br />
			<input type="submit" class="button addButton" value="Add synonyms"  />
			<br /><br />
			<ul class="info">
				<li><b>Basionym / Original combination:</b> The first name under which this taxon was described.</li>
				<li><b>Objective/Homotypic:</b> Name based on same type specimen.</li>
				<li><b>Subjective/Heterotypic:</b> Name not based on same type specimen.</li>
				<li><b>Alternative:</b> Alternative valid name (not synonym).</li>
				<li><b>Synonym:</b> Unspecified synonym category.</li>
				<li><b>Misspelled/Orthographic:</b> The name is a (common) misspelling for the taxon.</li>
				<li><b>Uncertain:</b> Uncertain synonym.</li>
				<li><b>Misapplied:</b> This name has been wrongly used to refer this taxon (in Finland).</li>
			</ul>
			<p class="info">
				Kaikkia synonyymeja käytetään lajinimihaussa, mutta suositeltu nimi näytetään (ja tämä synonyymi suluissa). Kaikkia muita paitsi misapplied voi käyttää havaintojen linkitykseen. Kaikkia muita paitsi misapplied käytetään havaintojen linkitykseen. 
			</p>
			<p class="info">
				All synonyms are used in taxon name search, but primary scientific name is shown (and synonym name in brackets). All synonyms except misapplied can be used to report occurrences. All synonyms except misapplied are used in occurrence linking. 
			</p>
		</form>
	</div>
	
<#include "luomus-footer.ftl">