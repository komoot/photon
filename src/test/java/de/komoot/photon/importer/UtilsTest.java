package de.komoot.photon.importer;

import com.google.common.collect.ImmutableMap;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.*;

import java.util.HashSet;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * date: 18.05.14
 *
 * @author christoph
 */
public class UtilsTest {
	@Test
	public void testWriteContext() throws Exception {
		XContentBuilder builder = XContentFactory.jsonBuilder();

		final HashSet<Map<String, String>> contexts = new HashSet<>();
		contexts.add(ImmutableMap.of("name", "blub1"));
		contexts.add(ImmutableMap.of("name", "blub2"));

		builder.startObject();
		Utils.writeContext(builder, contexts);
		builder.endObject();

		// since set has no order both options are possible
		final boolean isEqual1 = "{\"context\":{\"default\":\"blub1, blub2\"}}".equals(builder.string());
		final boolean isEqual2 = "{\"context\":{\"default\":\"blub2, blub1\"}}".equals(builder.string());
		assertTrue(isEqual1 || isEqual2);
	}
}
