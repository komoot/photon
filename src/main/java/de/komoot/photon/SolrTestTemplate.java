package de.komoot.photon;

/**
 * @author christoph
 * @date 15.08.13
 */
public class SolrTestTemplate extends AbstractSolrTest {
	private final String identifier;

	public SolrTestTemplate(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public String getTestcases() {
		return "/testcases/" + identifier + ".csv";
	}

	@Override
	public String getInputData() {
		return "/testcases/" + identifier + ".xml";
	}
}
