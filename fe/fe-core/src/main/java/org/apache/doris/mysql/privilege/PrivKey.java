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

package org.apache.doris.mysql.privilege;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

public interface PrivKey extends Comparable<PrivKey> {


    /**
     * Help derived classes compare in the order of 'user', 'host', 'catalog', 'db', 'ctl'.
     * Compare strings[i] with strings[i+1] successively, return if the comparison value is not 0 in current loop.
     */
    default int compareAssist(String... strings) {
        Preconditions.checkState(strings.length % 2 == 0);
        for (int i = 0; i < strings.length; i += 2) {
            int res = strings[i].compareTo(strings[i + 1]);
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }


    class GlobalPrivKey implements PrivKey {

        public GlobalPrivKey() {
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof GlobalPrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            return 0;
        }

        public String toString() {
            return "global";
        }
    }

    class WorkloadGroupPrivKey implements PrivKey {
        private String workloadGroupName;

        public WorkloadGroupPrivKey(final String workloadGroupName) {
            this.workloadGroupName = workloadGroupName;
        }

        public String getOrigWorkloadGroupName() {
            return workloadGroupName;
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof WorkloadGroupPrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            WorkloadGroupPrivKey otherKey = (WorkloadGroupPrivKey) other;
            return workloadGroupName.compareTo(otherKey.workloadGroupName);
        }

        public String toString() {
            return String.format("WorkloadGroupPrivKey: %s", workloadGroupName);
        }
    }

    class ResourcePrivKey implements PrivKey {
        private String resourceName;

        public ResourcePrivKey(final String resourceName) {
            this.resourceName = resourceName;
        }

        public String getOrigResource() {
            return resourceName;
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof ResourcePrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            ResourcePrivKey otherKey = (ResourcePrivKey) other;
            return resourceName.compareTo(otherKey.resourceName);
        }

        public String toString() {
            return String.format("ResourcePrivKey: %s", resourceName);
        }
    }

    class CatalogPrivKey implements PrivKey {
        private String ctl;

        CatalogPrivKey(String ctl) {
            this.ctl  = ctl;
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof CatalogPrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            CatalogPrivKey otherKey = (CatalogPrivKey) other;
            return compareAssist(getCtl(), otherKey.getCtl());
        }

        public String getCtl() {
            return ctl;
        }

        public String toString() {
            return  String.format("CatalogPrivKey: %s", ctl);
        }
    }

    class DBPrivKey extends CatalogPrivKey {
        private String db;

        public DBPrivKey(final String ctl, final String db) {
            super(ctl);
            this.db = db;
        }

        public String getDb() {
            return db;
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof DBPrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            DBPrivKey otherKey = (DBPrivKey) other;
            return compareAssist(
                getCtl(), otherKey.getCtl(),
                getDb(), otherKey.getDb());
        }

        public String toString() {
            return  String.format("DBPrivKey : %s.%s", getCtl(), getDb());
        }
    }

    class TablePrivKey extends DBPrivKey {
        private String tbl;

        public TablePrivKey(final String ctl, final String db, final String tbl) {
            super(ctl, db);
            this.tbl = tbl;
        }

        public String getTbl() {
            return this.tbl;
        }

        @Override
        public int compareTo(@NotNull PrivKey other) {
            if (!(other instanceof TablePrivKey)) {
                throw new ClassCastException("cannot cast " + other.getClass().toString() + " to " + this.getClass());
            }

            TablePrivKey otherKey = (TablePrivKey) other;
            return compareAssist(
                getCtl(), otherKey.getCtl(),
                getDb(), otherKey.getDb(),
                getTbl(), otherKey.getTbl());
        }

        public String toString() {
            return  String.format("TablePrivKey : %s.%s.%s", getCtl(), getDb(), getTbl());
        }
    }

}
