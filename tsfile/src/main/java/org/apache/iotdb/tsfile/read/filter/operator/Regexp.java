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
package org.apache.iotdb.tsfile.read.filter.operator;

import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.linq4j.tree.Expressions;
import org.apache.iotdb.tsfile.file.metadata.statistics.Statistics;
import org.apache.iotdb.tsfile.read.filter.basic.Filter;
import org.apache.iotdb.tsfile.read.filter.codegen.GenerableFilter;
import org.apache.iotdb.tsfile.read.filter.factory.FilterSerializeId;
import org.apache.iotdb.tsfile.read.filter.factory.FilterType;
import org.apache.iotdb.tsfile.utils.ReadWriteIOUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Regexp.
 *
 * @param <T> comparable data type
 */
public class Regexp<T extends Comparable<T>> implements Filter, GenerableFilter {

  protected String value;

  protected FilterType filterType;

  protected Pattern pattern;

  public Regexp() {}

  public Regexp(String value, FilterType filterType) {
    this.value = value;
    this.filterType = filterType;
    try {
      this.pattern = Pattern.compile(this.value);
    } catch (PatternSyntaxException e) {
      throw new PatternSyntaxException("Regular expression error", value, e.getIndex());
    }
  }

  @Override
  public boolean satisfy(Statistics statistics) {
    return true;
  }

  @Override
  public boolean satisfy(long time, Object value) {
    if (filterType != FilterType.VALUE_FILTER) {
      return false;
    }
    return pattern.matcher(value.toString()).find();
  }

  @Override
  public boolean satisfyStartEndTime(long startTime, long endTime) {
    return true;
  }

  @Override
  public boolean containStartEndTime(long startTime, long endTime) {
    return true;
  }

  @Override
  public Filter copy() {
    return new Regexp(value, filterType);
  }

  @Override
  public void serialize(DataOutputStream outputStream) {
    try {
      outputStream.write(getSerializeId().ordinal());
      outputStream.write(filterType.ordinal());
      ReadWriteIOUtils.write(value, outputStream);
    } catch (IOException ex) {
      throw new IllegalArgumentException("Failed to serialize outputStream of type:", ex);
    }
  }

  @Override
  public void deserialize(ByteBuffer buffer) {
    filterType = FilterType.values()[buffer.get()];
    value = ReadWriteIOUtils.readString(buffer);
    if (value != null) {
      try {
        this.pattern = Pattern.compile(value);
      } catch (PatternSyntaxException e) {
        throw new PatternSyntaxException("Regular expression error", value, e.getIndex());
      }
    }
  }

  @Override
  public String toString() {
    return filterType + " is " + value;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Regexp
        && Objects.equals(((Regexp<?>) o).value, value)
        && ((Regexp<?>) o).filterType == filterType;
  }

  @Override
  public FilterSerializeId getSerializeId() {
    return FilterSerializeId.REGEXP;
  }

  @Override
  public boolean canGenerate() {
    return true;
  }

  @Override
  public Expression generate() {
    // Simply access the delegate
    return Expressions.call(Expressions.parameter(Filter.class, "delegate"), "satisfy", Arrays.asList(
        Expressions.parameter(Long.TYPE, "time"),
        Expressions.parameter(Object.class, "v")
    ));
  }
}
