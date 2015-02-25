/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.oak.plugins.index.perf;

import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.ContentRepository;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexEditorProvider;
import org.apache.jackrabbit.oak.plugins.index.property.PropertyIndexProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.segment.SegmentNodeStore;
import org.apache.jackrabbit.oak.spi.security.OpenSecurityProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import forstudy.ContextRunner;
import forstudy.TestHelpers;
import forstudy.TestSpec;
import forstudy.TestSpec.Confirmatin;

@RunWith(ContextRunner.class)
public class FileStorePropertyIndexPerfPocTest extends
		FileStoreNoIndexPerfPocTest {
	@Override
	protected String getIndexTypeName() {
		return "prop";
	}

	@Override
	protected Logger getLogger() {
		return  LoggerFactory.getLogger(FileStorePropertyIndexPerfPocTest.class);
	}

	@Override
	protected ContentRepository createRepository() {
		fs = TestHelpers.createFileStore();
		store = new SegmentNodeStore(fs);
		return new Oak(store).with(new InitialContent())
				.with(new OpenSecurityProvider())
				.with(new PropertyIndexProvider())
				.with(new PropertyIndexEditorProvider())
				.createContentRepository();
	}

	@Override
	protected void createTestIndexNode() throws Exception {
		createPropertyIndex();
	}

	@Test
	@TestSpec(
			objective = { "FileStore,PropertyIndexの性能測定" },
			confirmatins = {
					@Confirmatin(operation = "テストデータを作成する"),
					@Confirmatin(operation = "データノードサイズを計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "インデックスノードサイズを計測する", expected = { "csvにデータ出力されている事" }),
					@Confirmatin(operation = "クエリを実行し処理時間を計測する", expected = { "csvにデータ出力されている事" }),
					})
	public void doPerfTest() throws Exception {
		super.doPerfTest();
	}
}
