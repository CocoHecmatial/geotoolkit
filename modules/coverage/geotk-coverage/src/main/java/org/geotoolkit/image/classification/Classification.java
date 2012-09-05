/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.image.classification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.geotoolkit.util.ArgumentChecks;

/**
 * <p>Define and compute two sort of data classifications.<br/>
 * Quantile classification.<br/>
 * Quantile classification is the most basic classification.<br/>
 * Algorithm divide in some equal parts(at best, if its possible) all data.<br/><br/>
 * Jenks classification.<br/>
 * Jenks method is the most effective, but also most costly in computing terms.<br/>
 * For each case, in first time,  the algorithm computes the "intra-class variance"
 * ie the average of the variances of each of the classes.<br/>
 * A second step, consist to calculates the "inter-class variance",
 * ie the variance of each of the generated classes.<br/>
 * The aim is thus to minimize the "intra-class variance" so that each
 * elements group has generated individuals who "look at best"
 * and maximize the "inter-class variance" in order to obtain the most dissimilar classes possible.<br/><br/>
 * Data will aren't sort in ascending order.</p>
 *
 * @author Rémi Marechal (Geomatys).
 */
public class Classification {

    /**
     * data will be classified.
     */
    private final double[] data;

    /**
     * Number of class which fragment data.
     */
    private final int classNumber;

    /**
     * List will be contain classification result.
     */
    private final List<double[]> classList;

    /**
     * Data value number.
     */
    private final int dataLength;

    /**
     * Begin and ending classes index from {@link #data} table.
     */
    private final int[] index;

    /**
     * Stock average and variance of each possible value group.
     */
    private final double[] moyVar;
    private final int cellLength;

    /**
     * <p>Define and compute two sort of data classifications.<br/>
     * Quantile classification.<br/>
     * Jenks classification.<br/><br/>
     *
     * Note : if "classNumber" parameter equal 1, the 2 classification made add
     * in result list only one ascending order class.</p>
     *
     * @param data table will be classified.
     * @param classNumber class number.
     */
    public Classification(double[] data, int classNumber) {
        ArgumentChecks.ensureNonNull("data table", data);
        if (classNumber < 1)
            throw new IllegalArgumentException("impossible to classify datas with"
                + " class number lesser 1");
        if (classNumber > data.length)
            throw new IllegalArgumentException("impossible to classify datas"
                + " with class number larger than overall elements number");
        this.data        = data;
        this.classNumber = classNumber;
        this.classList   = new LinkedList<double[]>();
        this.dataLength  = data.length;
        this.index       = new int[2 * classNumber];
        this.cellLength  = dataLength - classNumber + 1;
        this.moyVar      = new double[2 * dataLength * cellLength];
    }

    /**
     * Class data from quantile method.
     */
    public void computeQuantile() {
        if (classNumber == 1) {
            classList.add(data);
            index[0] = 0;
            index[1] = data.length;
            return;
        }
        int lowLimit = 0, compIndex = 0;
        int highLimit, comp, l, j;
        double[] result;
        for (int i = 1; i<=classNumber; i++) {
            highLimit = (int) Math.round(i*((double)dataLength)/classNumber);
            //fill index
            index[compIndex++] = lowLimit;
            index[compIndex++] = highLimit;
            l = highLimit-lowLimit;
            comp = 0;
            result = new double[l];
            for (j = lowLimit; j<highLimit; j++) {
                result[comp++] = data[j];
            }
            lowLimit +=l;
            classList.add(result);
        }
    }

//    /**
//     * Class data from Jenks method.
//     */
//    public void computeJenks() {
//        if (classNumber == 1) {
//            classList.add(data);
//            index[0] = 0;
//            index[1] = data.length;
//            return;
//        }
//        int[] finalSequenceKept = null;
//        final int[] jSequence = new int[classNumber];
//        final JenkSequence jSeq = new JenkSequence(jSequence, dataLength);
//        int max, len, min;
//        double moy, currentVar, currentVariance, varianceIntraClass, varianceInterClass;
//        double[] average  = new double[classNumber];
//        double[] variance = new double[classNumber];
//        double diff = 0;
//        //for each classes possibilities.
//        while (jSeq.next()) {
//            min = 0;
//            //for each sequence index.
//            for (int i = 0; i<classNumber; i++) {
//                max = jSequence[i];
//                len = max - min;
//                moy = 0;
//                currentVariance = 0;
//                //average computing.
//                for (int j = min; j<max;j++) {
//                    moy+=data[j];
//                }
//                moy/=len;
//                average[i] = moy;
//                //variance computing.
//                for (int j = min; j<max;j++) {
//                    currentVar = data[j] - moy;
//                    currentVar *= currentVar;
//                    currentVariance += currentVar;
//                }
//                variance[i] = currentVariance/len;
//                //next table begin index.
//                min = max;
//            }
//            /**
//             * Average of classes variances.
//             * Named SDBC or "intra classes variances".
//             */
//            varianceIntraClass = getAverage(variance);
//            /**
//             * Variance of classes averages.
//             * Named SDAM or "inter classes variance".
//             */
//            varianceInterClass = getVariance(average);
//            if (finalSequenceKept == null) {
//                finalSequenceKept = jSequence.clone();
//                diff = (varianceInterClass - varianceIntraClass)/varianceInterClass;
//            } else {
//                if (diff < (varianceInterClass - varianceIntraClass)/varianceInterClass) {
//                    finalSequenceKept = jSequence.clone();
//                    diff = (varianceInterClass - varianceIntraClass)/varianceInterClass;
//                }
//            }
//        }
//        min = 0;
//        int compteur, compIndex = 0;
//        double[] result;
//        for (int i = 0; i<classNumber; i++) {
//            max = finalSequenceKept[i];
//            //fill index table
//            index[compIndex++] = min;
//            index[compIndex++] = max;
//            len = max - min;
//            compteur = 0;
//            result = new double[len];
//            for (int j = min; j<max; j++) {
//                result[compteur++] = data[j];
//            }
//            classList.add(result);
//            min = max;
//        }
//    }

