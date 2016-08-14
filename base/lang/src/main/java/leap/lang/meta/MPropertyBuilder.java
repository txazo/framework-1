/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.lang.meta;

import leap.lang.enums.Bool;

public class MPropertyBuilder extends MNamedWithDescBuilder<MProperty> {

    protected MType    type;
    protected Boolean  required;
    protected String   defaultValue;
    protected String[] enumValues;
    protected boolean  fixedLength;
    protected Integer  length;
    protected Integer  precision;
    protected Integer  scale;
    protected Boolean  userCreatable;
    protected Boolean  userUpdatable;
    protected Boolean  userSortable;
    protected Boolean  userFilterable;

    public MType getType() {
        return type;
    }

    public void setType(MType type) {
        this.type = type;
    }

    public Boolean getRequired() {
        return required;
    }

    public void setRequired(Boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String[] getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(String[] enumValues) {
        this.enumValues = enumValues;
    }

    public boolean isFixedLength() {
        return fixedLength;
    }

    public void setFixedLength(boolean fixedLength) {
        this.fixedLength = fixedLength;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public Boolean getUserCreatable() {
        return userCreatable;
    }

    public void setUserCreatable(Boolean userCreatable) {
        this.userCreatable = userCreatable;
    }

    public Boolean getUserUpdatable() {
        return userUpdatable;
    }

    public void setUserUpdatable(Boolean userUpdatable) {
        this.userUpdatable = userUpdatable;
    }

    public Boolean getUserSortable() {
        return userSortable;
    }

    public void setUserSortable(Boolean userSortable) {
        this.userSortable = userSortable;
    }

    public Boolean getUserFilterable() {
        return userFilterable;
    }

    public void setUserFilterable(Boolean userFilterable) {
        this.userFilterable = userFilterable;
    }

    @Override
    public MProperty build() {
        return new MProperty(name, title, summary, description, type, required,
                             defaultValue, enumValues, fixedLength, length, precision, scale,
                userCreatable, userUpdatable, userSortable, userFilterable);
    }

}
