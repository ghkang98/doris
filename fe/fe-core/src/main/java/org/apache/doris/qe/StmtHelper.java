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

package org.apache.doris.qe;

import org.apache.doris.analysis.Analyzer;
import org.apache.doris.analysis.CreateTableAsSelectStmt;
import org.apache.doris.analysis.CreateTableLikeStmt;
import org.apache.doris.analysis.CreateTableStmt;
import org.apache.doris.analysis.CreateViewStmt;
import org.apache.doris.analysis.DdlStmt;
import org.apache.doris.analysis.GrantStmt;
import org.apache.doris.analysis.TablePattern;
import org.apache.doris.catalog.AccessPrivilege;
import org.apache.doris.catalog.AccessPrivilegeWithCols;
import org.apache.doris.common.Config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * help to change stmt
 * */
public class StmtHelper {

    public static final Logger LOG = LogManager.getLogger(StmtHelper.class);
    private static List<AccessPrivilegeWithCols> TABLE_OWNER_PRIVILEGES = new ArrayList<>();

    static {
        TABLE_OWNER_PRIVILEGES.add(new AccessPrivilegeWithCols(AccessPrivilege.ALL));
        TABLE_OWNER_PRIVILEGES.add(new AccessPrivilegeWithCols(AccessPrivilege.GRANT_PRIV));
        TABLE_OWNER_PRIVILEGES.add(new AccessPrivilegeWithCols(AccessPrivilege.SHOW_VIEW_PRIV));
    }

    private StmtHelper() {

    }

    /**
     * wrap grant stmt from create, then grant automatically controlled by config
     * */
    public static void wrapAndGrantAuto(ConnectContext ctx, DdlStmt ddlStmt) {
        if (Config.enable_grant_after_creating_table) {
            GrantStmt grantStmt = null;
            if (ddlStmt instanceof CreateTableStmt) {
                grantStmt = wrapGrantStmt(ctx, (CreateTableStmt) ddlStmt);
            } else if (ddlStmt instanceof CreateTableLikeStmt) {
                grantStmt = wrapGrantStmt(ctx, (CreateTableLikeStmt) ddlStmt);
            } else if (ddlStmt instanceof CreateTableAsSelectStmt) {
                grantStmt = wrapGrantStmt(ctx, (CreateTableAsSelectStmt) ddlStmt);
            } else if (ddlStmt instanceof CreateViewStmt) {
                grantStmt = wrapGrantStmt(ctx, (CreateViewStmt) ddlStmt);
            }
            if (null == grantStmt) {
                return;
            }
            try {
                grantStmt.analyzeAttr(new Analyzer(ctx.getEnv(), ctx));
                LOG.info("enable grant after create, stmt: " + grantStmt);
                ctx.getEnv().getAuth().grant(grantStmt);
            } catch (Exception e) {
                LOG.warn("grant after create table fail!!", e);
            }
        }
    }

    private static GrantStmt wrapGrantStmt(ConnectContext ctx, CreateTableStmt createTableStmt) {
        TablePattern tablePattern = new TablePattern(createTableStmt.getCatalogName(),
                createTableStmt.getDbName(), createTableStmt.getTableName());
        return new GrantStmt(ctx.getCurrentUserIdentity(), null, tablePattern, TABLE_OWNER_PRIVILEGES);
    }

    private static GrantStmt wrapGrantStmt(ConnectContext ctx, CreateTableLikeStmt createTableLikeStmt) {
        TablePattern tablePattern = new TablePattern(createTableLikeStmt.getCatalogName(),
                createTableLikeStmt.getDbName(), createTableLikeStmt.getTableName());
        return new GrantStmt(ctx.getCurrentUserIdentity(), null, tablePattern, TABLE_OWNER_PRIVILEGES);
    }

    private static GrantStmt wrapGrantStmt(ConnectContext ctx, CreateTableAsSelectStmt createTableAsSelectStmt) {
        return wrapGrantStmt(ctx, createTableAsSelectStmt.getCreateTableStmt());
    }

    private static GrantStmt wrapGrantStmt(ConnectContext ctx, CreateViewStmt createViewStmt) {
        TablePattern tablePattern = new TablePattern(createViewStmt.getCatalogName(),
                createViewStmt.getDbName(), createViewStmt.getTable());
        return new GrantStmt(ctx.getCurrentUserIdentity(), null, tablePattern, TABLE_OWNER_PRIVILEGES);
    }

}
