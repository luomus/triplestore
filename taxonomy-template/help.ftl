<#include "luomus-header.ftl">

<div class="helpPage">

<h1>Help </h1>
<span style="float: right;" class="info">Updated 21.3.2018</span>

<p>Here you can find instructions on how to use the Taxon Editor.</p>

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">IUCN user instructions (in Finnish)</a> are also available.</p>

<p>Questions to <em>helpdesk@laji.fi</em></p>

<h2>Taxon editor operations</h2>

<table id="helpTable">
	<tr>
		<th>Add new taxon</th>
		<td>Navigate taxon tree so that the parent of the new taxon is expanded (use <img src="${staticURL}/img/help_plusicon.png"/> icon). Click "Add child" from bottom of the child container. Fill in data and press "Add".</td>
	</tr>
	<tr>
		<th>Add new synonym</th>
		<td>Navigate taxon tree so the taxon is visible. Click "Add synonyms". Select type of the synonym and fill in names(s). Click "Add synonyms".</td>
	</tr>
	<tr>
		<th>Rename taxon</th>
		<td>
			<ul>
				<li><b>Case 1 - Fix name:</b> Click the taxon. Click "Fix name/author". Click "Save" (or "Save and close").</li>
				<li><b>Case 2 - Change name:</b> Click the taxon. Click "Change name and create synonym". Click "Save" (or "Save and close"). The checklist taxon will have a new name. Old name is now a synonym name of the checklist taxon.</li>
				<li><b>Case 3 - Rename synonym:</b> Click the synonym. Click "Fix name/author". Click "Save" (or "Save and close").</li>
			</ul>
		</td>
	</tr>
	<tr>
		<th>Change parent</th>
		<td>
			<ul>
				<li><b>Method 1</b> (useful for example when moving many taxa from genus to genus): Expand the taxon tree so that the two genuses(etc) are side by side. Activate "Move taxa" mode. Drag & drop taxa from genus to other genus. Synonyms will be created automatically.</li>
				<li><b>Method 2:</b> Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the taxon to move. Select "Move as...". Select "as child". Type name (or MX-identifier) of the new parent. Click the name from appearing list. Click "Move". No synonyms will be created.</li>
			</ul>
		</td>
	</tr>
	<tr>
		<th>Split taxon</th>
		<td>
			<ol>
				<li>Create new splitted taxa under the taxon to split as children (see Add new taxon).</li>
				<li>Modify the taxon to split to be a group: Change taxon rank to aggregate (etc) and modify the name/author (see Fix name).</li>
				<li>Optionally: Hide the group if you do not want it to be visible when browsing public view of the taxonomy (see Hide taxon).</li>
			</ol>
		</td>
	</tr>
	<tr>
		<th>Merge taxa</th>
		<td>
			<ol>
				<li>Add a new taxon to the tree (the merged taxon) (see Add new taxon).</li>
				<li>Move the taxa to merge under the new taxon as children (see Change parent).</li>
				<li>Optionally: Hide the children  (the merged taxa) if you do not want them to be visible when browsing public view of the taxonomy (see Hide taxon).</li>
			</ol>
		</td>
	</tr>
	<tr>
		<th>Hide / unhide taxon</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the taxon. Click "Hide" or "Unhide".</td>
	</tr>
	<tr>
		<th>Detach / delete taxon</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the taxon. Click "Detach" or "Delete".</td>
	</tr>
	<tr>
		<th>Move taxon as synonym</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the taxon to move. Select "Move as...". Select type of synonym. Type name (or MX-identifier) of the synonyn parent. Click the name from appearing list. Click "Move".</td>
	</tr>
	<tr>
		<th>Move synonym to checklist</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the synonym to move. Select "Move as...". Select "as child". Type name (or MX-identifier) of the parent. Click the name from appearing list. Click "Move".</td>
	</tr>
	<tr>
		<th>Move synonym as synonym of other taxon</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the synonym to move. Select "Move as...". Select synonym type. Type name (or MX-identifier) of the new synonym parent. Click the name from appearing list. Click "Move".</td>
	</tr>
	<tr>
		<th>Change type of synonym</th>
		<td>Click tools gear icon (<span class="ui-icon ui-icon-gear"></span>) of the synonym. Select "Move as...". Select the new synonym type. Type name (or MX-identifier) of the current synonym parent. Click the name from appearing list. Click "Move".</td>
	</tr>
	<tr>
		<th>Change order of taxa</th>
		<td>You can change the taxonomic order of children of a taxon. Navigate the tree so that the children to reorder are visible (use <img src="${staticURL}/img/help_plusicon.png"/> icon). Click "Enable sorting" from top of the child container. Drag & drop taxa to the desired order. (You can also order all alphabetically by clicking "ABC.."). Click "Save order".</td>
	</tr>
	<tr>
		<th>Change order of synonyms</th>
		<td>Not yet implemented</td>
	</tr>
	
