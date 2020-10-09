package de.komoot.photon.utils;

import de.komoot.photon.Utils;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.index.query.QueryBuilder;

import java.io.IOException;

/**
 * Created by Sachin Dole on 2/28/2015.
 */
@Slf4j
public class QueryToJson {

    public String convert(QueryBuilder anItem) {
        try {
            // BytesReference bytes = anItem.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null))
            //     .bytes();
            // return bytes.utf8ToString();
            // FIXME - Above code did not compile for ES 7 - so added the following fix
            return Strings.toString(anItem.toXContent(JsonXContent.contentBuilder(), new ToXContent.MapParams(null)));
        } catch (Exception e) {
            log.error("Unable to transform querybuilder to a json string due to an exception", e);
            throw new RuntimeException("Unable to transform querybuilder to a json string due to an exception", e);
        }
    }
}
