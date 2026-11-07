/**
 * Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.value.ResAttr;
import brut.androlib.res.data.value.ResScalarValue;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResAttrDecoder {
    private ResPackage mCurrentPackage;

    public String decode(int type, int value, String rawValue, int attrResId)
            throws AndrolibException {
        if (mCurrentPackage == null) {
            // No package available (nothing in the table matched): do not crash the
            // whole file decode, just pass the raw value through.
            return rawValue != null ? rawValue : "";
        }
        ResScalarValue resValue = mCurrentPackage.getValueFactory().factory(
                type, value, rawValue);

        String decoded = null;
        if (attrResId > 0) {
            try {
                ResAttr attr = (ResAttr) getCurrentPackage().getResTable()
                        .getResSpec(attrResId).getDefaultResource().getValue();

                decoded = attr.convertToResXmlFormat(resValue);
            } catch (Exception ex) {
                // ignored
            }
        }

        return decoded != null ? decoded : resValue.encodeAsResXmlAttr();
    }

    public String decodeManifestAttr(int attrResId) throws AndrolibException {

        if (attrResId != 0 && mCurrentPackage != null) {
            try {
                ResResSpec resResSpec = mCurrentPackage.getResTable().getResSpec(attrResId);

                if (resResSpec != null) {
                    return resResSpec.getName();
                }
            } catch (Exception e) {
                // Spec is not in the table (e.g. the framework table could not be
                // loaded); the caller falls back to a synthetic attribute name.
                return null;
            }
        }

        return null;
    }

    public ResPackage getCurrentPackage() throws AndrolibException {
        if (mCurrentPackage == null) {
            throw new AndrolibException("Current package is null");
        }
        return mCurrentPackage;
    }

    public void setCurrentPackage(ResPackage currentPackage) {
        mCurrentPackage = currentPackage;
    }
}
