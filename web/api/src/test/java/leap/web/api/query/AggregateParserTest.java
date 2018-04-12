/*
 *  Copyright 2018 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package leap.web.api.query;

import leap.junit.TestBase;
import leap.web.exception.BadRequestException;
import org.junit.Test;

public class AggregateParserTest extends TestBase {

    @Test
    public void testInvalidExpr() {
        assertInvalidExpr("a");
        assertInvalidExpr("sum()");
        assertInvalidExpr("sum( )");
        assertInvalidExpr("sum(a) a b");
        assertInvalidExpr("sum(a),a");
    }

    @Test
    public void testSingle() {
        assertAggregate(parseSingle("sum(a)"));
        assertAggregate(parseSingle("sum(a),"));
        assertAggregate(parseSingle(",sum(a)"));
        assertAggregate(parseSingle(",sum(a),"));
        assertAggregate(parseSingle("sum( a)"));
        assertAggregate(parseSingle("sum( a )"));
        assertAggregate(parseSingle("sum(a) aSum"));
        assertAggregate(parseSingle("sum(a) as aSum"));

        assertAggregate(parseSingle("sum(a) b"), "b");
        assertAggregate(parseSingle("sum(a) as c"), "c");
    }

    @Test
    public void testMulti() {
        Aggregate[] aggs = AggregateParser.parse("sum(a), avg(b)");
        assertAggregate(aggs[0]);
        assertAggregate(aggs[1], "bAvg", "avg", "b");

        aggs = AggregateParser.parse("sum(a) aa, avg(b) as b");
        assertAggregate(aggs[0], "aa");
        assertAggregate(aggs[1], "b", "avg", "b");
    }

    private static void assertAggregate(Aggregate agg) {
        assertAggregate(agg, "aSum");
    }

    private static void assertAggregate(Aggregate agg, String alias) {
        assertAggregate(agg, alias, "sum", "a");
    }

    private static void assertAggregate(Aggregate agg, String alias, String func, String field) {
        assertEquals(field, agg.getField());
        assertEquals(func, agg.getFunction());
        assertEquals(alias, agg.getAlias());
    }

    private static Aggregate parseSingle(String expr) {
        return AggregateParser.parse(expr)[0];
    }

    private static void assertInvalidExpr(String expr) {
         try{
             AggregateParser.parse(expr);
             fail("Should throw bad request");
         }catch (BadRequestException e) {

         }
    }

}
