/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.document.blob.ds;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.data.FileDataStore;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreBlobStore;
import org.apache.jackrabbit.oak.plugins.blob.datastore.DataStoreUtils;
import org.apache.jackrabbit.oak.plugins.document.DocumentMK;
import org.apache.jackrabbit.oak.plugins.document.MongoBlobGCTest;
import org.apache.jackrabbit.oak.plugins.document.MongoUtils;
import org.apache.jackrabbit.oak.spi.blob.FileBlobStore;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;

import forstudy.PocMarking;

/**
 * Test for MongoMK GC with {@link DataStoreBlobStore}
 *
 */
@PocMarking("★[BLOB]MongoBlobGCTestのサブクラス。FileDataStoreを持つDataStoreBlobStoreをBlobStoreとする。")
public class MongoDataStoreBlobStoreGC_FileDataStoreTest extends MongoBlobGCTest {
    @Before
    @Override
    public void setUpConnection() throws Exception {
        mongoConnection = MongoUtils.getConnection();
        MongoUtils.dropCollections(mongoConnection.getDB());

        FileDataStore fileDS = new FileDataStore();
        fileDS.setMinRecordLength(4092);
        File storeDir = new File(DataStoreUtils.getHomeDir());
        fileDS.init(storeDir.getAbsolutePath());
        DataStoreBlobStore bs = new DataStoreBlobStore(fileDS, true, 10);
        bs.setMaxCachedBinarySize(10 * 1024 * 1024);

        mk = new DocumentMK.Builder().clock(getTestClock()).setMongoDB(mongoConnection.getDB())
                .setBlobStore(bs).open();
    }

    @After
    @Override
    public void tearDownConnection() throws Exception {
        FileUtils.deleteDirectory(new File(DataStoreUtils.getHomeDir()));
        mk.dispose();
        // the db might already be closed
        mongoConnection.close();
        mongoConnection = MongoUtils.getConnection();
        MongoUtils.dropCollections(mongoConnection.getDB());
        mongoConnection.close();
    }
}
