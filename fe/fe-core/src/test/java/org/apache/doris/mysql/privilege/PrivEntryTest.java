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

import org.apache.doris.datasource.InternalCatalog;

import org.junit.Assert;
import org.junit.Test;

public class PrivEntryTest {
    @Test
    public void testNameWithUnderscores() throws Exception {
        // TODO: 2023/1/20 zdtodo
        //        TablePrivEntry tablePrivEntry = TablePrivEntry.create(InternalCatalog.INTERNAL_CATALOG_NAME,
        //        "db_db1", "tbl_tbl1", PrivBitSet.of(Privilege.SELECT_PRIV, Privilege.DROP_PRIV));
        //        // pattern match
        //        Assert.assertFalse(tablePrivEntry.getDbPattern().match("db-db1"));
        //        Assert.assertFalse(tablePrivEntry.getTblPattern().match("tbl-tbl1"));
        //        // create TablePrivTable
        //        TablePrivTable tablePrivTable = new TablePrivTable();
        //        tablePrivTable.addEntry(tablePrivEntry, false, false);
        //        UserIdentity userIdentity = new UserIdentity("user1", "127.%", false);
        //        userIdentity.setIsAnalyzed();
        //
        //        PrivBitSet privs1 = PrivBitSet.of();
        //        tablePrivTable.getPrivs(userIdentity, "##internal", "db#db1", "tbl#tbl1", privs1);
        //        Assert.assertFalse(Privilege.satisfy(privs1, PrivPredicate.DROP));
        //
        //        PrivBitSet privs2 = PrivBitSet.of();
        //        tablePrivTable.getPrivs(userIdentity, InternalCatalog.INTERNAL_CATALOG_NAME, "db_db1", "tbl_tbl1", privs2);
        //        Assert.assertTrue(Privilege.satisfy(privs2, PrivPredicate.DROP));
    }

    @Test
    public void testPrivBitSet() {
        PrivBitSet privBitSet = PrivBitSet.of(Privilege.ADMIN_PRIV, Privilege.NODE_PRIV);
        Assert.assertTrue(privBitSet.containsPrivs(Privilege.ADMIN_PRIV));
        Assert.assertTrue(privBitSet.containsPrivs(Privilege.NODE_PRIV));
        privBitSet.set(Privilege.DROP_PRIV.getIdx());
        Assert.assertTrue(privBitSet.containsPrivs(Privilege.DROP_PRIV));
        privBitSet.set(Privilege.DROP_PRIV.getIdx());
        Assert.assertTrue(privBitSet.containsPrivs(Privilege.DROP_PRIV));
        privBitSet.unset(Privilege.NODE_PRIV.getIdx());
        Assert.assertFalse(privBitSet.containsPrivs(Privilege.NODE_PRIV));
        privBitSet.unset(Privilege.NODE_PRIV.getIdx());
        Assert.assertFalse(privBitSet.containsPrivs(Privilege.NODE_PRIV));
    }

