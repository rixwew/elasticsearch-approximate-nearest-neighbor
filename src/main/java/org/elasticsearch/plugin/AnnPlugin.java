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
package org.elasticsearch.plugin;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.analysis.IvfpqAnalyzerProvider;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.mapper.Mapper;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.mapper.IvfpqFieldMapper;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.MapperPlugin;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.plugins.SearchPlugin;
import org.elasticsearch.search.IvfpqQueryBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnPlugin extends Plugin implements AnalysisPlugin, MapperPlugin, SearchPlugin {

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() {
        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>>
                analyzers = new HashMap<>();
        analyzers.put("ivfpq_analyzer", IvfpqAnalyzerProvider::new);
        return analyzers;
    }

    @Override
    public Map<String, Mapper.TypeParser> getMappers() {
        Map<String, Mapper.TypeParser> map = new HashMap<>();
        map.put(IvfpqFieldMapper.CONTENT_TYPE, new IvfpqFieldMapper.TypeParser());
        return map;
    }

    @Override
    public List<QuerySpec<?>> getQueries() {
        List<QuerySpec<?>> queries = new ArrayList<>();
        queries.add(new QuerySpec<>(IvfpqQueryBuilder.NAME, IvfpqQueryBuilder::new,
                IvfpqQueryBuilder::fromXContent));
        return queries;
    }
}
