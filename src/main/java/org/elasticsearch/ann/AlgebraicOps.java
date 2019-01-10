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


public class AlgebraicOps {

    public static void cumulativeSum(float[] x, float[] z) {
        int total = 0;
        for (int i = 0; i < x.length; ++i) {
            total += x[i];
            z[i] = total;
        }
    }

    public static void square(float[] x, float[] z, int n, int d) {
        for (int i = 0, xoffset = 0; i < n; ++i, xoffset += d) {
            for (int j = 0; j < d; j++) {
                z[i] += x[xoffset + j] * x[xoffset + j];
            }
        }
    }

    public static void transpose(float[] x, float[] z, int nx, int d) {
        for (int i = 0, xoffset = 0; i < nx; ++i, xoffset += d) {
            for (int j = 0, zoffset = 0; j < d; ++j, zoffset += nx) {
                z[zoffset + i] = x[xoffset + j];
            }
        }
    }

    public static void multiply(float[] x, float[] y, float[] z, int nx, int ny, int d) {
        float[] ypart = new float[ny];
        float[] zpart = new float[ny];
        for (int i = 0, xoffset = 0, zoffset = 0; i < nx; ++i, xoffset += d, zoffset += ny) {
            for (int k = 0, yoffset = 0; k < d; ++k, yoffset += ny) {
                final float xval = x[xoffset + k];
                System.arraycopy(y, yoffset, ypart, 0, ny);
                saxpy(ny, xval, ypart, zpart);
            }
            System.arraycopy(zpart, 0, z, zoffset, ny);
            Arrays.fill(zpart, 0f);
        }
    }

    public static void l2distance(float[] x, float[] y, float[] z, int nx, int d) {
        for (int i = 0, xoffset = 0; i < nx; ++i, xoffset += d) {
            for (int k = 0; k < d; ++k) {
                final float diff = x[xoffset + k] - y[k];
                z[i] += diff * diff;
            }
        }
    }

    public static void multiplyElementwise(float[] x, float[] y, float[] z, int nx) {
        for (int i = 0; i < nx; ++i) {
            z[i] = x[i] * y[i];
        }
    }

    public static int findNearest(float[] x, float[] y, int ny, int d) {
        int nearest = -1;
        float minDistance = Float.MAX_VALUE;
        float[] ypart = new float[d];
        for (int i = 0, ioffset = 0; i < ny; ++i, ioffset += d) {
            System.arraycopy(y, ioffset, ypart, 0, d);
            float distance = 0;
            for (int j = 0; j < d; ++j) {
                final float diff = ypart[j] - x[j];
                distance += diff * diff;
            }
            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }
        return nearest;
    }

    private static void saxpy(int ny, float x, float[] y, float[] z) {
        for (int i = 0; i < ny; ++i) {
            z[i] += x * y[i];
        }
    }

}