    @Test
    public void testPrivEntryCreate() throws Exception {
        //create tablePrivEntry
        TablePrivEntry tablePrivEntry = TablePrivEntry.create(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db1", "tbl_tbl1", PrivBitSet.of(Privilege.SELECT_PRIV, Privilege.DROP_PRIV));
        Assert.assertEquals(InternalCatalog.INTERNAL_CATALOG_NAME, tablePrivEntry.getOrigCtl());
        Assert.assertEquals("db_db1", tablePrivEntry.getOrigDb());
        Assert.assertEquals("tbl_tbl1", tablePrivEntry.getOrigTbl());
        Assert.assertThrows(ClassCastException.class, () -> tablePrivEntry.key.compareTo(new PrivKey.GlobalPrivKey()));
        Assert.assertEquals(tablePrivEntry.key.compareTo(new PrivKey.TablePrivKey(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db1", "tbl_tbl0")), 1);
        Assert.assertEquals(tablePrivEntry.key.compareTo(new PrivKey.TablePrivKey(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db1", "tbl_tbl1")), 0);
        Assert.assertEquals(tablePrivEntry.key.compareTo(
            new PrivKey.TablePrivKey(InternalCatalog.INTERNAL_CATALOG_NAME, "db_db2", "tbl_tbl2")), -1);
        Assert.assertFalse(tablePrivEntry.isAnyCtl());
        Assert.assertFalse(tablePrivEntry.isAnyDb());
        Assert.assertFalse(tablePrivEntry.isAnyTbl());
        Assert.assertTrue(tablePrivEntry.key instanceof PrivKey.TablePrivKey);
        Assert.assertTrue(tablePrivEntry.privSet.containsPrivs(Privilege.SELECT_PRIV));
        Assert.assertTrue(tablePrivEntry.privSet.containsPrivs(Privilege.DROP_PRIV));
        Assert.assertFalse(tablePrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));

        //create dbPrivEntry
        DbPrivEntry dbPrivEntry = DbPrivEntry.create(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db1", PrivBitSet.of(Privilege.SELECT_PRIV, Privilege.DROP_PRIV));
        Assert.assertEquals(InternalCatalog.INTERNAL_CATALOG_NAME, dbPrivEntry.getOrigCtl());
        Assert.assertEquals("db_db1", dbPrivEntry.getOrigDb());
        Assert.assertThrows(ClassCastException.class, () -> dbPrivEntry.key.compareTo(new PrivKey.GlobalPrivKey()));
        Assert.assertEquals(dbPrivEntry.key.compareTo(new PrivKey.DBPrivKey(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db0")), 1);
        Assert.assertEquals(dbPrivEntry.key.compareTo(new PrivKey.DBPrivKey(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db1")), 0);
        Assert.assertEquals(dbPrivEntry.key.compareTo(new PrivKey.DBPrivKey(InternalCatalog.INTERNAL_CATALOG_NAME,
                "db_db2")), -1);
        Assert.assertFalse(dbPrivEntry.isAnyCtl());
        Assert.assertFalse(dbPrivEntry.isAnyDb());
        Assert.assertTrue(dbPrivEntry.key instanceof PrivKey.DBPrivKey);
        Assert.assertTrue(dbPrivEntry.privSet.containsPrivs(Privilege.SELECT_PRIV));
        Assert.assertTrue(dbPrivEntry.privSet.containsPrivs(Privilege.DROP_PRIV));
        Assert.assertFalse(dbPrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));

        //create CatalogPrivEntry
        CatalogPrivEntry catalogPrivEntry = CatalogPrivEntry.create(InternalCatalog.INTERNAL_CATALOG_NAME,
                PrivBitSet.of(Privilege.SELECT_PRIV, Privilege.DROP_PRIV));
        Assert.assertEquals(InternalCatalog.INTERNAL_CATALOG_NAME, catalogPrivEntry.getOrigCtl());
        Assert.assertThrows(ClassCastException.class, () -> catalogPrivEntry.key.compareTo(
                new PrivKey.GlobalPrivKey()));
        Assert.assertEquals(catalogPrivEntry.key.compareTo(new PrivKey.CatalogPrivKey("iceberg")), 11);
        Assert.assertEquals(catalogPrivEntry.key.compareTo(new PrivKey.CatalogPrivKey(
                InternalCatalog.INTERNAL_CATALOG_NAME)), 0);
        Assert.assertFalse(catalogPrivEntry.isAnyCtl());
        Assert.assertTrue(catalogPrivEntry.key instanceof PrivKey.CatalogPrivKey);
        Assert.assertTrue(catalogPrivEntry.privSet.containsPrivs(Privilege.SELECT_PRIV));
        Assert.assertTrue(catalogPrivEntry.privSet.containsPrivs(Privilege.DROP_PRIV));
        Assert.assertFalse(catalogPrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));

        //create WorkloadGroupPrivEntry
        WorkloadGroupPrivEntry workLoadPrivEntry = WorkloadGroupPrivEntry.create("test-g1",
                PrivBitSet.of(Privilege.ADMIN_PRIV, Privilege.GRANT_PRIV));
        Assert.assertEquals("test-g1", workLoadPrivEntry.getOrigWorkloadGroupName());
        Assert.assertTrue(workLoadPrivEntry.key instanceof PrivKey.WorkloadGroupPrivKey);
        Assert.assertThrows(ClassCastException.class, () -> workLoadPrivEntry.key.compareTo(
                new PrivKey.GlobalPrivKey()));
        Assert.assertEquals(workLoadPrivEntry.key.compareTo(new PrivKey.WorkloadGroupPrivKey("test-g0")), 1);
        Assert.assertEquals(workLoadPrivEntry.key.compareTo(new PrivKey.WorkloadGroupPrivKey("test-g1")), 0);
        Assert.assertEquals(workLoadPrivEntry.key.compareTo(new PrivKey.WorkloadGroupPrivKey("test-g2")), -1);
        Assert.assertTrue(workLoadPrivEntry.privSet.containsPrivs(Privilege.ADMIN_PRIV));
        Assert.assertTrue(workLoadPrivEntry.privSet.containsPrivs(Privilege.GRANT_PRIV));
        Assert.assertFalse(workLoadPrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));

        //create GlobalPrivEntry
        GlobalPrivEntry globalPrivEntry = GlobalPrivEntry.create(PrivBitSet.of(Privilege.SELECT_PRIV,
                Privilege.DROP_PRIV));
        Assert.assertTrue(globalPrivEntry.key instanceof PrivKey.GlobalPrivKey);
        Assert.assertEquals(globalPrivEntry.key.compareTo(new PrivKey.GlobalPrivKey()), 0);
        Assert.assertThrows(ClassCastException.class, () -> globalPrivEntry.key.compareTo(
                new PrivKey.ResourcePrivKey("")));
        Assert.assertTrue(globalPrivEntry.privSet.containsPrivs(Privilege.SELECT_PRIV));
        Assert.assertTrue(globalPrivEntry.privSet.containsPrivs(Privilege.DROP_PRIV));
        Assert.assertFalse(globalPrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));

        //create ResourcePrivEntry
        ResourcePrivEntry resourcePrivEntry = ResourcePrivEntry.create("test-r1", PrivBitSet.of(Privilege.ADMIN_PRIV,
                Privilege.GRANT_PRIV));
        Assert.assertTrue(resourcePrivEntry.key instanceof PrivKey.ResourcePrivKey);
        Assert.assertThrows(ClassCastException.class, () -> workLoadPrivEntry.key.compareTo(
                new PrivKey.GlobalPrivKey()));
        Assert.assertEquals(resourcePrivEntry.key.compareTo(new PrivKey.ResourcePrivKey("test-r0")), 1);
        Assert.assertEquals(resourcePrivEntry.key.compareTo(new PrivKey.ResourcePrivKey("test-r1")), 0);
        Assert.assertEquals(resourcePrivEntry.key.compareTo(new PrivKey.ResourcePrivKey("test-r2")), -1);
        Assert.assertTrue(resourcePrivEntry.privSet.containsPrivs(Privilege.ADMIN_PRIV));
        Assert.assertTrue(resourcePrivEntry.privSet.containsPrivs(Privilege.GRANT_PRIV));
        Assert.assertFalse(resourcePrivEntry.privSet.containsPrivs(Privilege.LOAD_PRIV));
    }

}
