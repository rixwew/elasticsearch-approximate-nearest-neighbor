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
package org.elasticsearch.analysis;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.elasticsearch.ann.ExactSearch;
import org.elasticsearch.ann.ProductQuantizer;


public class IvfpqAnalyzer extends Analyzer {

    private ExactSearch cq;

    private ProductQuantizer pq;

    public IvfpqAnalyzer(ExactSearch cq, ProductQuantizer pq) {
        this.cq = cq;
        this.pq = pq;
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new IvfpqTokenizer(cq, pq);
        return new TokenStreamComponents(tokenizer);
    }

    public ProductQuantizer getProductQuantizer() {
        return pq;
    }

    public ExactSearch getCoarseQuantizer() {
        return cq;
    }

}
