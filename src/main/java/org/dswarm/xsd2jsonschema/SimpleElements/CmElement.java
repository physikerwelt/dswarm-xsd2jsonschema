/**
 * Copyright (C) 2013 – 2016 SLUB Dresden & Avantgarde Labs GmbH (<code@dswarm.org>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dswarm.xsd2jsonschema.SimpleElements;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;

public abstract class CmElement {

	private final String name;

	private String description;

	protected CmElement(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	protected abstract String getType();

	public abstract List<CmElement> getProperties();

	public abstract CmElement withName(final String newName);

	void render(final JsonGenerator jgen) throws IOException {

		jgen.writeObjectFieldStart(getName());

		jgen.writeStringField("type", getType());
		renderDescription(jgen);
		renderInternal(jgen);

		jgen.writeEndObject();
	}

	void renderDescription(final JsonGenerator jgen) throws IOException {
		if (getDescription() != null) {
			jgen.writeStringField("description", getDescription());
		}
	}

	void renderInternal(final JsonGenerator jgen) throws IOException {}
}