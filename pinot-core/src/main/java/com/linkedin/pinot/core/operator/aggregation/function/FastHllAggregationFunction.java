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

import com.clearspring.analytics.stream.cardinality.HyperLogLog;
import com.google.common.base.Preconditions;
import com.linkedin.pinot.common.Utils;
import com.linkedin.pinot.core.operator.aggregation.AggregationResultHolder;
import com.linkedin.pinot.core.operator.aggregation.groupby.GroupByResultHolder;
import com.linkedin.pinot.core.startree.hll.HllUtil;
import java.util.List;


/**
 * Class to implement the new 'fasthll' aggregation function,
 * fasthll takes advantage of pre-aggregated results for fast distinct count estimation.
 */
public class FastHllAggregationFunction implements AggregationFunction {
  private static final String FUNCTION_NAME = AggregationFunctionFactory.FASTHLL_AGGREGATION_FUNCTION;
  // TODO: change or not?
  private static final ResultDataType RESULT_DATA_TYPE = ResultDataType.HLL_PREAGGREGATED;
  private final int hllLog2m;

  public FastHllAggregationFunction(int hllLog2m) {
    this.hllLog2m = hllLog2m;
  }

  /**
   * Performs 'fasthll' aggregation on the input array.
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
    Preconditions.checkArgument(valueArray[0] instanceof String[]);
    final String[] values = (String[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    HyperLogLog hll = resultHolder.getResult();
    if (hll == null) {
      hll = new HyperLogLog(hllLog2m);
      resultHolder.setValue(hll);
    }

    for (int i = 0; i < length; i++) {
      try {
        HyperLogLog value = HllUtil.convertStringToHll(values[i]);
        hll.addAll(value);
      } catch (Exception e) {
        Utils.rethrowException(e);
      }
    }
  }

  @Override
  public void aggregateMV(int length, AggregationResultHolder resultHolder, Object... valueArrayArray) {
    throw new RuntimeException(
        "Unsupported method aggregateMV(int length, AggregationResultHolder resultHolder, Object... valueArrayArray) for class " + getClass().getName());
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
    Preconditions.checkArgument(valueArray[0] instanceof String[]);
    final String[] values = (String[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    for (int i = 0; i < length; i++) {
      int groupKey = groupKeys[i];
      HyperLogLog hll = resultHolder.getResult(groupKey);
      if (hll == null) {
        hll = new HyperLogLog(hllLog2m);
        resultHolder.setValueForKey(groupKey, hll);
      }
      try {
        HyperLogLog value = HllUtil.convertStringToHll(values[i]);
        hll.addAll(value);
      } catch (Exception e) {
        Utils.rethrowException(e);
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
    Preconditions.checkArgument(valueArray[0] instanceof String[]);
    final String[] values = (String[]) valueArray[0];
    Preconditions.checkState(length <= values.length);

    for (int i = 0; i < length; i++) {
      for (int groupKey : docIdToGroupKeys[i]) {
        HyperLogLog hll = resultHolder.getResult(groupKey);
        if (hll == null) {
          hll = new HyperLogLog(hllLog2m);
          resultHolder.setValueForKey(groupKey, hll);
        }
        try {
          HyperLogLog value = HllUtil.convertStringToHll(values[i]);
          hll.addAll(value);
        } catch (Exception e) {
          Utils.rethrowException(e);
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
