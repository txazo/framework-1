/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.web.api.meta.model;

import leap.lang.beans.BeanProperty;
import leap.lang.meta.MProperty;

public class MApiPropertyBuilder extends MApiParameterBaseBuilder<MApiProperty> {

    protected MProperty    metaProperty;
    protected BeanProperty beanProperty;
    protected boolean      discriminator;
    protected Boolean      creatable;
    protected Boolean      updatable;
    protected Boolean      sortable;
    protected Boolean      filterable;
    protected MApiExtension extension;

    public MApiPropertyBuilder() {
	    super();
    }

    public MApiPropertyBuilder(MProperty mp) {
        super();
        this.setMProperty(mp);
    }

	public void setMProperty(MProperty mp) {
        this.metaProperty = mp;
        this.beanProperty = mp.getBeanProperty();
		this.name  = mp.getName();
		this.title = mp.getTitle();
		this.summary = mp.getSummary();
		this.description = mp.getDescription();
        this.metaProperty = mp;
		this.type = mp.getType();
		this.defaultValue = mp.getDefaultValue();
        this.enumValues = mp.getEnumValues();
		this.required =  mp.getRequired();
        this.discriminator = mp.isDiscriminator();
        this.creatable = mp.getCreatable();
        this.updatable = mp.getUpdatable();
        this.sortable = mp.getSortable();
        this.filterable = mp.getFilterable();
	}

    public MProperty getMetaProperty() {
        return metaProperty;
    }

    public void setMetaProperty(MProperty metaProperty) {
        this.metaProperty = metaProperty;
    }

    public BeanProperty getBeanProperty() {
        return beanProperty;
    }

    public void setBeanProperty(BeanProperty beanProperty) {
        this.beanProperty = beanProperty;
    }

    public boolean isDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(boolean discriminator) {
        this.discriminator = discriminator;
    }

    public Boolean getCreatable() {
        return creatable;
    }

    public void setCreatable(Boolean creatable) {
        this.creatable = creatable;
    }

    public Boolean getUpdatable() {
        return updatable;
    }

    public void setUpdatable(Boolean updatable) {
        this.updatable = updatable;
    }

    public Boolean getSortable() {
        return sortable;
    }

    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    public Boolean getFilterable() {
        return filterable;
    }

    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }

    public MApiExtension getExtension() {
        return extension;
    }

    public void setExtension(MApiExtension extension) {
        this.extension = extension;
    }

    @Override
    public MApiProperty build() {
	    return new MApiProperty(name, title, summary, description, metaProperty, beanProperty,
                                type, format, discriminator, password, required,
                                defaultValue, enumValues,
	    					    null == validation ? null : validation.build(), attrs,
                                creatable, updatable, sortable, filterable, extension);
    }
}