/*******************************************************************************
 * Copyright (c) 2012 The University of Reading
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

package uk.ac.rdg.resc.edal.coverage.plugins;

import java.util.Arrays;
import java.util.List;

import uk.ac.rdg.resc.edal.Phenomenon;
import uk.ac.rdg.resc.edal.PhenomenonVocabulary;
import uk.ac.rdg.resc.edal.Unit;
import uk.ac.rdg.resc.edal.UnitVocabulary;
import uk.ac.rdg.resc.edal.coverage.impl.AbstractMultimemberDiscreteCoverage;
import uk.ac.rdg.resc.edal.coverage.metadata.RangeMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.ScalarMetadata;
import uk.ac.rdg.resc.edal.coverage.metadata.VectorComponent;
import uk.ac.rdg.resc.edal.coverage.metadata.VectorComponent.VectorComponentType;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.VectorComponentImpl;
import uk.ac.rdg.resc.edal.coverage.metadata.impl.VectorMetadataImpl;

/**
 * A {@link Plugin} which takes two members and replaces them with X, Y, MAG,
 * DIR - the two original components, and their magnitude and direction when
 * treated as a vector. Add this to an instance of
 * {@link AbstractMultimemberDiscreteCoverage} to have some of its members
 * automatically recognised as vectors
 * 
 * @author Guy Griffiths
 * 
 */
public class VectorPlugin extends Plugin {

    private final String description;
    private final String commonStandardName;
    private final String xName;
    private final String yName;
    private final String magName;
    private final String dirName;

    /**
     * Instantiate a new {@link VectorPlugin}
     * 
     * @param xCompId
     *            the textual identifier of the x-component
     * @param yCompId
     *            the textual identifier of the y-component
     * @param commonStandardName
     *            the common part of their standard name:
     * 
     *            e.g. for components with standard names
     *            eastward_sea_water_velocity and northward_sea_water_velocity,
     *            the common part of the standard name is sea_water_velocity
     * 
     * @param description
     *            a description of the new {@link RangeMetadata}
     */
    public VectorPlugin(ScalarMetadata xCompMetadata, ScalarMetadata yCompMetadata, String commonStandardName,
            String description) {
        super(Arrays.asList((RangeMetadata) xCompMetadata, (RangeMetadata) yCompMetadata));
        this.description = description;
        this.commonStandardName = commonStandardName;
        this.xName = getParentName() + "_X";
        this.yName = getParentName() + "_Y";
        this.magName = getParentName() + "_MAG";
        this.dirName = getParentName() + "_DIR";
    }

    @Override
    protected Object generateValue(String component, List<Object> values) {
        if (xName.equals(component)) {
            return values.get(0);
        } else if (yName.equals(component)) {
            return values.get(1);
        } else if (magName.equals(component)) {
            return (float) Math.sqrt(Math.pow((Float) values.get(0), 2)
                    + Math.pow((Float) values.get(1), 2));
        } else if (dirName.equals(component)) {
            return (float) Math.atan2((Float) values.get(1), (Float) values.get(0));
        } else {
            throw new IllegalArgumentException("This Plugin does not provide the field "
                    + component);
        }
    }

    @Override
    protected Class<?> generateValueType(String component, List<Class<?>> classes) {
        if (xName.equals(component)) {
            return classes.get(0);
        } else if (yName.equals(component)) {
            return classes.get(1);
        } else if (magName.equals(component)) {
            return Float.class;
        } else if (dirName.equals(component)) {
            return Float.class;
        } else {
            throw new IllegalArgumentException("This Plugin does not provide the field "
                    + component);
        }
    }
    
    private VectorComponent xMetadata = null;
    private VectorComponent yMetadata = null;
    private VectorComponent magMetadata = null;
    private VectorComponent dirMetadata = null;
    
