package fi.luomus.triplestore.taxonomy.models;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.taxonomy.Taxon;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaxonSearchResponse {

	public static class Match {
		
		private final Taxon taxon;
		private final String matchingName;
		private Double similarity;
		private final List<InformalTaxonGroup> informalGroups = new ArrayList<>();
		
		public Match(Taxon taxon, String matchingName) {
			this.taxon = taxon;
			this.matchingName = matchingName.toLowerCase().trim();
		}

		public Double getSimilarity() {
			return similarity;
		}

		public void setSimilarity(Double similarity) {
			this.similarity = similarity;
		}

		public List<InformalTaxonGroup> getInformalGroups() {
			return informalGroups;
		}
		
		public String getMatchingName() {
			return matchingName;
		}

		public Taxon getTaxon() {
			return taxon;
		}
		
	}
	
	private final List<Match> exactMatches = new ArrayList<>();
	private final List<Match> likelyMatches = new ArrayList<>();
	private final List<Match> partialMatches = new ArrayList<>();
	private String error;
	
	public List<Match> getExactMatches() {
		return exactMatches;
	}
	
	public List<Match> getLikelyMatches() {
		return likelyMatches;
	}
	
	public List<Match> getPartialMatches() {
		return partialMatches;
	}
	
	public Document getResultsAsDocument() {
		Document results = initResults();
		if (this.hasError()) {
			results.getRootNode().addAttribute("error", this.getError());
			return results;
		}
		
		if (!exactMatches.isEmpty()) {
			Node exactMatch = new Node("exactMatch");
			results.getRootNode().addChildNode(exactMatch);
			for (Match match : exactMatches) {
				exactMatch.addChildNode(toNode(match));
			}
		}
		if (!likelyMatches.isEmpty()) {
			Node likelyMatchesNode = new Node("likelyMatches");
			results.getRootNode().addChildNode(likelyMatchesNode);
			for (Match match : likelyMatches) {
				likelyMatchesNode.addChildNode(toNode(match));
			}
		}
		if (!partialMatches.isEmpty()) {
			Node partialMatchesNode = new Node("partialMatches");
			results.getRootNode().addChildNode(partialMatchesNode);
			for (Match match : partialMatches) {
				partialMatchesNode.addChildNode(toNode(match));
			}
		}
		
		return results;
	}

	private Node toNode(Match match) {
		Taxon t = match.getTaxon();
		Node matchNode = new Node(t.getQname().toString());
		matchNode.addAttribute("matchingName", match.getMatchingName());
		if (given(t.getScientificName())) {
			matchNode.addAttribute("scientificName", t.getScientificName());
		}
		if (given(t.getScientificNameAuthorship())) {
			matchNode.addAttribute("scientificNameAuthorship", t.getScientificNameAuthorship());
		}
		if (given(t.getTaxonRank())) {
			matchNode.addAttribute("taxonRank", t.getTaxonRank().toString());
		}
		if (given(match.getSimilarity())) {
			Double similarity = Utils.round(match.getSimilarity(), 3);
			matchNode.addAttribute("similarity", similarity.toString());
		}
		Node informalGroupsNode = matchNode.addChildNode("informalGroups");
		for (InformalTaxonGroup informalGroup : match.getInformalGroups()) {
			Node informalGroupNode = informalGroupsNode.addChildNode(informalGroup.getQname().toString());
			for (Map.Entry<String, String> e : informalGroup.getName().getAllTexts().entrySet()) {
				informalGroupNode.addAttribute(e.getKey(), e.getValue());
			}
		}
		return matchNode;
	}
	
	private boolean given(Object o) {
		return o != null && o.toString().length() > 0;
	}

	private static Document initResults() {
		Document results = new Document("results");
		return results;
	}

	public void setError(String error) {
		this.error = error;
	}
	
	public String getError() {
		return error;
	}
	
	public boolean hasError() {
		return error != null;
	}
	
}
