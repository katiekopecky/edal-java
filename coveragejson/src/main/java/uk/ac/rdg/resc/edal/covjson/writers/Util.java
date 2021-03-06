/*******************************************************************************
 * Copyright (c) 2016 The University of Reading
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package uk.ac.rdg.resc.edal.covjson.writers;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.covjson.StreamingEncoder.MapEncoder;
import uk.ac.rdg.resc.edal.covjson.writers.Constants.Keys;
import uk.ac.rdg.resc.edal.domain.GridDomain;
import uk.ac.rdg.resc.edal.domain.MapDomain;
import uk.ac.rdg.resc.edal.domain.SimpleGridDomain;
import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.feature.DiscreteFeature;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.GridFeature;
import uk.ac.rdg.resc.edal.feature.MapFeature;
import uk.ac.rdg.resc.edal.grid.HorizontalGrid;
import uk.ac.rdg.resc.edal.grid.TimeAxis;
import uk.ac.rdg.resc.edal.grid.TimeAxisImpl;
import uk.ac.rdg.resc.edal.grid.VerticalAxis;
import uk.ac.rdg.resc.edal.grid.VerticalAxisImpl;
import uk.ac.rdg.resc.edal.metadata.Parameter;
import uk.ac.rdg.resc.edal.util.Array2D;
import uk.ac.rdg.resc.edal.util.Array4D;

/**
 * 
 * @author Maik Riechert
 *
 */
public class Util {
	public static final String CoverageJSONContext =
			"https://rawgit.com/reading-escience-centre/coveragejson/master/contexts/coveragejson-base.jsonld";
		
	public static <T> void  addJsonLdContext (MapEncoder<T> map) throws IOException {
		// skip for now
		// map.put("@context", CoverageJSONContext);
	}
	
	public static GridFeature convertToGridFeature(MapFeature feature) {
		// A MapFeature is a GridFeature with T and Z fixed.
		// TODO MapFeature should inherit from GridFeature
		
		MapDomain domain = feature.getDomain();
		VerticalAxis z = domain.getZ() != null ? new VerticalAxisImpl(Keys.Z, 
				Arrays.asList(domain.getZ()), domain.getVerticalCrs()) : null;
		HorizontalGrid xy = domain;
		DateTime time = domain.getTime();
		TimeAxis t = time != null ? new TimeAxisImpl(Keys.T, Arrays.asList(time)) : null;
		
		GridDomain gridDomain = new SimpleGridDomain(xy, z, t);
		
		Map<String, Array4D<Number>> valuesMap = new HashMap<>();
		for (String paramId : feature.getVariableIds()) {
			final Array2D<Number> vals = feature.getValues(paramId);
			if (vals != null) {
				valuesMap.put(paramId, new Array4D<Number>(1, 1, domain.getYSize(), domain.getXSize()) {
					@Override
					public Number get(int... coords) {
						return vals.get(coords[2], coords[3]);
					}
					@Override
					public void set(Number value, int... coords) {
						throw new UnsupportedOperationException();
					}
				});
			}
		}
		
		GridFeature gridFeature = new GridFeature(feature.getId(), feature.getName(), 
				feature.getDescription(), gridDomain, feature.getParameterMap(), valuesMap);
		return gridFeature;
	}
	
	public static Collection<Parameter> withoutParameterGroups(Collection<Parameter> params, Feature<?> feature) {
		// TODO this would be a lot easier if a Parameter would have a isGroup() method or similar
		
		if (!(feature instanceof DiscreteFeature)) {
			throw new EdalException("Only discrete-type features are supported");
		}
		DiscreteFeature<?,?> discreteFeature = (DiscreteFeature<?, ?>) feature;
		
		List<Parameter> filteredParams = new LinkedList<>();
		for (Parameter param : feature.getParameterMap().values()) {
			// skip parameters which are parameter groups and have no values
			if (discreteFeature.getValues(param.getVariableId()) != null) {
				filteredParams.add(param);
			}
		}
		return filteredParams;
	}

}
