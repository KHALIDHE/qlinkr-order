# SQL notes

All dashboard numbers are computed in Oracle (`backend/src/main/java/ma/qlinkr/stats/StatsRepository.java`).
Queries use window functions (`RANK() OVER`, a 7-day moving `AVG() OVER`, `RATIO_TO_REPORT`) and
`PERCENTILE_CONT` for the median and p90 preparation time.

## Index for the dashboard

`V5__dashboard_indexes.sql` adds `ix_order_business_created (business_id, created_at, status, total_cents)`.
Every dashboard query filters on one business and a date range; the index serves that range scan, and since it
also holds `status` and `total_cents`, the sales total can be answered from the index without reading the table.

The plans below were produced by `DashboardStatsTest.recordsQueryPlansBeforeAndAfterTheIndex` with
`EXPLAIN PLAN` on Oracle Database Free 23ai. "Before" uses an Oracle **invisible index**: the index exists but
the optimizer ignores it, which shows the plan without dropping anything.

Query: orders and revenue of one business over two days, excluding cancelled and rejected orders.

```
-- index INVISIBLE
Plan hash value: 707094883
 
-------------------------------------------------------------------------------------
| Id  | Operation          | Name           | Rows  | Bytes | Cost (%CPU)| Time     |
-------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT   |                |     1 |    47 |     3   (0)| 00:00:01 |
|   1 |  SORT AGGREGATE    |                |     1 |    47 |            |          |
|*  2 |   TABLE ACCESS FULL| CUSTOMER_ORDER |     3 |   141 |     3   (0)| 00:00:01 |
-------------------------------------------------------------------------------------
 
Predicate Information (identified by operation id):
---------------------------------------------------
 
   2 - filter("BUSINESS_ID"=4 AND "STATUS"<>'CANCELLED' AND 
              "STATUS"<>'REJECTED' AND SYS_EXTRACT_UTC("CREATED_AT")>=TIMESTAMP' 
              2026-01-10 00:00:00.000000000' AND SYS_EXTRACT_UTC("CREATED_AT")<TIMESTAMP' 
              2026-01-12 00:00:00.000000000')
 
Note
-----
   - dynamic statistics used: dynamic sampling (level=2)
 
SQL Analysis Report (identified by operation id/Query Block Name/Object Alias):
-------------------------------------------------------------------------------
 
   2 -  SEL$1 / "CUSTOMER_ORDER"@"SEL$1"
           -  The following columns have predicates which preclude their 
              use as keys in index range scan. Consider rewriting the 
              predicates.
                "STATUS"
-- index VISIBLE
Plan hash value: 4136049505
 
---------------------------------------------------------------------------------------------------
| Id  | Operation             | Name                      | Rows  | Bytes | Cost (%CPU)| Time     |
---------------------------------------------------------------------------------------------------
|   0 | SELECT STATEMENT      |                           |     1 |    47 |     2   (0)| 00:00:01 |
|   1 |  SORT AGGREGATE       |                           |     1 |    47 |            |          |
|*  2 |   INDEX FAST FULL SCAN| IX_ORDER_BUSINESS_CREATED |     3 |   141 |     2   (0)| 00:00:01 |
---------------------------------------------------------------------------------------------------
 
Predicate Information (identified by operation id):
---------------------------------------------------
 
   2 - filter("BUSINESS_ID"=4 AND "STATUS"<>'CANCELLED' AND "STATUS"<>'REJECTED' AND 
              SYS_EXTRACT_UTC("CREATED_AT")>=TIMESTAMP' 2026-01-10 00:00:00.000000000' AND 
              SYS_EXTRACT_UTC("CREATED_AT")<TIMESTAMP' 2026-01-12 00:00:00.000000000')
 
Note
-----
   - dynamic statistics used: dynamic sampling (level=2)
 
SQL Analysis Report (identified by operation id/Query Block Name/Object Alias):
-------------------------------------------------------------------------------
 
   2 -  SEL$1 / "CUSTOMER_ORDER"@"SEL$1"
           -  The following columns have predicates which preclude their 
              use as keys in index range scan. Consider rewriting the 
              predicates.
                "STATUS"
```

The test data is small, so costs are tiny either way; what matters is the access path: a full table scan
before, an index range scan on `IX_ORDER_BUSINESS_CREATED` after. The test fails if the index is not used.
