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
package org.elasticsearch.mapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.analysis.CodeAttribute;
import org.elasticsearch.ann.ArrayUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.mapper.FieldMapper;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.index.mapper.MapperParsingException;
import org.elasticsearch.index.mapper.ParseContext;
import org.elasticsearch.index.mapper.TypeParsers;
import org.elasticsearch.index.query.QueryShardContext;

import java.io.IOException;
import java.util.List;
import java.util.Map;


public class IvfpqFieldMapper extends FieldMapper {

    private static final Logger LOGGER = LogManager.getLogger(IvfpqFieldMapper.class);

    public static final String CONTENT_TYPE = "ivfpq";

    protected IvfpqFieldMapper(String simpleName, MappedFieldType fieldType, MappedFieldType defaultFieldType,
                               Settings indexSettings, MultiFields multiFields, CopyTo copyTo) {
        super(simpleName, fieldType, defaultFieldType, indexSettings, multiFields, copyTo);
    }

    @Override
    protected String contentType() {
        return CONTENT_TYPE;
    }

    @Override
    protected void parseCreateField(ParseContext context, List<IndexableField> fields)
            throws IOException {
        String value;
        if (context.externalValueSet()) {
            value = context.externalValue().toString();
        } else {
            XContentParser parser = context.parser();
            if (parser.currentToken() == XContentParser.Token.VALUE_NULL) {
                value = fieldType().nullValueAsString();
            } else {
                value = parser.textOrNull();
            }
        }
        if (value == null) {
            return;
        }
        NamedAnalyzer indexAnalyzer = fieldType.indexAnalyzer();
        try (TokenStream tokenStream = indexAnalyzer.tokenStream(name(), value)) {
            final CharTermAttribute charTermAttribute =
                    tokenStream.addAttribute(CharTermAttribute.class);
            final CodeAttribute codeAttribute = tokenStream.addAttribute(CodeAttribute.class);
            tokenStream.reset();
            if (tokenStream.incrementToken()) {
                fields.add(new StringField(name(), charTermAttribute.toString(), Field.Store.NO));
                BytesRef bytes = new BytesRef(ArrayUtils.encodeShortArray(codeAttribute.getCodes()));
                fields.add(new StoredField(getCodesField(name()), bytes));
            }
            tokenStream.end();
        }
    }

    @Override
    protected void doXContentBody(XContentBuilder builder, boolean includeDefaults, Params params)
            throws IOException {
        super.doXContentBody(builder, includeDefaults, params);
        doXContentAnalyzers(builder, includeDefaults);
    }

    static class Defaults {
        static final MappedFieldType FIELD_TYPE = new IvfpqFieldType();

        static {
            FIELD_TYPE.freeze();
        }
    }

    public static class TypeParser implements Mapper.TypeParser {
        @Override
        public Mapper.Builder<?, ?> parse(String name, Map<String, Object> node,
                                          ParserContext parserContext) throws MapperParsingException {
            Builder builder = new Builder(name);
            TypeParsers.parseTextField(builder, name, node, parserContext);
            return builder;
        }
    }


    public static class Builder extends FieldMapper.Builder<Builder, IvfpqFieldMapper> {

        Builder(String name) {
            super(name, Defaults.FIELD_TYPE, Defaults.FIELD_TYPE);
            builder = this;
        }

        @Override
        public IvfpqFieldMapper build(BuilderContext context) {
            setupFieldType(context);
            return new IvfpqFieldMapper(name, fieldType, defaultFieldType, context.indexSettings(),
                    multiFieldsBuilder.build(this, context), copyTo);
        }
    }


    public static class IvfpqFieldType extends MappedFieldType {
        IvfpqFieldType() {
        }

        IvfpqFieldType(IvfpqFieldType ref) {
            super(ref);
        }

        @Override
        public String typeName() {
            return CONTENT_TYPE;
        }

        @Override
        public Query termQuery(Object o, QueryShardContext queryShardContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Query existsQuery(QueryShardContext queryShardContext) {
            throw new UnsupportedOperationException();
        }

        @Override
        public IvfpqFieldType clone() {
            return new IvfpqFieldType(this);
        }

    }

    public static String getCodesField(String field) {
        return field + ".pq";
    }
}
