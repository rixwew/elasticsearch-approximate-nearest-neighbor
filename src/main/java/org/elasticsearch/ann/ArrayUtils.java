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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ArrayUtils {

    public static float[] parseFloatArrayCsv(String floatArrayText) {
        if (floatArrayText == null || floatArrayText.length() == 0) {
            return new float[]{};
        }
        String[] texts = floatArrayText.split(",");
        float[] floats = new float[texts.length];
        for (int i = 0; i < floats.length; ++i) {
            floats[i] = Float.valueOf(texts[i].trim());
        }
        return floats;
    }

    public static byte[] encodeShortArray(short[] array) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(
                array.length * 2);
             DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream)) {
            for (short x : array) {
                dataOutputStream.writeShort(x);
            }
            dataOutputStream.flush();
            return byteArrayOutputStream.toByteArray();
        }
    }

    public static short[] decodeShortArray(byte[] array) throws IOException {
        short[] shorts = new short[array.length / 2];
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(array);
             DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream)) {
            for (int i = 0; i < shorts.length; i++) {
                shorts[i] = dataInputStream.readShort();
            }
        }
        return shorts;
    }

}
