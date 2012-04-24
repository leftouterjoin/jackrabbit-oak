/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.jackrabbit.oak.query.index;

import java.util.List;

/**
 * A mechanism to index data. Indexes might be added or removed at runtime,
 * possibly by changing content in the repository. The provider knows about
 * indexes, and informs the query engine about indexes that where added or
 * removed at runtime.
 */
public interface QueryIndexProvider {

    /**
     * Initialize the instance.
     */
    void init();

    /**
     * Get the currently configured indexes.
     *
     * @return the list of indexes
     */
    List<QueryIndex> getQueryIndexes();

    /**
     * Add a listener that is notified about added and removed indexes.
     *
     * @param listener the listener
     */
    void addListener(QueryIndexListener listener);

    /**
     * Remove a listener.
     *
     * @param listener the listener
     */
    void removeListener(QueryIndexListener listener);

    /**
     * A query index listener
     */
    interface QueryIndexListener {

        /**
         * The given index was added.
         *
         * @param index the index
         */
        void added(QueryIndex index);

        /**
         * The given index was removed.
         *
         * @param index the index
         */
        void removed(QueryIndex index);

    }

}
