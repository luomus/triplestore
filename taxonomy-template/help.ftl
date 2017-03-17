<#include "luomus-header.ftl">

<div class="helpPage">

<p><a href="${staticURL}/IUCN-käyttöohje.pdf" target="_blank">IUCN user instructions (in Finnish)</a></p>


<h1>Help</h1>


Here you can find instructions on how to use the Taxon Editor.

<h2>Q & A</h2>

<ul id="questionsAndAnswers">

	<li>
		<p><span class="qa">Q:</span> 
			Who has access to the Taxon Editor?
		</p>
		<p><span class="qa">A:</span> 
			Only people who have been granted access can use the editor.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Who can edit my taxa?
		</p>
		<p><span class="qa">A:</span> 
			Each taxon can have one or more person set as the editor of that taxon. If the taxon does not have any editors set, this property is inherited from the next higher taxon that has an editor.
			Only administrators (ICT-staff) can edit those taxa that do not have anyone as editor (for example the very highest taxonomic levels). Administrators can edit any taxon.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What are "checklists"?
		</p>
		<p><span class="qa">A:</span> 
			A checklist is a collection of taxa that may or may not form a tree structure. We have the main "FinBIF Master Checklist", which represents FinBIF's official view on taxonomy. 
			Besides there master checklist, there are many other checklists already in the system. Anyone with access to the system can create their own checklists.
			In the future it will be possible to link taxa between checklists.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			I made a mistake, how to delete the taxon I just created?
		</p>
		<p><span class="qa">A:</span> 
			Navigate to the taxon in taxonomy tree, click the taxon and look to the lower right corner of "Edit taxon" section to find the Delete button. You can only delete taxa for
			a short while. After that they can no longer be deleted. 
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What is the difference between "detaching" and "deleting" a taxon?
		</p>
		<p><span class="qa">A:</span> 
			Once a taxon has been created, you can only delete if for a short while (5 hours). Once the taxon has been publiced and MX-identifier code has been given,
			the taxon should never be removed. However, if there is no reason to have that taxon on your checklist, you can detach it. It will become an orphan taxa
			with no parent and does not belong to any checklist. 
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What is detaching a taxon?
		</p>
		<p><span class="qa">A:</span> 
			Once a taxon has been created, you can only delete if for a short while (5 hours). Once the taxon has been publiced and MX-identifier code has been given,
			the taxon should never be removed. However, if there is no reason to have that taxon on your checklist, you can detach it. It will become an orphan taxa
			with no parent and does not belong to any checklist. Detaching can be considered a soft delete. Only taxons that do not have
			critical data can be detached or deleted.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What is "critical data"?
		</p>
		<p><span class="qa">A:</span> 
			Taxons that have critical data are marked with a "key icon" in the taxonomy tree. Taxons that have children, administrative properties, descriptions texts, invasive species
			attributes (etc) are considered critical. Before they can be removed from the checklist (detached, spitted or merged), the properties must be examined, removed and then moved
			to some other taxa if applicable. 
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Can I "reuse" taxa? For example if I find a taxon that should not be on the checklist, can I just rename the taxon to something else?
		</p>
		<p><span class="qa">A:</span> 
			You should not. Each taxon gets a MX-identifier code. This should be permanent and should not mean something entirely different in the future.
			The taxone can be detached entirely.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			My taxon is known by a different name now. How do I change the name?
		</p>
		<p><span class="qa">A:</span> 
			Navigate to the taxon in taxonomy tree. Click the taxon and look for "Basic taxonomic information"-box in the "Edit taxon" section. Select "Change name and create synonym".
			You will give the new name and the old name is made a synonym. The MX-code does not change and administrative properties etc remain in the taxon with the new name. 
			A new taxon is created for the old name (it gets a different MX-code). If the genus changes, you can then drag the taxon to different genus. Note that the names of 
			subspecies etc are not changed automatically.  
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			How do I split a species to many species?
		</p>
		<p><span class="qa">A:</span> 
			Click gear icon of the taxon in taxonomy tree and select "split". The taxon that is being splitted will be removed and you will provide the names of the replacing taxa.
			Only taxons without critical data can be splitted. 
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			How do I merge a many species into one species?
		</p>
		<p><span class="qa">A:</span> 
			Click gear icon of the taxon in taxonomy tree and select "merge". Select the other taxa that will be merged to the taxon. All merged taxe will be removed from the checklist
			and you provide the name of the replacing taxon. Only taxons without critical data can be splitted. 
		</p> 
	</li>
	
</ul>

<h2>General help</h2>

<p>...</p>

</div>

<#include "luomus-footer.ftl">