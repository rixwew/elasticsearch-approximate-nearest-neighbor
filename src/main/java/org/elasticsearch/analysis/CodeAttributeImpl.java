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

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeReflector;


public class CodeAttributeImpl extends AttributeImpl implements CodeAttribute, Cloneable {

    private short[] codes;

    @Override
    public short[] getCodes() {
        return codes;
    }

    @Override
    public void setCodes(short[] codes) {
        this.codes = codes;
    }

    @Override
    public void clear() {
        codes = null;
    }

    @Override
    public void reflectWith(AttributeReflector reflector) {
        reflector.reflect(CodeAttribute.class, "codes", getCodes());
    }

    @Override
    public void copyTo(AttributeImpl target) {
        CodeAttribute codeAttribute = (CodeAttribute) target;
        codeAttribute.setCodes(codes);

    }
}
