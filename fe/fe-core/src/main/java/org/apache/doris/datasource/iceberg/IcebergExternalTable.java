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

package org.apache.doris.datasource.iceberg;

import org.apache.doris.catalog.Column;
import org.apache.doris.datasource.ExternalTable;
import org.apache.doris.datasource.SchemaCacheValue;
import org.apache.doris.statistics.AnalysisInfo;
import org.apache.doris.statistics.BaseAnalysisTask;
import org.apache.doris.statistics.ExternalAnalysisTask;
import org.apache.doris.thrift.THiveTable;
import org.apache.doris.thrift.TIcebergTable;
import org.apache.doris.thrift.TTableDescriptor;
import org.apache.doris.thrift.TTableType;

import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.Table;
import org.apache.iceberg.view.SQLViewRepresentation;
import org.apache.iceberg.view.View;
import org.apache.iceberg.view.ViewVersion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class IcebergExternalTable extends ExternalTable {

    private static final String ENGINE_PROP_NAME = "engine-name";
    private boolean isView;

    public IcebergExternalTable(long id, String name, String remoteName, IcebergExternalCatalog catalog,
            IcebergExternalDatabase db) {
        super(id, name, remoteName, catalog, db, TableType.ICEBERG_EXTERNAL_TABLE);
    }

    public String getIcebergCatalogType() {
        return ((IcebergExternalCatalog) catalog).getIcebergCatalogType();
    }

    protected synchronized void makeSureInitialized() {
        super.makeSureInitialized();
        if (!objectCreated) {
            objectCreated = true;
            isView = catalog.viewExists(dbName, getRemoteName());
        }
    }

    @Override
    public Optional<SchemaCacheValue> initSchema() {
        if (isView()) {
            View icebergView = IcebergUtils.getIcebergView(getCatalog(), getRemoteDbName(), getRemoteName());
            return Optional.of(new SchemaCacheValue(
                IcebergUtils.getSchema(getCatalog(), icebergView::schema)));
        } else {
            Table icebergTable = IcebergUtils.getIcebergTable(getCatalog(), getRemoteDbName(), getRemoteName());
            return Optional.of(new SchemaCacheValue(
                IcebergUtils.getSchema(getCatalog(), icebergTable::schema)));
        }
    }

    @Override
    public boolean isView() {
        makeSureInitialized();
        return isView;
    }

    public String getViewText() {
        try {
            return catalog.getPreExecutionAuthenticator().execute(() -> {
                View icebergView = IcebergUtils.getIcebergView(getCatalog(), dbName, getRemoteName());
                ViewVersion viewVersion = icebergView.currentVersion();
                if (viewVersion == null) {
                    throw new RuntimeException(String.format("Cannot get view version for view '%s'", icebergView));
                }
                Map<String, String> summary = viewVersion.summary();
                if (summary == null) {
                    throw new RuntimeException(String.format("Cannot get summary for view '%s'", icebergView));
                }
                String engineName = summary.get(ENGINE_PROP_NAME);
                if (StringUtils.isEmpty(engineName)) {
                    throw new RuntimeException(String.format("Cannot get engine-name for view '%s'", icebergView));
                }
                SQLViewRepresentation sqlViewRepresentation = icebergView.sqlFor(engineName.toLowerCase());
                if (sqlViewRepresentation == null) {
                    throw new UnsupportedOperationException("Cannot get view text from iceberg view");
                }
                return sqlViewRepresentation.sql();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getSqlDialect() {
        try {
            return catalog.getPreExecutionAuthenticator().execute(() -> {
                View icebergView = IcebergUtils.getIcebergView(getCatalog(), dbName, getRemoteName());
                ViewVersion viewVersion = icebergView.currentVersion();
                if (viewVersion == null) {
                    throw new RuntimeException(String.format("Cannot get view version for view '%s'", icebergView));
                }
                Map<String, String> summary = viewVersion.summary();
                if (summary == null) {
                    throw new RuntimeException(String.format("Cannot get summary for view '%s'", icebergView));
                }
                String engineName = summary.get(ENGINE_PROP_NAME);
                if (StringUtils.isEmpty(engineName)) {
                    throw new RuntimeException(String.format("Cannot get engine-name for view '%s'", icebergView));
                }
                return engineName.toLowerCase();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public View getIcebergView() {
        return IcebergUtils.getIcebergView(getCatalog(), dbName, getRemoteName());
    }

    /**
     * get location of an iceberg table or view
     * @return
     */
    public String location() {
        if (isView()) {
            View icebergView = getIcebergView();
            return icebergView.location();
        } else {
            Table icebergTable = getIcebergTable();
            return icebergTable.location();
        }
    }

    /**
     * get properties of an iceberg table or view
     * @return
     */
    public Map<String, String> properties() {
        if (isView()) {
            View icebergView = getIcebergView();
            return icebergView.properties();
        } else {
            Table icebergTable = getIcebergTable();
            return icebergTable.properties();
        }
    }

    @Override
    public TTableDescriptor toThrift() {
        List<Column> schema = getFullSchema();
        if (getIcebergCatalogType().equals("hms")) {
            THiveTable tHiveTable = new THiveTable(getDbName(), getName(), new HashMap<>());
            TTableDescriptor tTableDescriptor = new TTableDescriptor(getId(), TTableType.HIVE_TABLE, schema.size(), 0,
                    getName(), getDbName());
            tTableDescriptor.setHiveTable(tHiveTable);
            return tTableDescriptor;
        } else {
            TIcebergTable icebergTable = new TIcebergTable(getDbName(), getName(), new HashMap<>());
            TTableDescriptor tTableDescriptor = new TTableDescriptor(getId(), TTableType.ICEBERG_TABLE,
                    schema.size(), 0, getName(), getDbName());
            tTableDescriptor.setIcebergTable(icebergTable);
            return tTableDescriptor;
        }
    }

    @Override
    public BaseAnalysisTask createAnalysisTask(AnalysisInfo info) {
        makeSureInitialized();
        return new ExternalAnalysisTask(info);
    }

    @Override
    public long fetchRowCount() {
        makeSureInitialized();
        String tableName = getRemoteName();
        if (Objects.isNull(tableName)) {
            tableName = getName();
        }
        long rowCount = IcebergUtils.getIcebergRowCount(getCatalog(), getRemoteDbName(), getRemoteName());
        return rowCount > 0 ? rowCount : UNKNOWN_ROW_COUNT;
    }

    public Table getIcebergTable() {
        return IcebergUtils.getIcebergTable(getCatalog(), getDbName(), getName());
    }
}
