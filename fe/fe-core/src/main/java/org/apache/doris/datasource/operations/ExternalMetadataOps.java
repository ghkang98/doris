// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.datasource.operations;

import org.apache.doris.analysis.CreateDbStmt;
import org.apache.doris.analysis.CreateTableStmt;
import org.apache.doris.analysis.DropDbStmt;
import org.apache.doris.analysis.DropTableStmt;
import org.apache.doris.common.DdlException;
import org.apache.doris.common.UserException;

import org.apache.iceberg.view.View;

import java.util.Collections;
import java.util.List;

/**
 * all external metadata operations use this interface
 */
public interface ExternalMetadataOps {

    /**
     * create db in external metastore
     * @param stmt
     * @throws DdlException
     */
    void createDb(CreateDbStmt stmt) throws DdlException;

    /**
     * drop db in external metastore
     * @param stmt
     * @throws DdlException
     */
    void dropDb(DropDbStmt stmt) throws DdlException;

    /**
     *
     * @param stmt
     * @return if set isExists is true, return true if table exists, otherwise return false
     * @throws UserException
     */
    boolean createTable(CreateTableStmt stmt) throws UserException;

    /**
     *
     * @param stmt
     * @throws DdlException
     */
    void dropTable(DropTableStmt stmt) throws DdlException;

    /**
     *
     * @param dbName
     * @param tblName
     * @param partitions
     */
    void truncateTable(String dbName, String tblName, List<String> partitions) throws DdlException;

    /**
     *
     * @return
     */
    List<String> listDatabaseNames();

    /**
     *
     * @param db
     * @return
     */
    List<String> listTableNames(String db);

    /**
     *
     * @param dbName
     * @param tblName
     * @return
     */
    boolean tableExist(String dbName, String tblName);

    boolean databaseExist(String dbName);

    default Object loadTable(String dbName, String tblName) {
        throw new UnsupportedOperationException("Load table is not supported.");
    }

    /**
     * close the connection, eg, to hms
     */
    void close();

    /**
     * load an iceberg view.
     * @param dbName
     * @param viewName
     * @return
     */
    default View loadView(String dbName, String viewName) {
        throw new UnsupportedOperationException("Load view is not supported.");
    }

    /**
     * Check if an Iceberg view exists.
     * @param dbName
     * @param viewName
     * @return
     */
    default boolean viewExists(String dbName, String viewName) {
        throw new UnsupportedOperationException("View is not supported.");
    }

    /**
     * List all views under a specific database.
     * @param db
     * @return
     */
    default List<String> listViewNames(String db) {
        return Collections.emptyList();
    }
}
