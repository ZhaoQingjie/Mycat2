package io.mycat.calcite.prepare;

import io.mycat.PlanRunner;
import io.mycat.api.collector.RowBaseIterator;
import io.mycat.beans.mycat.MycatRowMetaData;
import io.mycat.calcite.CalciteRunners;
import io.mycat.calcite.MycatCalciteDataContext;
import io.mycat.calcite.MycatCalciteSupport;
import io.mycat.calcite.resultset.CalciteRowMetaData;
import io.mycat.hbt.HBTQueryConvertor;
import io.mycat.hbt.ast.base.Schema;
import io.mycat.upondb.MycatDBContext;
import io.mycat.util.Explains;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.sql.dialect.MysqlSqlDialect;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class MycatHbtCalcitePrepareObject extends MycatHbtPrepareObject {
    final Schema schema;
    private final MycatCalciteDataContext mycatCalciteDataContext;
    private  DatasourceInfo  datasourceInfo;

    public MycatHbtCalcitePrepareObject(Long id, int paramCount, Schema schema, MycatDBContext uponDBContext) {
        super(id, paramCount);
        this.schema = schema;
        this.mycatCalciteDataContext = MycatCalciteSupport.INSTANCE.create(uponDBContext);
    }

    @Override
    public MycatRowMetaData resultSetRowType() {
        RelNode handle = getRelNode(Collections.emptyList());
        return new CalciteRowMetaData(handle.getRowType().getFieldList());
    }

    @Override
    public PlanRunner plan(List<Object> params) {
        RelNode relNode = getRelNode(params);
        Supplier<RowBaseIterator> run = CalciteRunners.run(mycatCalciteDataContext, datasourceInfo.preSeq, relNode);
        return new PlanRunner() {
            @Override
            public RowBaseIterator run() {
                return run.get();
            }

            @Override
            public List<String> explain() {
                return Explains.explain(MycatCalciteSupport.INSTANCE.convertToSql(relNode, MysqlSqlDialect.DEFAULT,false),
                        MycatCalciteSupport.INSTANCE.convertToHBTText(datasourceInfo.preSeq),
                        MycatCalciteSupport.INSTANCE.dumpMetaData(relNode.getRowType()),
                        MycatCalciteSupport.INSTANCE.convertToHBTText(relNode, mycatCalciteDataContext),
                        MycatCalciteSupport.INSTANCE.convertToMycatRelNodeText(relNode, mycatCalciteDataContext));
            }

        };
    }

    public RelNode getRelNode(List<Object> params) {
        MycatCalcitePlanner planner1 = MycatCalciteSupport.INSTANCE.createPlanner(mycatCalciteDataContext);
        HBTQueryConvertor hbtConvertor = new HBTQueryConvertor( params,mycatCalciteDataContext);

        RelNode handle = hbtConvertor.handle(schema);
        this.datasourceInfo = planner1.preComputeSeq(handle);
        return handle;
    }

}