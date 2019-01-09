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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.function.FunctionScoreQuery;
import org.apache.lucene.search.DisjunctionMaxQuery;
import org.apache.lucene.search.DoubleValues;
import org.apache.lucene.search.DoubleValuesSource;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.analysis.IvfpqAnalyzer;
import org.elasticsearch.ann.ArrayUtils;
import org.elasticsearch.ann.ExactSearch;
import org.elasticsearch.ann.ProductQuantizer;
import org.elasticsearch.index.analysis.NamedAnalyzer;
import org.elasticsearch.index.mapper.MappedFieldType;
import org.elasticsearch.index.query.QueryShardContext;
import org.elasticsearch.mapper.IvfpqFieldMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

class IvfpqQuery {

    private QueryShardContext context;

    IvfpqQuery(QueryShardContext context) {
        this.context = context;
    }

    Query parse(Map<String, Float> fieldNames, Object value, int nprobe) {
        float[] features = ArrayUtils.parseFloatArrayCsv((String) value);
        List<Query> fieldQueries = new ArrayList<>();
        for (String field : fieldNames.keySet()) {
            MappedFieldType fieldMapper = context.fieldMapper(field);
            Analyzer analyzer = context.getSearchAnalyzer(fieldMapper);
            while (analyzer instanceof NamedAnalyzer) {
                analyzer = ((NamedAnalyzer) analyzer).analyzer();
            }
            if (!(analyzer instanceof IvfpqAnalyzer)) {
                throw new ElasticsearchException("illegal analyzer: " + analyzer);
            }
            ProductQuantizer pq = ((IvfpqAnalyzer) analyzer).getProductQuantizer();
            ExactSearch cq = ((IvfpqAnalyzer) analyzer).getCoarseQuantizer();
            for (int nearest : cq.searchNearest(features, nprobe)) {
                float[] residual = cq.getResidual(nearest, features);
                float[] table = pq.getCodeTable(residual);
                Query query = new FunctionScoreQuery(
                        new TermQuery(new Term(field, String.valueOf(nearest))),
                        new CustomValueSource(field, pq, table));
                fieldQueries.add(query);
            }
        }
        return new DisjunctionMaxQuery(fieldQueries, 1.0f);
    }

    private class CustomValueSource extends DoubleValuesSource {

        private String field;

        private ProductQuantizer pq;

        private float[] codeTable;

        CustomValueSource(String field, ProductQuantizer pq, float[] codeTable) {
            this.field = field;
            this.pq = pq;
            this.codeTable = codeTable;
        }

        @Override
        public DoubleValues getValues(LeafReaderContext leafReaderContext, DoubleValues scores) {
            return new DoubleValues() {

                private float value;

                @Override
                public double doubleValue() {
                    return value;
                }

                @Override
                public boolean advanceExact(int doc) throws IOException {
                    Document document = leafReaderContext.reader()
                            .document(doc, new HashSet<>(Collections.singletonList(
                                    IvfpqFieldMapper.getCodesField(field))));
                    BytesRef bytesRef = document.getBinaryValue(
                            IvfpqFieldMapper.getCodesField(field));
                    if (bytesRef == null) {
                        return false;
                    }
                    short[] codes = ArrayUtils.decodeShortArray(bytesRef.bytes);
                    value = pq.getDistance(codeTable, codes);
                    return true;
                }
            };
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        @Override
        public DoubleValuesSource rewrite(IndexSearcher reader) {
            return this;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof IvfpqQuery) {
                return o.hashCode() == this.hashCode();
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(field, codeTable);
        }

        @Override
        public String toString() {
            return null;
        }

        @Override
        public boolean isCacheable(LeafReaderContext ctx) {
            return false;
        }
    }

}
