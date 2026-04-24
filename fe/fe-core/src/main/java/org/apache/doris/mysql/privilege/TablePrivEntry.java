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

import org.apache.doris.catalog.Env;
import org.apache.doris.common.AnalysisException;
import org.apache.doris.common.CaseSensibility;
import org.apache.doris.common.PatternMatcher;
import org.apache.doris.common.PatternMatcherException;
import org.apache.doris.common.io.Text;

import java.io.DataInput;
import java.io.IOException;

public class TablePrivEntry extends DbPrivEntry {
    private static final String ANY_TBL = "*";

    private PatternMatcher tblPattern;
    private boolean isAnyTbl;

    protected TablePrivEntry() {
    }

    private TablePrivEntry(
            PatternMatcher ctlPattern, String origCtl,
            PatternMatcher dbPattern, String origDb,
            PatternMatcher tblPattern, String origTbl,
            PrivBitSet privSet) {
        super(ctlPattern, origCtl, dbPattern, origDb, privSet);
        this.tblPattern = tblPattern;
        key = new PrivKey.TablePrivKey(origCtl, origDb, origTbl);
        if (origTbl.equals(ANY_TBL)) {
            isAnyTbl = true;
        }
    }

    public static TablePrivEntry create(
            String ctl, String db, String tbl,
            PrivBitSet privs) throws AnalysisException {
        PatternMatcher dbPattern = PatternMatcher.createFlatPattern(
                db, CaseSensibility.DATABASE.getCaseSensibility(), db.equals(ANY_DB));
        PatternMatcher ctlPattern = PatternMatcher.createFlatPattern(
                ctl, CaseSensibility.CATALOG.getCaseSensibility(), ctl.equals(ANY_CTL));

        PatternMatcher tblPattern = PatternMatcher.createFlatPattern(
                tbl, Env.isTableNamesCaseSensitive(), tbl.equals(ANY_TBL));

        if (privs.containsNodePriv() || privs.containsResourcePriv()) {
            throw new AnalysisException("Table privilege can not contains global or resource privileges: " + privs);
        }

        return new TablePrivEntry(
                ctlPattern, ctl, dbPattern, db, tblPattern, tbl, privs);
    }

    public PatternMatcher getTblPattern() {
        return tblPattern;
    }

    public String getOrigTbl() {
        return ((PrivKey.TablePrivKey) key).getTbl();
    }

    public boolean isAnyTbl() {
        return isAnyTbl;
    }

    @Override
    public String toString() {
        return String.format("table privilege.ctl: %s, db: %s, tbl: %s, priv: %s", getOrigCtl(), getOrigDb(),
            getOrigTbl(), privSet.toString());
    }

    @Deprecated
    public void readFields(DataInput in) throws IOException {
        super.readFields(in);

        String origCtl = getOrigCtl();
        String origDb = getOrigDb();
        String origTbl = Text.readString(in);
        this.key = new PrivKey.TablePrivKey(origCtl, origDb, origTbl);
        try {
            tblPattern = PatternMatcher.createMysqlPattern(origTbl, CaseSensibility.TABLE.getCaseSensibility());
        } catch (PatternMatcherException e) {
            throw new IOException(e);
        }
        isAnyTbl = origTbl.equals(ANY_TBL);
    }

    @Override
    protected PrivEntry copy() throws AnalysisException, PatternMatcherException {
        return TablePrivEntry.create(this.getOrigCtl(), this.getOrigDb(), this.getOrigTbl(), this.getPrivSet().copy());
    }
}
