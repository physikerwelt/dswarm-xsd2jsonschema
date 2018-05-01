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

public class CmArray extends CmElement {

	private final CmElement item;

	public CmArray(final CmElement item) {

		super(item.getName());
		this.item = item;
	}

	@Override
	public String getType() {
		return "array";
	}

	@Override
	public List<CmElement> getProperties() {
		return null;
	}

	@Override
	public CmElement withName(final String newName) {
		return new CmArray(item.withName(newName));
	}

	public CmElement getItem() {
		return item;
	}

	@Override
	protected void renderInternal(final JsonGenerator jgen) throws IOException {

		renderDescription(jgen);

		jgen.writeObjectFieldStart("items");
		getItem().render(jgen);
		jgen.writeEndObject();
	}
}
