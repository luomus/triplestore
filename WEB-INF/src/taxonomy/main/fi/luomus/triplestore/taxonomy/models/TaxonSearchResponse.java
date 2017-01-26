package fi.luomus.triplestore.taxonomy.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fi.luomus.commons.containers.InformalTaxonGroup;
import fi.luomus.commons.containers.rdf.Qname;
import fi.luomus.commons.utils.Utils;
import fi.luomus.commons.xml.Document;
import fi.luomus.commons.xml.Document.Node;

public class TaxonSearchResponse {

	public static class Match {
		
		private final Qname taxonId;
		private final String matchingName;
		private String scientificName;
		private String scientificNameAuthorship;
		private Qname taxonRank;
		private Double similarity;
		private final List<InformalTaxonGroup> informalGroups = new ArrayList<>();
		
		public Match(Qname taxonId, String matchingName) {
			this.taxonId = taxonId;
			this.matchingName = matchingName.toLowerCase().trim();
		}

		public String getScientificName() {
			return scientificName;
		}

		public void setScientificName(String scientificName) {
			this.scientificName = scientificName;
		}

		public String getScientificNameAuthorship() {
			return scientificNameAuthorship;
		}

		public void setScientificNameAuthorship(String scientificNameAuthorship) {
			this.scientificNameAuthorship = scientificNameAuthorship;
		}

		public Qname getTaxonRank() {
			return taxonRank;
		}

		public void setTaxonRank(Qname taxonRank) {
			this.taxonRank = taxonRank;
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

		public Qname getTaxonId() {
			return taxonId;
		}

		public String getMatchingName() {
			return matchingName;
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
		Node matchNode = new Node(match.taxonId.toString());
		matchNode.addAttribute("matchingName", match.getMatchingName());
		if (given(match.getScientificName())) {
			matchNode.addAttribute("scientificName", match.getScientificName());
		}
		if (given(match.getScientificNameAuthorship())) {
			matchNode.addAttribute("scientificNameAuthorship", match.scientificNameAuthorship);
		}
		if (given(match.getTaxonRank())) {
			matchNode.addAttribute("taxonRank", match.getTaxonRank().toString());
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
