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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hive.metastore;

import java.util.concurrent.TimeUnit;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf;
import org.apache.hadoop.hive.metastore.conf.MetastoreConf.ConfVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Metastore task to remove old scheduled query executions.
 */
public class ScheduledQueryExecutionsMaintTask implements MetastoreTaskThread {
  private static final Logger LOG = LoggerFactory.getLogger(ScheduledQueryExecutionsMaintTask.class);

  private Configuration conf;

  @Override
  public long runFrequency(TimeUnit unit) {
    return MetastoreConf.getTimeVar(conf, MetastoreConf.ConfVars.SCHEDULED_QUERIES_EXECUTION_MAINT_TASK_FREQUENCY,
        unit);
  }

  @Override
  public void setConf(Configuration configuration) {
    conf = configuration;
  }

  @Override
  public Configuration getConf() {
    return conf;
  }

  @Override
  public void run() {
    try {
      if (!MetastoreConf.getBoolVar(conf, ConfVars.SCHEDULED_QUERIES_ENABLED)) {
        return;
      }
      RawStore ms = HiveMetaStore.HMSHandler.getMSForConf(conf);

      int timeoutSecs = (int) MetastoreConf.getTimeVar(conf,
          MetastoreConf.ConfVars.SCHEDULED_QUERIES_EXECUTION_PROGRESS_TIMEOUT, TimeUnit.SECONDS);
      int timedOutCnt = ms.markScheduledExecutionsTimedOut(timeoutSecs);

      if (timedOutCnt > 0L) {
        LOG.info("Number of timed out scheduled query executions:" + timedOutCnt);
      }

      int maxRetainSecs = (int) MetastoreConf.getTimeVar(conf,
          MetastoreConf.ConfVars.SCHEDULED_QUERIES_EXECUTION_MAX_AGE, TimeUnit.SECONDS);
      int deleteCnt = ms.deleteScheduledExecutions(maxRetainSecs);

      if (deleteCnt > 0L){
        LOG.info("Number of deleted entries: " + deleteCnt);
      }
    } catch (Exception e) {
      LOG.error("Exception while trying to delete: " + e.getMessage(), e);
    }
  }
}
