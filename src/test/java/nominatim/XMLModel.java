package nominatim;

/**
 * @author christoph
 */
public class XMLModel {

	private long id;
	private String name;
	private long osmId;

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setOsmId(long osmId) {
		this.osmId = osmId;
	}

	public long getOsmId() {
		return osmId;
	}
}
