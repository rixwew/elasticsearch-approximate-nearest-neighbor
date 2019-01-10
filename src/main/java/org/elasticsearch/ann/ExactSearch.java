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

import java.util.Arrays;

public class ExactSearch {

    private final int d;

    private final int nlist;

    private final float[] centroids;

    public ExactSearch(int d, float[] centroids) {
        this.d = d;
        this.nlist = d == 0 ? 0 : centroids.length / d;
        this.centroids = centroids;
    }

    public float[] getResidual(int nearest, float[] feature) {
        float[] residual = new float[d];
        System.arraycopy(centroids, nearest * d, residual, 0, d);
        for (int i = 0; i < d; ++i) {
            residual[i] = feature[i] - residual[i];
        }
        return residual;
    }

    public int searchNearest(float[] feature) {
        return AlgebraicOps.findNearest(feature, centroids, nlist, d);
    }

    public int[] searchNearest(float[] feature, int k) {
        float[] distances = new float[nlist];
        AlgebraicOps.l2distance(centroids, feature, distances, nlist, d);
        long[] encoded = new long[distances.length];
        for (int i = 0; i < nlist; ++i) {
            encoded[i] = (((long) Float.floatToIntBits(distances[i])) << 32) | (i & 0xffffffffL);
        }
        Arrays.sort(encoded);
        int size = nlist >= k ? k : nlist;
        int[] result = new int[size];
        for (int i = 0; i < size; ++i) {
            result[i] = (int) (encoded[i]);
        }
        return result;
    }
}
