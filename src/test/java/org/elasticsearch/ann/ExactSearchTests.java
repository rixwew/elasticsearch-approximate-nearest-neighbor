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

public class ExactSearchTests  extends LuceneTestCase {

    public void testGetResidual01() {
        float[] centroids = new float[]{0, 0.5F, 0.2F, 0.5F, 1, 1.1F};
        ExactSearch exactSearch = new ExactSearch(3, centroids);
        float[] vector = new float[]{0.1F, 0.1F, 0.1F};
        float[] residual0 = exactSearch.getResidual(0, vector);
        assertArrayEquals(new float[]{0.1F, -0.4F, -0.1F}, residual0, Float.MIN_NORMAL);
        float[] residual1 = exactSearch.getResidual(1, vector);
        assertArrayEquals(new float[]{-0.4F, -0.9F, -1}, residual1, Float.MIN_NORMAL);
    }

    public void testSearchNearest01() {
        float[] centroids = new float[]{0, 0, 0, 1, 1, 1};
        ExactSearch exactSearch = new ExactSearch(3, centroids);
        int nearest;
        nearest = exactSearch.searchNearest(new float[]{0.1F, 0.3F, 0.5F});
        assertEquals(0, nearest);
        nearest = exactSearch.searchNearest(new float[]{1.1F, 0.5F, 0.5F});
        assertEquals(1, nearest);
    }

    public void testSearchNearest02() {
        float[] centroids = new float[]{0, 0, 0, 1, 1, 1, 2, 2, 2};
        ExactSearch exactSearch = new ExactSearch(3, centroids);
        int[] nearest = exactSearch.searchNearest(new float[]{1.3F, 1, 0.9F}, 2);
        assertArrayEquals(new int[]{1, 2}, nearest);
    }
}
