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

import org.elasticsearch.ann.ArrayUtils;
import org.elasticsearch.ann.ExactSearch;
import org.elasticsearch.ann.ProductQuantizer;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractIndexAnalyzerProvider;

public class IvfpqAnalyzerProvider extends AbstractIndexAnalyzerProvider<IvfpqAnalyzer> {

    private ProductQuantizer pq;

    private ExactSearch cq;

    public IvfpqAnalyzerProvider(IndexSettings indexSettings, Environment environment, String name,
                                 Settings settings) {
        super(indexSettings, name, settings);
        loadSettings(settings);
    }

    @Override
    public IvfpqAnalyzer get() {
        return new IvfpqAnalyzer(cq, pq);
    }

    private void loadSettings(Settings settings) {
        int m = settings.getAsInt("m", 0);
        int d = settings.getAsInt("d", 0);
        int ksub = settings.getAsInt("ksub", 0);
        float[] coarseCentroids = ArrayUtils.parseFloatArrayCsv(settings.get("coarseCentroids"));
        float[] pqCentroids = ArrayUtils.parseFloatArrayCsv(settings.get("pqCentroids"));
        this.cq = new ExactSearch(d, coarseCentroids);
        this.pq = new ProductQuantizer(d, m, ksub, pqCentroids);
    }

}
