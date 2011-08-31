package uk.ac.rdg.resc.edal.coverage.grid.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.rdg.resc.edal.Extent;
import uk.ac.rdg.resc.edal.coverage.grid.TimeAxis;
import uk.ac.rdg.resc.edal.position.CalendarSystem;
import uk.ac.rdg.resc.edal.position.TimePosition;
import uk.ac.rdg.resc.edal.position.impl.TimePositionImpl;
import uk.ac.rdg.resc.edal.util.Extents;

public final class TimeAxisImpl extends AbstractReferenceableAxis<TimePosition> implements TimeAxis {

    private TimePosition[] axisValues;
    private final CalendarSystem calSys;
    
    private boolean reversed;
    
    public TimeAxisImpl(String name, TimePosition[] axisValues) {
        super(name);
        this.calSys = axisValues[0].getCalendarSystem(); 
        init(axisValues);
    }
    
    public TimeAxisImpl(String name, List<TimePosition> axisValues) {
        super(name);
        this.calSys = axisValues.get(0).getCalendarSystem(); 
        init(axisValues.toArray(new TimePosition[0]));
    }
    
    private void init(TimePosition[] axisValues){
        if (axisValues.length == 0) {
            throw new IllegalArgumentException("Zero-length array");
        }

        if (axisValues.length == 1) {
            this.axisValues = axisValues.clone();
            return;
        }

        reversed = axisValues[1].compareTo(axisValues[0]) < 0;
        if (reversed) {
            // Copy out the array in reverse order
            this.axisValues = new TimePosition[axisValues.length];
            for (int i = 0; i < axisValues.length; i++) {
                this.axisValues[i] = axisValues[axisValues.length - 1 - i];
            }
        } else {
            this.axisValues = axisValues.clone();
        }

        checkAscending();
    }
    
    /**
     * Checks that the axis values ascend or descend monotonically, throwing an
     * IllegalArgumentException if not.
     */
    private void checkAscending() {
        long prevVal = axisValues[0].getValue();
        for (int i = 1; i < axisValues.length; i++) {
            if (axisValues[i].getValue() <= prevVal) {
                throw new IllegalArgumentException("Coordinate values must increase or decrease monotonically");
            }
            prevVal = axisValues[i].getValue();
        }
    }

    @Override
    protected TimePosition extendFirstValue(TimePosition firstVal, TimePosition nextVal) {
        long tVal = (long) (firstVal.getValue() - 0.5 * (nextVal.getValue() - firstVal.getValue()));
        return new TimePositionImpl(tVal);
    }

    @Override
    protected TimePosition extendLastValue(TimePosition lastVal, TimePosition secondLastVal) {
        long tVal = (long) (lastVal.getValue() + 0.5 * (lastVal.getValue() - secondLastVal.getValue()));
        return new TimePositionImpl(tVal);
    }

    @Override
    public CalendarSystem getCalendarSystem() {
        return calSys;
    }

    @Override
    public boolean isAscending() {
        return !reversed;
    }

    @Override
    public int findIndexOf(TimePosition time) {
        int index = Arrays.binarySearch(axisValues, time);
        if(index >= 0){
            return maybeReverseIndex(index);
        } else {
            int insertionPoint = -(index+1);
            if(insertionPoint == axisValues.length || insertionPoint == 0){
                return -1;
            }
            if(Math.abs(axisValues[insertionPoint].getValue() - time.getValue()) <= 
               Math.abs(axisValues[insertionPoint-1].getValue() - time.getValue())){
                return maybeReverseIndex(insertionPoint);
            } else {
                return maybeReverseIndex(insertionPoint-1);
            }
        }
    }

    /** If the array has been reversed, we need to reverse the index */
    private int maybeReverseIndex(int index) {
        if (reversed)
            return axisValues.length - 1 - index;
        else
            return index;
    }
    
    @Override
    public Extent<TimePosition> getCoordinateBounds(int index) {
        int upperIndex = index + 1;
        int lowerIndex = index - 1;
        TimePosition lowerBound;
        if (index == 0) {
            lowerBound = getCoordinateExtent().getLow();
        } else {
            lowerBound = new TimePositionImpl((long) (0.5 * (axisValues[index].getValue() + axisValues[lowerIndex].getValue())));
        }

        TimePosition upperBound;
        if (index == size() - 1) {
            upperBound = getCoordinateExtent().getHigh();
        } else {
            upperBound = new TimePositionImpl((long) (0.5 * (axisValues[upperIndex].getValue() + axisValues[index].getValue())));
        }

        return Extents.newExtent(lowerBound, upperBound);
    }

    @Override
    public TimePosition getCoordinateValue(int index) {
        return axisValues[maybeReverseIndex(index)];
    }

    @Override
    public int size() {
        return axisValues.length;
    }
    
    public static void main(String[] args) {
        List<TimePosition> axisValues = new ArrayList<TimePosition>();
        for(int minute = 0; minute < 60; minute += 5){
            TimePosition t = new TimePositionImpl(2011, 8, 31, 9, minute, 0);
            axisValues.add(t);
        }
        TimeAxis a = new TimeAxisImpl("time", axisValues);
        for(int i=0;i<60;i++){
            TimePosition t1 = new TimePositionImpl(2011, 8, 31, 9, i, 30);
            int ind = a.findIndexOf(t1);
            if(ind >=0)
                System.out.println("Index of t1:"+ind+"    "+t1+" => "+a.getCoordinateValue(ind));
            else
                System.out.println("Index of t1:"+ind+"    "+t1+" => not in range");
        }
        for(int i=0; i<a.size(); i++){
            System.out.println(i+":"+a.getCoordinateValue(i));
        }
    }
}