</table>

<h2>Critical data</h2>

<p>If "Move as.." or "Delete"/"Detach" options are not visible, the taxon has critical data. You can see the critical data by selecting <span style="border: 1px solid grey; display: inline-block; white-space: nowrap;"> <span class="ui-icon ui-icon-alert"></span> Manage critical data </span> under the taxon tools menu (<span class="ui-icon ui-icon-gear"></span> icon).</p>
<p>If "Move as.." option is visible, but synonym types can not be selected (you can only move as child), the taxon has critical data. Manage critical data option is then also accessible from this view.</p>
<p>When you click <span style="border: 1px solid grey; display: inline-block; white-space: nowrap;"> <span class="ui-icon ui-icon-alert"></span> Manage critical data </span>,
   you will see what critical data the taxon has and what you must do in order to perform the operation you want.</p> 

<h2>Q & A</h2>

<ul id="questionsAndAnswers">

	<li>
		<p><span class="qa">Q:</span> 
			Who has access to the Taxon Editor?
		</p>
		<p><span class="qa">A:</span> 
			Only people who have been granted access by FinBIF staff can get in to the editor.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Who can edit my taxa?
		</p>
		<p><span class="qa">A:</span> 
			Each taxon can have one or more persons set as the editor of that taxon. Only those persons can alter the the data. Everyone with access to the editor can see all data.
			Only administrators (ICT-staff) can edit those taxa that do not have anyone as editor (the very highest taxonomic levels). Administrators can edit any taxon.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What are "checklists"?
		</p>
		<p><span class="qa">A:</span> 
			A checklist is a collection of taxa that may or may not form a tree structure. We have the main "FinBIF Master Checklist", which represents FinBIF's official view of taxonomy. 
			Besides there master checklist, there are many other checklists already in the system. Anyone with access to the system can create their own checklists.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What is "critical data"?
		</p>
		<p><span class="qa">A:</span> 
			When you access the tools (<span class="ui-icon ui-icon-gear"></span> icon) that can be used to manipulate a taxa in a way that causes it not to be a part of the checklist any more 
			(for example detaching it or moving it as a synonym) certain options may be disabled because the taxa has critical data. 
			In that case you have the option to see what critical data the taxon has.
			Taxons that have children, administrative properties, descriptions texts, invasive species attributes (etc) are considered critical. 
			Before a taxon can be removed from the checklist, the properties must be examined, removed or moved to some other taxa before you can proceed.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What is the difference between "detaching" and "deleting" a taxon?
		</p>
		<p><span class="qa">A:</span> 
			Once a taxon has been created, you can only delete if for a short while (5 hours). Once the taxon has been published,
			the taxon should never be removed. However, if there is no reason to have that taxon on your checklist, you can detach it. It will become an orphan taxa
			(no parent and does not belong to any checklist). Deleting a taxon removes is entirely. Taxa that have critical data can not be detached or deleted.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Can I "reuse" taxa? For example if I find a taxon that should not be on the checklist, can I just rename the taxon to something else?
		</p>
		<p><span class="qa">A:</span> 
			You should not. Each taxon gets a MX-identifier code. This should be permanent and should not mean something entirely different in the future.
			The taxon should be for example detached and you should create a new taxon (with a new MX-identifier) for the new name.
		</p> 
	</li>
	
</ul>

</div>

<#include "luomus-footer.ftl">