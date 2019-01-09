/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.search;

import org.apache.lucene.search.Query;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.DeprecationHandler;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.QueryShardContext;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class IvfpqQueryBuilder extends AbstractQueryBuilder<IvfpqQueryBuilder> {

    public static final String NAME = "ivfpq_query";

    private static final int DEFAULT_NPROBE = 3;

    private static final ParseField QUERY_FIELD = new ParseField("query");

    private static final ParseField FIELDS_FIELD = new ParseField("fields");

    private static final ParseField NPROBE_FIELD = new ParseField("nprobe");

    private final Object value;

    private Map<String, Float> fieldsBoosts;

    private int nprobe;

    private IvfpqQueryBuilder(Object value, Map<String, Float> fieldsBoosts, int nprobe) {
        if (value == null) {
            throw new IllegalArgumentException("[" + NAME + "] requires query value");
        }
        this.value = value;
        this.fieldsBoosts = fieldsBoosts;
        this.nprobe = nprobe;
    }

    public IvfpqQueryBuilder(StreamInput in) throws IOException {
        super(in);
        value = in.readGenericValue();
        nprobe = in.readVInt();
        int size = in.readVInt();
        fieldsBoosts = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            fieldsBoosts.put(in.readString(), in.readFloat());
        }
    }

    @Override
    protected void doWriteTo(StreamOutput streamOutput) throws IOException {
        streamOutput.writeGenericValue(value);
        streamOutput.writeVInt(nprobe);
        streamOutput.writeVInt(fieldsBoosts.size());
        for (Map.Entry<String, Float> fieldsEntry : fieldsBoosts.entrySet()) {
            streamOutput.writeString(fieldsEntry.getKey());
            streamOutput.writeFloat(fieldsEntry.getValue());
        }
    }

    @Override
    protected void doXContent(XContentBuilder xContentBuilder, Params params) throws IOException {
        xContentBuilder.startObject(NAME);
        xContentBuilder.field(QUERY_FIELD.getPreferredName(), value);
        xContentBuilder.field(NPROBE_FIELD.getPreferredName(), nprobe);
        xContentBuilder.startArray(FIELDS_FIELD.getPreferredName());
        for (Map.Entry<String, Float> fieldEntry : this.fieldsBoosts.entrySet()) {
            xContentBuilder.value(fieldEntry.getKey() + "^" + fieldEntry.getValue());
        }
        xContentBuilder.endArray();
        xContentBuilder.endObject();
    }

    @Override
    protected Query doToQuery(QueryShardContext queryShardContext) {
        IvfpqQuery ivfpqQuery = new IvfpqQuery(queryShardContext);
        return ivfpqQuery.parse(fieldsBoosts, value, nprobe);
    }

    @Override
    protected boolean doEquals(IvfpqQueryBuilder ivfpqQueryBuilder) {
        return ivfpqQueryBuilder.value.equals(value) && ivfpqQueryBuilder.fieldsBoosts
                .equals(fieldsBoosts);
    }

    @Override
    protected int doHashCode() {
        return Objects.hash(value, fieldsBoosts);
    }

    @Override
    public String getWriteableName() {
        return NAME;
    }

    public static IvfpqQueryBuilder fromXContent(XContentParser parser) throws IOException {
        Object value = null;
        int nprobe = DEFAULT_NPROBE;
        Map<String, Float> fieldsBoosts = new TreeMap<>();
        XContentParser.Token token;
        String currentFieldName = null;
        while ((token = parser.nextToken()) != XContentParser.Token.END_OBJECT) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentFieldName = parser.currentName();
            } else if (FIELDS_FIELD
                    .match(currentFieldName, DeprecationHandler.THROW_UNSUPPORTED_OPERATION)) {
                if (token == XContentParser.Token.START_ARRAY) {
                    while (parser.nextToken() != XContentParser.Token.END_ARRAY) {
                        parseFieldAndBoost(parser, fieldsBoosts);
                    }
                } else if (token.isValue()) {
                    parseFieldAndBoost(parser, fieldsBoosts);
                }
            } else if (token.isValue()) {
                if (QUERY_FIELD
                        .match(currentFieldName, DeprecationHandler.THROW_UNSUPPORTED_OPERATION)) {
                    value = parser.objectText();
                } else if (NPROBE_FIELD.match(currentFieldName, DeprecationHandler.THROW_UNSUPPORTED_OPERATION)) {
                    nprobe = parser.intValue();
                }
            }
        }
        return new IvfpqQueryBuilder(value, fieldsBoosts, nprobe);
    }

    private static void parseFieldAndBoost(XContentParser parser, Map<String, Float> fieldsBoosts)
            throws IOException {
        String fField = null;
        float fBoost = AbstractQueryBuilder.DEFAULT_BOOST;
        char[] fieldText = parser.textCharacters();
        int end = parser.textOffset() + parser.textLength();
        for (int i = parser.textOffset(); i < end; i++) {
            if (fieldText[i] == '^') {
                int relativeLocation = i - parser.textOffset();
                fField = new String(fieldText, parser.textOffset(), relativeLocation);
                fBoost = Float.parseFloat(
                        new String(fieldText, i + 1, parser.textLength() - relativeLocation - 1));
                break;
            }
        }
        if (fField == null) {
            fField = parser.text();
        }
        fieldsBoosts.put(fField, fBoost);
    }

}
