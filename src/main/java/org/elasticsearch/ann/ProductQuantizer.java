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
        float[] fsub = new float[dsub];
        float[] csub = new float[ksub * dsub];
        for (int i = 0, istart = 0; i < m; ++i, istart += dsub) {
            System.arraycopy(feature, istart, fsub, 0, dsub);
            System.arraycopy(pqCentroids, ksub * istart, csub, 0, ksub * dsub);
            codes[i] = (short) AlgebraicOps.findNearest(fsub, csub, ksub, dsub);
        }
        return codes;
    }

}
