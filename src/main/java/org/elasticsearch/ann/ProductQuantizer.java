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

public class ProductQuantizer {

    private final int m;

    private final int dsub;

    private final int ksub;

    private final float[] pqCentroids;

    public ProductQuantizer(int d, int m, int ksub, float[] pqCentroids) {
        this.m = m;
        this.dsub = m == 0 ? 0 : d / m;
        this.ksub = ksub;
        this.pqCentroids = pqCentroids;
    }

    public float getDistance(float[] codeTable, short[] codes) {
        float distance = 0;
        for (int i = 0, offset = 0; i < codes.length; ++i, offset += ksub) {
            distance += codeTable[offset + codes[i]];
        }
        return distance;
    }

    public float[] getCodeTable(float[] feature) {
        final float[] codeTable = new float[m * ksub];
        for (int i = 0, ioffset = 0, foffset = 0, toffset = 0, subLen = ksub * dsub;
             i < m; ++i, ioffset += subLen, foffset += dsub, toffset += ksub) {
            for (int j = 0, joffset = 0; j < ksub; ++j, joffset += dsub) {
                for (int k = 0; k < dsub; ++k) {
                    final float diff = feature[foffset + k] - pqCentroids[ioffset + joffset + k];
                    codeTable[toffset + j] += diff * diff;
                }
            }
        }
        return codeTable;
    }

    public short[] getCodes(float[] feature) {
        short[] codes = new short[m];
        float[] dist = new float[m];
        findNearestNeighbor(feature, pqCentroids, codes, dist, m, ksub, dsub);
        return codes;
    }

    private static void findNearestNeighbor(float[] x, float[] y, short[] nearest, float[] distances,
                                            int nx, int ny, int d) {
        float distance;
        float[] xsq = new float[nx];
        float[] ysq = new float[ny];
        float[] ytr = new float[ny * d];
        float[] ip = new float[nx * ny];
        AlgebraicOps.square(x, xsq, nx, d);
        AlgebraicOps.square(y, ysq, ny, d);
        AlgebraicOps.transpose(y, ytr, ny, d);
        AlgebraicOps.multiply(x, ytr, ip, nx, ny, d);
        for (int i0 = 0, offset = 0; i0 < nx; ++i0, offset += ny) {
            int index = 0;
            float minDistance = distances[i0];
            final float xvalue = xsq[i0];
            for (int j0 = 0; j0 < ny; ++j0) {
                distance = xvalue + ysq[j0] - 2 * ip[offset + j0];
                if (distance < minDistance) {
                    minDistance = distance;
                    index = j0;
                }
            }
            distances[i0] = minDistance;
            nearest[i0] = (short) index;
        }
    }
}
