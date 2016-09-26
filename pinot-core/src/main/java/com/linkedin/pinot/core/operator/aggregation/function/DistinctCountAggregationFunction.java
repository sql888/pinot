/**
 * Copyright (C) 2014-2016 LinkedIn Corp. (pinot-core@linkedin.com)
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
package com.linkedin.pinot.core.operator.aggregation.function;

import com.google.common.base.Preconditions;
import com.linkedin.pinot.core.operator.aggregation.AggregationResultHolder;
import com.linkedin.pinot.core.operator.aggregation.groupby.GroupByResultHolder;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.List;


/**
 * Class to implement the 'distinctcount' aggregation function.
 */
public class DistinctCountAggregationFunction implements AggregationFunction {
  private static final String FUNCTION_NAME = AggregationFunctionFactory.DISTINCTCOUNT_AGGREGATION_FUNCTION;
  private static final ResultDataType RESULT_DATA_TYPE = ResultDataType.DISTINCTCOUNT_SET;

  /**
   * Performs 'distinctcount' aggregation on the input array.
   *
   * {@inheritDoc}
   *
   * @param length
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregate(int length, AggregationResultHolder resultHolder, Object... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    Preconditions.checkArgument(valueArray[0] instanceof double[]);
    final double[] values = (double[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    IntOpenHashSet valueSet = resultHolder.getResult();
    if (valueSet == null) {
      valueSet = new IntOpenHashSet();
      resultHolder.setValue(valueSet);
    }

    for (int i = 0; i < length; i++) {
      valueSet.add((int) values[i]);
    }
  }

  /**
   * Performs 'distinctcount' aggregation on the input array.
   *
   * {@inheritDoc}
   *
   * @param length
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregateMV(int length, AggregationResultHolder resultHolder, Object... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    Preconditions.checkArgument(valueArray[0] instanceof double[][]);
    final double[][] values = (double[][]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    IntOpenHashSet valueSet = resultHolder.getResult();
    if (valueSet == null) {
      valueSet = new IntOpenHashSet();
      resultHolder.setValue(valueSet);
    }

    for (int i = 0; i < length; i++) {
      for (int j = 0; j < values[i].length; ++j) {
        valueSet.add((int) values[i][j]);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * While the interface allows for variable number of valueArrays, we do not support
   * multiple columns within one aggregation function right now.
   *
   * @param length
   * @param groupKeys
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregateGroupBySV(int length, int[] groupKeys, GroupByResultHolder resultHolder, Object... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    if (valueArray[0] instanceof double[][]) {
      aggregateMVGroupBySV(length, groupKeys, resultHolder, valueArray);
    } else {
      aggregateSVGroupBySV(length, groupKeys, resultHolder, valueArray);
    }
  }

  private void aggregateSVGroupBySV(int length, int[] groupKeys, GroupByResultHolder resultHolder, Object... valueArray) {
    Preconditions.checkArgument(valueArray[0] instanceof double[]);
    final double[] values = (double[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    for (int i = 0; i < length; i++) {
      int groupKey = groupKeys[i];
      IntOpenHashSet valueSet = resultHolder.getResult(groupKey);
      if (valueSet == null) {
        valueSet = new IntOpenHashSet();
        resultHolder.setValueForKey(groupKey, valueSet);
      }
      valueSet.add((int) values[i]);
    }
  }

  /**
   * {@inheritDoc}
   *
   * While the interface allows for variable number of valueArrays, we do not support
   * multiple columns within one aggregation function right now.
   *
   * @param length
   * @param groupKeys
   * @param resultHolder
   * @param valueArray
   */
  private void aggregateMVGroupBySV(int length, int[] groupKeys, GroupByResultHolder resultHolder, Object... valueArray) {
    final double[][] values = (double[][]) valueArray[0];
    Preconditions.checkState(length <= values.length);
    for (int i = 0; i < length; i++) {
      int groupKey = groupKeys[i];
      IntOpenHashSet valueSet = resultHolder.getResult(groupKey);
      if (valueSet == null) {
        valueSet = new IntOpenHashSet();
        resultHolder.setValueForKey(groupKey, valueSet);
      }
      for (double value : values[i]) {
        valueSet.add((int) value);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param length
   * @param docIdToGroupKeys
   * @param resultHolder
   * @param valueArray
   */
  @Override
  public void aggregateGroupByMV(int length, int[][] docIdToGroupKeys, GroupByResultHolder resultHolder,
      Object... valueArray) {
    Preconditions.checkArgument(valueArray.length == 1);
    if (valueArray[0] instanceof double[][]) {
      aggregateMVGroupByMV(length, docIdToGroupKeys, resultHolder, valueArray);
    } else {
      aggregateSVGroupByMV(length, docIdToGroupKeys, resultHolder, valueArray);
    }
  }

  private void aggregateSVGroupByMV(int length, int[][] docIdToGroupKeys, GroupByResultHolder resultHolder, Object... valueArray) {
    Preconditions.checkArgument(valueArray[0] instanceof double[]);
    final double[] values = (double[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    for (int i = 0; i < length; i++) {
      int value = (int) values[i];
      for (int groupKey : docIdToGroupKeys[i]) {
        IntOpenHashSet valueSet = resultHolder.getResult(groupKey);
        if (valueSet == null) {
          valueSet = new IntOpenHashSet();
          resultHolder.setValueForKey(groupKey, valueSet);
        }
        valueSet.add(value);
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @param length
   * @param docIdToGroupKeys
   * @param resultHolder
   * @param valueArrayArray
   */
  private void aggregateMVGroupByMV(int length, int[][] docIdToGroupKeys, GroupByResultHolder resultHolder, Object... valueArray) {
    final double[][] values = (double[][]) valueArray[0];
    Preconditions.checkState(length <= values.length);
    for (int i = 0; i < length; i++) {
      for (int groupKey : docIdToGroupKeys[i]) {
        IntOpenHashSet valueSet = resultHolder.getResult(groupKey);
        if (valueSet == null) {
          valueSet = new IntOpenHashSet();
          resultHolder.setValueForKey(groupKey, valueSet);
        }
        for (double value : values[i]) {
          valueSet.add((int) value);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   *
   * @return
   */
  @Override
  public double getDefaultValue() {
    throw new RuntimeException("Unsupported method getDefaultValue() for class " + getClass().getName());
  }

  /**
   * {@inheritDoc}
   * @return
   */
  @Override
  public ResultDataType getResultDataType() {
    return RESULT_DATA_TYPE;
  }

  @Override
  public String getName() {
    return FUNCTION_NAME;
  }

  /**
   * {@inheritDoc}
   *
   * @param combinedResult
   * @return
   */
  @Override
  public Double reduce(List<Object> combinedResult) {
    throw new RuntimeException(
        "Unsupported method reduce(List<Object> combinedResult) for class " + getClass().getName());
  }
}
