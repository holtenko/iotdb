/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.cluster.query.groupby;

import org.apache.iotdb.cluster.common.TestUtils;
import org.apache.iotdb.cluster.query.BaseQueryTest;
import org.apache.iotdb.cluster.query.RemoteQueryContext;
import org.apache.iotdb.db.exception.StorageEngineException;
import org.apache.iotdb.db.exception.metadata.IllegalPathException;
import org.apache.iotdb.db.exception.query.QueryProcessException;
import org.apache.iotdb.db.metadata.path.MeasurementPath;
import org.apache.iotdb.db.metadata.path.PartialPath;
import org.apache.iotdb.db.qp.constant.SQLConstant;
import org.apache.iotdb.db.qp.physical.crud.GroupByTimePlan;
import org.apache.iotdb.db.query.context.QueryContext;
import org.apache.iotdb.db.query.control.QueryResourceManager;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.read.expression.IExpression;
import org.apache.iotdb.tsfile.read.expression.impl.BinaryExpression;
import org.apache.iotdb.tsfile.read.expression.impl.SingleSeriesExpression;
import org.apache.iotdb.tsfile.read.filter.TimeFilter;
import org.apache.iotdb.tsfile.read.filter.ValueFilter;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class ClusterGroupByVFilterDataSetTest extends BaseQueryTest {

  @Test
  public void test()
      throws IOException, StorageEngineException, QueryProcessException, IllegalPathException {
    QueryContext queryContext =
        new RemoteQueryContext(QueryResourceManager.getInstance().assignQueryId(true));
    try {
      GroupByTimePlan groupByPlan = new GroupByTimePlan();
      List<PartialPath> pathList = new ArrayList<>();
      List<String> aggregations = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        pathList.add(new MeasurementPath(TestUtils.getTestSeries(i, 0), TSDataType.DOUBLE));
        aggregations.add(SQLConstant.COUNT);
      }
      groupByPlan.setPaths(pathList);
      groupByPlan.setDeduplicatedPathsAndUpdate(pathList);
      groupByPlan.setAggregations(aggregations);
      groupByPlan.setDeduplicatedAggregations(aggregations);

      groupByPlan.setStartTime(0);
      groupByPlan.setEndTime(20);
      groupByPlan.setSlidingStep(5);
      groupByPlan.setInterval(5);

      IExpression expression =
          BinaryExpression.and(
              new SingleSeriesExpression(
                  new MeasurementPath(TestUtils.getTestSeries(0, 0), TSDataType.DOUBLE),
                  ValueFilter.gtEq(5.0)),
              new SingleSeriesExpression(
                  new MeasurementPath(TestUtils.getTestSeries(5, 0), TSDataType.DOUBLE),
                  TimeFilter.ltEq(15)));
      groupByPlan.setExpression(expression);

      ClusterGroupByVFilterDataSet dataSet =
          new ClusterGroupByVFilterDataSet(queryContext, groupByPlan, testMetaMember);
      dataSet.initGroupBy(queryContext, groupByPlan);
      Object[][] answers =
          new Object[][] {
            new Object[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0},
            new Object[] {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0},
            new Object[] {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0},
            new Object[] {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0},
          };
      for (Object[] answer : answers) {
        checkDoubleDataset(dataSet, answer);
      }
      assertFalse(dataSet.hasNext());
    } finally {
      QueryResourceManager.getInstance().endQuery(queryContext.getQueryId());
    }
  }
}
