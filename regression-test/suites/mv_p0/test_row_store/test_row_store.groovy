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

import org.codehaus.groovy.runtime.IOGroovyMethods

suite ("test_row_store") {

    sql """ DROP TABLE IF EXISTS d_table; """

    sql """
            create table d_table ( k1 int null, k2 int not null, k3 bigint null,     k4 varchar(100) null ) duplicate key (k1,k2,k3) distributed BY hash(k1) buckets 3 properties("replication_num" = "1","store_row_column" = "true");
        """

    test {
        sql "create materialized view kavg as select k1,k4,avg(k2) from d_table group by k1,k4;"
        exception "RowStore table can't create materialized view"
    }

    // test ip type with row store
    sql """ DROP TABLE IF EXISTS ipv_row; """
    sql """ CREATE TABLE ipv_row (
             `id` int,
             `ip_v6` ipv6,
             `ip_v4` ipv4
            ) ENGINE=OLAP
            DISTRIBUTED BY HASH(`id`) BUCKETS 4
            PROPERTIES (
                 "store_row_column"="true",
                 "replication_allocation" = "tag.location.default: 1"
            ); """

    // rowstore column can't  make inde
    sql """ insert into ipv_row values(2, '2001:16a0:2:200a::2', '192.1.2.0');"""
    sql """ insert into ipv_row values(1, '::', '0.0.0.0');"""
    qt_sql """select * from ipv_row where id = 1;"""

}
