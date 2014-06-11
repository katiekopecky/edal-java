/*******************************************************************************
 * Copyright (c) 2013 The University of Reading
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

package uk.ac.rdg.resc.edal.graphics.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.xml.bind.Unmarshaller;

import uk.ac.rdg.resc.edal.exceptions.EdalException;
import uk.ac.rdg.resc.edal.feature.DiscreteFeature;
import uk.ac.rdg.resc.edal.feature.Feature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.graphics.style.util.ColourableIcon;
import uk.ac.rdg.resc.edal.graphics.style.util.FeatureCatalogue;
import uk.ac.rdg.resc.edal.graphics.style.util.FeatureCatalogue.FeaturesAndMemberName;
import uk.ac.rdg.resc.edal.grid.RegularAxis;
import uk.ac.rdg.resc.edal.grid.RegularGrid;
import uk.ac.rdg.resc.edal.position.HorizontalPosition;
import uk.ac.rdg.resc.edal.util.Extents;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.PlottingDomainParams;

public class ColouredGlyphLayer extends ImageLayer {
    protected String dataFieldName;
    protected String glyphIconName = "circle";
    protected ColourScheme colourScheme;

    protected Map<String, ColourableIcon> icons;
    protected ColourableIcon icon;

    public ColouredGlyphLayer(String dataFieldName, String glyphIconName, ColourScheme colourScheme)
            throws InstantiationException {
        this.dataFieldName = dataFieldName;
        this.glyphIconName = glyphIconName;
        this.colourScheme = colourScheme;
        /*
         * Read the icon files before the object is created.
         */
        readInIcons();
        /*
         * Now set the icon to the default
         */
        icon = getIcon(glyphIconName);
    }

    void afterUnmarshal(Unmarshaller u, Object parent) {
        /*
         * Once we have unmarshalled from XML, we may need to change the icon
         * used
         */
        icon = getIcon(glyphIconName);
    }

    public String getGlyphIconName() {
        return glyphIconName;
    }

    protected void readInIcons() throws InstantiationException {
        icons = new HashMap<String, ColourableIcon>();

        URL iconUrl;
        BufferedImage iconImage;

        /*
         * This will work when the files are packaged as a JAR. For running
         * within an IDE, you may need to add the root directory of the project
         * to the classpath
         */
        try {
            iconUrl = this.getClass().getResource("/img/circle.png");
            iconImage = ImageIO.read(iconUrl);
            icons.put("circle", new ColourableIcon(iconImage));

            iconUrl = this.getClass().getResource("/img/square.png");
            iconImage = ImageIO.read(iconUrl);
            icons.put("square", new ColourableIcon(iconImage));
        } catch (IOException e) {
            throw new InstantiationException(
                    "Cannot read required icons.  Ensure that JAR is packaged correctly, or that your project is set up correctly in your IDE");
        }

    }

    protected ColourableIcon getIcon(String name) {
        ColourableIcon ret = null;
        if (name == null) {
            ret = icons.get("circle");
        } else {
            ret = icons.get(name.toLowerCase());
        }
        if (ret != null) {
            return ret;
        } else {
            return icons.get("circle");
        }
    }

    @Override
    protected void drawIntoImage(BufferedImage image, PlottingDomainParams params,
            FeatureCatalogue catalogue) throws EdalException {
        /*
         * Get all of the features which need to be drawn in this image
         */
        FeaturesAndMemberName featuresForLayer = catalogue.getFeaturesForLayer(dataFieldName,
                params);
        Collection<? extends DiscreteFeature<?, ?>> features = featuresForLayer.getFeatures();

        /*
         * Get a RegularGrid from the parameters
         */
        RegularGrid imageGrid = params.getImageGrid();
        /*
         * Get the RegularAxis objects so that we can find the unconstrained
         * index of the position (i.e. the index even if it is beyond the axis
         * bounds)
         */
        RegularAxis xAxis = imageGrid.getXAxis();
        RegularAxis yAxis = imageGrid.getYAxis();

        /*
         * The graphics object for drawing
         */
        Graphics2D g = image.createGraphics();

        for (DiscreteFeature<?, ?> feature : features) {
            if (feature instanceof ProfileFeature) {
                ProfileFeature profileFeature = (ProfileFeature) feature;

                /*
                 * Find the co-ordinates to draw the icon at
                 */
                HorizontalPosition position = profileFeature.getHorizontalPosition();
                if (!GISUtils.crsMatch(position.getCoordinateReferenceSystem(), params.getBbox()
                        .getCoordinateReferenceSystem())) {
                    position = GISUtils.transformPosition(position, params.getBbox()
                            .getCoordinateReferenceSystem());
                }

                int i = xAxis.findIndexOfUnconstrained(position.getX());
                int j = params.getHeight() - 1 - yAxis.findIndexOfUnconstrained(position.getY());

                /*
                 * Get the z-index of the target depth within the vertical
                 * domain
                 */
                int zIndex;
                if (params.getTargetZ() == null) {
                    /*
                     * If no target z is provided, pick the value closest to the
                     * surface
                     */
                    zIndex = profileFeature.getDomain().findIndexOf(
                            GISUtils.getClosestElevationToSurface(profileFeature.getDomain()));
                } else {
                    zIndex = GISUtils.getIndexOfClosestElevationTo(params.getTargetZ(),
                            profileFeature.getDomain());
                }

                if (zIndex < 0) {
                    continue;
                }

                Number value = profileFeature.getValues(featuresForLayer.getMember()).get(zIndex);
                if (value != null && !Float.isNaN(value.floatValue())) {
                    /*
                     * Draw the icon
                     */
                    Color color = colourScheme.getColor(value);
                    g.drawImage(icon.getColouredIcon(color), i - icon.getWidth() / 2,
                            j - icon.getHeight() / 2, null);
                }
            } else {
                /*
                 * Write other features (just PointSeriesFeature ?) here
                 */
            }
        }
    }

    @Override
    public Collection<Class<? extends Feature<?>>> supportedFeatureTypes() {
        List<Class<? extends Feature<?>>> clazzes = new ArrayList<Class<? extends Feature<?>>>();
        clazzes.add(ProfileFeature.class);
        return clazzes;
    }

    @Override
    public Set<NameAndRange> getFieldsWithScales() {
        Set<NameAndRange> ret = new HashSet<Drawable.NameAndRange>();
        ret.add(new NameAndRange(dataFieldName, Extents.newExtent(colourScheme.getScaleMin(),
                colourScheme.getScaleMax())));
        return ret;
    }
}