    /**
     * Class data from Jenks method.
     */
    public void computeJenks() {
        if (classNumber == 1) {
            classList.add(data);
            index[0] = 0;
            index[1] = data.length;
            return;
        }
        computeMoyVar();
        int[] finalSequenceKept = null;
        final int[] jSequence = new int[classNumber];
        final JenkSequence jSeq = new JenkSequence(jSequence, dataLength);
        int max, len, min;
        double varianceIntraClass, varianceInterClass;
        double[] average  = new double[classNumber];
        double[] variance = new double[classNumber];
        double diff = 0;
        //for each classes possibilities.
        while (jSeq.next()) {
            min = 0;
            //for each sequence index.
            for (int i = 0; i<classNumber; i++) {
                max = jSequence[i];
                int id = 2*(min*cellLength+(max-1-min));
                average[i]  = moyVar[id];
                variance[i] = moyVar[id+1];
                //next table begin index.
                min = max;
            }
            /**
             * Average of classes variances.
             * Named SDBC or "intra classes variances".
             */
            varianceIntraClass = getAverage(variance);
            /**
             * Variance of classes averages.
             * Named SDAM or "inter classes variance".
             */
            varianceInterClass = getVariance(average);
            double diffTemp = (varianceInterClass - varianceIntraClass) / varianceInterClass;
            if (finalSequenceKept == null || (diff < diffTemp)) {
                finalSequenceKept = jSequence.clone();
                diff = diffTemp;
            }
        }
        min = 0;
        int compteur, compIndex = 0;
        double[] result;
        for (int i = 0; i<classNumber; i++) {
            max = finalSequenceKept[i];
            //fill index table
            index[compIndex++] = min;
            index[compIndex++] = max;
            len = max - min;
            compteur = 0;
            result = new double[len];
            for (int j = min; j<max; j++) {
                result[compteur++] = data[j];
            }
            classList.add(result);
            min = max;
        }
    }

    /**
     * Return classification result.
     *
     * @return classification result.
     */
    public List<double[]> getClasses() {
        return classList;
    }

    /**
     * <p>Return classes separation index from {@link #data} table.<br/><br/>
     * for example : caller want class 10 data in 3 distinct class.<br/>
     * first class  second class   third class<br/>
     * &nbsp;&nbsp;[0][4]&nbsp;&nbsp;&nbsp;...&nbsp;&nbsp;&nbsp;[4][7]&nbsp;&nbsp;&nbsp;...&nbsp;&nbsp;&nbsp;[7][10]<br/>
     * With begin index is inclusive and ending index is exclusive.</p>
     *
     * @return classes separation index from {@link #data} table.
     */
    public int[] getIndex() {
        return index;
    }

    /**
     * Return variance from double table elements.
     *
     * @param values table which contain value to compute variance.
     * @return variance of double elements.
     */
    double getVariance(double[] values) {
        assert (values != null) : "variance values table is null";
        final int length = values.length;
        double moy = 0;
        double var;
        double variance = 0;
        for (int i = 0; i<length; i++) {
            moy += values[i];
        }
        moy /= length;
        for (int i = 0; i<length; i++) {
            var = values[i]-moy;
            var *= var;
            variance += var;
        }
        return variance /= length;
    }

    /**
     * Return average from double table elements.
     *
     * @param values table which contain value to compute average.
     * @return average from double table elements.
     */
    private double getAverage(double[] values) {
        assert (values != null) : "average values table is null";
        final int length = values.length;
        double result = 0;
        for (int i = 0; i<length; i++) {
            result += values[i];
        }
        return result /= length;
    }

    /**
     * Stock and compute average and variance of each possible value group.
     */
    public void computeMoyVar() {
        int min = 0;
        int max = cellLength;
        int currentIndex = min - 1;
        double moy, var, variance;
        int id, len;
        while (min != dataLength) {
            moy = 0;
            while (++currentIndex != max) {
                moy += data[currentIndex];
                len  = currentIndex - min;
                id   = 2 * (min * cellLength + len);
                len++;
                moyVar[id] = moy / len;
                variance   = 0;
                for (int i = min; i <= currentIndex; i++) {
                    var       = data[i] - moy / len;
                    var      *= var;
                    variance += var;
                }
                moyVar[id + 1] = variance / len;
            }
            if (max < dataLength) max++;
            min++;
            currentIndex = min - 1;
        }
    }
}
