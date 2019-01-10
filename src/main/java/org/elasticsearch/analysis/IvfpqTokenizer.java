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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.ann.ArrayUtils;
import org.elasticsearch.ann.ExactSearch;
import org.elasticsearch.ann.ProductQuantizer;

import java.io.IOException;

public class IvfpqTokenizer extends Tokenizer {

    private static final Logger LOGGER = LogManager.getLogger(IvfpqTokenizer.class);

    private final CharTermAttribute charTermAttribute = addAttribute(CharTermAttribute.class);

    private final CodeAttribute codeAttribute = addAttribute(CodeAttribute.class);

    private ProductQuantizer pq;

    private ExactSearch cq;

    public IvfpqTokenizer(ExactSearch cq, ProductQuantizer pq) {
        this.cq = cq;
        this.pq = pq;
    }

    @Override
    public boolean incrementToken() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        for (int c = input.read(); c != -1; c = input.read()) {
            stringBuilder.append((char) c);
        }
        String value = stringBuilder.toString();
        if (value.length() == 0) {
            return false;
        }
        float[] features = ArrayUtils.parseFloatArrayCsv(value);
        int coarseCenter = cq.searchNearest(features);
        if (coarseCenter != -1) {
            String coarseCenterText = String.valueOf(coarseCenter);
            charTermAttribute.copyBuffer(coarseCenterText.toCharArray(), 0, coarseCenterText.length());
            float[] residual = cq.getResidual(coarseCenter, features);
            short[] codes = pq.getCodes(residual);
            codeAttribute.setCodes(codes);
            return true;
        } else {
            return false;
        }
    }
}
