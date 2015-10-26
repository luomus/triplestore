<#include "luomus-header.ftl">

<div class="helpPage">

<h1>Help</h1>

Here you can find instructions on how to use the Taxon Editor.

<h2>Q & A</h2>

<ul id="questionsAndAnswers">

	<li>
		<p><span class="qa">Q:</span> 
			Who has access to the Taxon Editor?
		</p>
		<p><span class="qa">A:</span> 
			Anyone with FMNH username and password can access the editor. 
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Who can edit my taxa?
		</p>
		<p><span class="qa">A:</span> 
			Each taxon can have one or more person set as the editor of that taxon. If the taxon does not have any editors set, this property is inherited from the next higher taxon that has an editor.
			Only administrators (ICT-staff) can edit those taxons that do not have anyone as editor (for example the very highest taxonomic levels). Administrators can edit any taxon.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What are "checklists"?
		</p>
		<p><span class="qa">A:</span> 
			A checklist is a collection of taxa that may or may not form a tree structure. We have the main "FinBIF Master Checklist", which represents FinBIF's official view on taxonomy. 
			Besides there master checklist, there are many other checklists already in the system. Anyone with access to the system can create their own checklists.
			In the future it will be possible to link the checklists (statements like: "X is part of A" or "X and Y form B"). 
			Each checklist can have one root taxon.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			What are orphan taxa?
		</p>
		<p><span class="qa">A:</span> 
			Orphan taxa are taxons that have been added to a checklist, but do not connect to the root taxon of the checklist. The "Orphan taxa" view allows users to see these taxons, and if you are one of the editors of that taxon, you can connect it to a tree of that same checklist.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			I made a mistake, how to delete the taxon I created?
		</p>
		<p><span class="qa">A:</span> 
			Click the taxon to be deleted and look for lower right corner for the Delete button.
		</p> 
	</li>
	
	<li>
		<p><span class="qa">Q:</span> 
			Can I "reuse" taxons? For example I find a taxon with a miss-typed name: Can I rename the taxon to something else?
		</p>
		<p><span class="qa">A:</span> 
			You shouldn't. Each taxon gets an MX-identifier code. This should be "permanent" and should not mean something entirely different in the future.
			The miss-typed name could be moved as a synonym (if it's a common mistake) or detached entirely.
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
			How do I split a species (or genus etc) to many species?
		</p>
		<p><span class="qa">A:</span> 
			Please don't do that yet!-) 
		</p> 
	</li>
	
	
	<li>
		<p><span class="qa">Q:</span> 
			what?
		</p>
		<p><span class="qa">A:</span> 
			answer
		</p> 
	</li>
</ul>

<h2>General help</h2>

<p>...</p>

</div>

<#include "luomus-footer.ftl">