    @Override
    protected RangeMetadata generateRangeMetadata(List<RangeMetadata> metadataList) {
        /*
         * The casts to ScalarMetadata are fine, because
         */
        VectorMetadataImpl metadata = new VectorMetadataImpl(getParentName(), description);
        if (xMetadata == null) {
            ScalarMetadata sMetadata = (ScalarMetadata) metadataList.get(0);
            xMetadata = new VectorComponentImpl(xName, sMetadata.getDescription(),
                    sMetadata.getParameter(), sMetadata.getUnits(), sMetadata.getValueType(),
                    VectorComponentType.X);
        }
        if (yMetadata == null) {
            ScalarMetadata sMetadata = (ScalarMetadata) metadataList.get(1);
            yMetadata = new VectorComponentImpl(yName, sMetadata.getDescription(),
                    sMetadata.getParameter(), sMetadata.getUnits(), sMetadata.getValueType(),
                    VectorComponentType.Y);
        }
        if (magMetadata == null) {
            ScalarMetadata xComponentMetadata = (ScalarMetadata) metadataList.get(0);
            ScalarMetadata yComponentMetadata = (ScalarMetadata) metadataList.get(1);
            String description;
            String xDesc = xComponentMetadata.getDescription(); 
            String yDesc = yComponentMetadata.getDescription(); 
            if (xDesc.toLowerCase().contains("current") && yDesc.toLowerCase().contains("current")) {
                description = "Current Magnitude";
            } else if (xDesc.toLowerCase().contains("wind") && yDesc.toLowerCase().contains("wind")) {
                description = "Wind Velocity";
            } else {
                description = "Magnitude of (" + xDesc + ", " + yDesc + ")";
            }
            magMetadata = new VectorComponentImpl(magName, description,
                    Phenomenon.getPhenomenon(commonStandardName.replaceFirst("velocity", "speed"),
                            PhenomenonVocabulary.CLIMATE_AND_FORECAST),
                    xComponentMetadata.getUnits(), xComponentMetadata.getValueType(),
                    VectorComponentType.MAGNITUDE);
        }
        if (dirMetadata == null) {
            ScalarMetadata xComponentMetadata = (ScalarMetadata) metadataList.get(0);
            ScalarMetadata yComponentMetadata = (ScalarMetadata) metadataList.get(1);
            String description;
            String xDesc = xComponentMetadata.getDescription(); 
            String yDesc = yComponentMetadata.getDescription(); 
            if (xDesc.toLowerCase().contains("current") && yDesc.toLowerCase().contains("current")) {
                description = "Current Direction";
            } else if (xDesc.toLowerCase().contains("wind") && yDesc.toLowerCase().contains("wind")) {
                description = "Wind Direction";
            } else {
                description = "Direction of (" + xDesc + ", " + yDesc + ")";
            }
            dirMetadata = new VectorComponentImpl(dirName, description,
                    Phenomenon.getPhenomenon(
                            commonStandardName.replaceFirst("velocity", "direction"),
                            PhenomenonVocabulary.UNKNOWN), Unit.getUnit("rad",
                            UnitVocabulary.UDUNITS2), xComponentMetadata.getValueType(),
                    VectorComponentType.DIRECTION);
        }
        metadata.addMember(xMetadata);
        metadata.addMember(yMetadata);
        metadata.addMember(magMetadata);
        metadata.addMember(dirMetadata);
        
        metadata.setChildrenToPlot(Arrays.asList(magMetadata.getName(), dirMetadata.getName()));
        return metadata;
    }

    @Override
    protected ScalarMetadata getScalarMetadata(String memberName) {
        if (xName.equals(memberName)) {
            return xMetadata;
        } else if (yName.equals(memberName)) {
            return yMetadata;
        } else if (magName.equals(memberName)) {
            return magMetadata;
        } else if (dirName.equals(memberName)) {
            return dirMetadata;
        } else {
            throw new IllegalArgumentException(memberName + " is not provided by this plugin");
        }
    }
}
