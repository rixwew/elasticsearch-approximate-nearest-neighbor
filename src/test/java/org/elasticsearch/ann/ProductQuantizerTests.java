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
package org.elasticsearch.ann;

import org.apache.lucene.util.LuceneTestCase;

public class ProductQuantizerTests extends LuceneTestCase {

    public void testGetDistance01() {
        int d = 4;
        int m = 2;
        int ksub = 2;
        float[] pqCentroids = new float[]{0, 0, 0.25F, 0, 0, 0.25F, 0.25F, 0.25F};
        ProductQuantizer pq = new ProductQuantizer(d, m, ksub, pqCentroids);
        float[] feature = new float[]{0.25F, 0, 0, 0.25F};
        assertEquals(0.25, feature[0], Float.MIN_NORMAL);
        assertEquals(0, feature[1], Float.MIN_NORMAL);
        assertEquals(0, feature[2], Float.MIN_NORMAL);
        assertEquals(0.25, feature[3], Float.MIN_NORMAL);
        float[] table = pq.getCodeTable(feature);
        short[] codes;
        float value;

        codes = new short[]{0, 0};
        value = pq.getDistance(table, codes);
        assertEquals(0.25 * 0.25, value, Float.MIN_NORMAL);

        codes = new short[]{0, 1};
        value = pq.getDistance(table, codes);
        assertEquals(0.25 * 0.25 * 2, value, Float.MIN_NORMAL);

        codes = new short[]{1, 0};
        value = pq.getDistance(table, codes);
        assertEquals(0, value, Float.MIN_NORMAL);

        codes = new short[]{1, 1};
        value = pq.getDistance(table, codes);
        assertEquals(0.25 * 0.25, value, Float.MIN_NORMAL);
    }

}
