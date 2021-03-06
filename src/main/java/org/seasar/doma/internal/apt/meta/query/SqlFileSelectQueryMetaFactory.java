package org.seasar.doma.internal.apt.meta.query;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic.Kind;
import org.seasar.doma.SelectType;
import org.seasar.doma.Suppress;
import org.seasar.doma.internal.apt.AptException;
import org.seasar.doma.internal.apt.Context;
import org.seasar.doma.internal.apt.annot.SelectAnnot;
import org.seasar.doma.internal.apt.cttype.BasicCtType;
import org.seasar.doma.internal.apt.cttype.CollectorCtType;
import org.seasar.doma.internal.apt.cttype.CtType;
import org.seasar.doma.internal.apt.cttype.DomainCtType;
import org.seasar.doma.internal.apt.cttype.EntityCtType;
import org.seasar.doma.internal.apt.cttype.FunctionCtType;
import org.seasar.doma.internal.apt.cttype.IterableCtType;
import org.seasar.doma.internal.apt.cttype.MapCtType;
import org.seasar.doma.internal.apt.cttype.OptionalCtType;
import org.seasar.doma.internal.apt.cttype.OptionalDoubleCtType;
import org.seasar.doma.internal.apt.cttype.OptionalIntCtType;
import org.seasar.doma.internal.apt.cttype.OptionalLongCtType;
import org.seasar.doma.internal.apt.cttype.SelectOptionsCtType;
import org.seasar.doma.internal.apt.cttype.SimpleCtTypeVisitor;
import org.seasar.doma.internal.apt.cttype.StreamCtType;
import org.seasar.doma.internal.apt.meta.dao.DaoMeta;
import org.seasar.doma.message.Message;

public class SqlFileSelectQueryMetaFactory
    extends AbstractSqlFileQueryMetaFactory<SqlFileSelectQueryMeta> {

  public SqlFileSelectQueryMetaFactory(Context ctx) {
    super(ctx);
  }

  @Override
  public QueryMeta createQueryMeta(ExecutableElement method, DaoMeta daoMeta) {
    assertNotNull(method, daoMeta);
    SqlFileSelectQueryMeta queryMeta = createSqlFileSelectQueryMeta(method, daoMeta);
    if (queryMeta == null) {
      return null;
    }
    doTypeParameters(queryMeta, method, daoMeta);
    doParameters(queryMeta, method, daoMeta);
    doReturnType(queryMeta, method, daoMeta);
    doThrowTypes(queryMeta, method, daoMeta);
    doSqlFiles(queryMeta, method, daoMeta, queryMeta.isExpandable(), false);
    return queryMeta;
  }

  protected SqlFileSelectQueryMeta createSqlFileSelectQueryMeta(
      ExecutableElement method, DaoMeta daoMeta) {
    SelectAnnot selectAnnot = ctx.getAnnotations().newSelectAnnot(method);
    if (selectAnnot == null) {
      return null;
    }
    SqlFileSelectQueryMeta queryMeta = new SqlFileSelectQueryMeta(method, daoMeta.getDaoElement());
    queryMeta.setSelectAnnot(selectAnnot);
    queryMeta.setQueryKind(QueryKind.SQLFILE_SELECT);
    return queryMeta;
  }

  @Override
  protected void doParameters(
      final SqlFileSelectQueryMeta queryMeta, ExecutableElement method, DaoMeta daoMeta) {
    for (VariableElement parameter : method.getParameters()) {
      final QueryParameterMeta parameterMeta = createParameterMeta(parameter, queryMeta);
      parameterMeta.getCtType().accept(new ParamCtTypeVisitor(queryMeta, parameterMeta), null);
      queryMeta.addParameterMeta(parameterMeta);
      if (parameterMeta.isBindable()) {
        queryMeta.addBindableParameterCtType(parameterMeta.getName(), parameterMeta.getCtType());
      }
    }

    if (queryMeta.getSelectStrategyType() == SelectType.STREAM) {
      if (queryMeta.getFunctionCtType() == null) {
        throw new AptException(Message.DOMA4247, method, new Object[] {});
      }
    } else if (queryMeta.getSelectStrategyType() == SelectType.COLLECT) {
      if (queryMeta.getCollectorCtType() == null) {
        throw new AptException(Message.DOMA4266, method, new Object[] {});
      }
    } else {
      if (queryMeta.getFunctionCtType() != null) {
        SelectAnnot selectAnnot = queryMeta.getSelectAnnot();
        throw new AptException(
            Message.DOMA4248,
            method,
            selectAnnot.getAnnotationMirror(),
            selectAnnot.getStrategy(),
            new Object[] {});
      }
    }
  }

  @Override
  protected void doReturnType(
      final SqlFileSelectQueryMeta queryMeta, ExecutableElement method, DaoMeta daoMeta) {
    final QueryReturnMeta returnMeta = createReturnMeta(queryMeta);
    queryMeta.setReturnMeta(returnMeta);

    if (queryMeta.getSelectStrategyType() == SelectType.STREAM) {
      FunctionCtType functionCtType = queryMeta.getFunctionCtType();
      CtType returnCtType = functionCtType.getReturnCtType();
      if (returnCtType == null
          || !ctx.getTypes().isSameType(returnMeta.getType(), returnCtType.getType())) {
        throw new AptException(
            Message.DOMA4246,
            method,
            new Object[] {returnMeta.getType(), returnCtType.getBoxedTypeName()});
      }
    } else if (queryMeta.getSelectStrategyType() == SelectType.COLLECT) {
      CollectorCtType collectorCtType = queryMeta.getCollectorCtType();
      CtType returnCtType = collectorCtType.getReturnCtType();
      if (returnCtType == null
          || !ctx.getTypes().isSameType(returnMeta.getType(), returnCtType.getType())) {
        throw new AptException(
            Message.DOMA4265,
            method,
            new Object[] {returnMeta.getType(), returnCtType.getBoxedTypeName()});
      }
    } else {
      returnMeta.getCtType().accept(new ReturnCtTypeVisitor(queryMeta, returnMeta), null);
    }
  }

  protected class ParamCtTypeVisitor extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryParameterMeta parameterMeta;

    protected ParamCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryParameterMeta parameterMeta) {
      this.queryMeta = queryMeta;
      this.parameterMeta = parameterMeta;
    }

    @Override
    public Void visitFunctionCtType(FunctionCtType ctType, Void p) throws RuntimeException {
      if (queryMeta.getFunctionCtType() != null) {
        throw new AptException(Message.DOMA4249, parameterMeta.getElement(), new Object[] {});
      }
      ctType
          .getTargetCtType()
          .accept(new ParamFunctionTargetCtTypeVisitor(queryMeta, parameterMeta), null);
      queryMeta.setFunctionCtType(ctType);
      queryMeta.setFunctionParameterName(parameterMeta.getName());
      return null;
    }

    @Override
    public Void visitCollectorCtType(CollectorCtType ctType, Void p) throws RuntimeException {
      if (queryMeta.getCollectorCtType() != null) {
        throw new AptException(Message.DOMA4264, parameterMeta.getElement(), new Object[] {});
      }
      ctType
          .getTargetCtType()
          .accept(new ParamCollectorTargetCtTypeVisitor(queryMeta, parameterMeta), null);
      queryMeta.setCollectorCtType(ctType);
      queryMeta.setCollectorParameterName(parameterMeta.getName());
      return null;
    }

    @Override
    public Void visitSelectOptionsCtType(SelectOptionsCtType ctType, Void p)
        throws RuntimeException {
      if (queryMeta.getSelectOptionsCtType() != null) {
        throw new AptException(Message.DOMA4053, parameterMeta.getElement(), new Object[] {});
      }
      queryMeta.setSelectOptionsCtType(ctType);
      queryMeta.setSelectOptionsParameterName(parameterMeta.getName());
      return null;
    }
  }

  protected class ParamFunctionTargetCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryParameterMeta parameterMeta;

    protected ParamFunctionTargetCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryParameterMeta parameterMeta) {
      this.queryMeta = queryMeta;
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(Message.DOMA4244, queryMeta.getMethodElement(), new Object[] {});
    }

    @Override
    public Void visitStreamCtType(StreamCtType ctType, Void p) throws RuntimeException {
      return ctType.getElementCtType().accept(new StreamElementCtTypeVisitor(), null);
    }

    protected class StreamElementCtTypeVisitor
        extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

      @Override
      protected Void defaultAction(CtType ctType, Void p) throws RuntimeException {
        throw new AptException(Message.DOMA4245, queryMeta.getMethodElement(), new Object[] {});
      }

      @Override
      public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
        return null;
      }

      @Override
      public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
        return null;
      }

      @Override
      public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
        return null;
      }

      @Override
      public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
        if (ctType.isAbstract()) {
          throw new AptException(
              Message.DOMA4250, parameterMeta.getElement(), new Object[] {ctType.getTypeName()});
        }
        queryMeta.setEntityCtType(ctType);
        return null;
      }

      @Override
      public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
        Boolean valid =
            ctType
                .getElementCtType()
                .accept(
                    new SimpleCtTypeVisitor<Boolean, Void, RuntimeException>() {

                      @Override
                      protected Boolean defaultAction(CtType ctType, Void p)
                          throws RuntimeException {
                        return false;
                      }

                      @Override
                      public Boolean visitBasicCtType(BasicCtType ctType, Void p)
                          throws RuntimeException {
                        return true;
                      }

                      @Override
                      public Boolean visitDomainCtType(DomainCtType ctType, Void p)
                          throws RuntimeException {
                        return true;
                      }
                    },
                    null);
        if (Boolean.FALSE == valid) {
          defaultAction(ctType, null);
        }
        return null;
      }

      @Override
      public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
        return null;
      }

      @Override
      public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p)
          throws RuntimeException {
        return null;
      }

      @Override
      public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
          throws RuntimeException {
        return null;
      }
    }
  }

  protected class ParamCollectorTargetCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryParameterMeta parameterMeta;

    protected ParamCollectorTargetCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryParameterMeta parameterMeta) {
      this.queryMeta = queryMeta;
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected Void defaultAction(CtType ctType, Void p) throws RuntimeException {
      throw new AptException(Message.DOMA4262, queryMeta.getMethodElement(), new Object[] {});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4263, parameterMeta.getElement(), new Object[] {ctType.getTypeName()});
      }
      queryMeta.setEntityCtType(ctType);
      return null;
    }

    @Override
    public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
      Boolean valid =
          ctType
              .getElementCtType()
              .accept(
                  new SimpleCtTypeVisitor<Boolean, Void, RuntimeException>() {

                    @Override
                    protected Boolean defaultAction(CtType ctType, Void p) throws RuntimeException {
                      return false;
                    }

                    @Override
                    public Boolean visitBasicCtType(BasicCtType ctType, Void p)
                        throws RuntimeException {
                      return true;
                    }

                    @Override
                    public Boolean visitDomainCtType(DomainCtType ctType, Void p)
                        throws RuntimeException {
                      return true;
                    }
                  },
                  null);
      if (Boolean.FALSE == valid) {
        defaultAction(ctType, null);
      }
      return null;
    }

    @Override
    public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
        throws RuntimeException {
      return null;
    }
  }

  protected class ReturnCtTypeVisitor extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected Suppress suppress;

    protected ReturnCtTypeVisitor(SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
      this.suppress = queryMeta.getMethodElement().getAnnotation(Suppress.class);
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4008, returnMeta.getMethodElement(), new Object[] {returnMeta.getType()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4154,
            returnMeta.getMethodElement(),
            new Object[] {ctType.getQualifiedName()});
      }
      queryMeta.setEntityCtType(ctType);
      return null;
    }

    @Override
    public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitIterableCtType(IterableCtType ctType, Void p) throws RuntimeException {
      if (!ctType.isList()) {
        defaultAction(ctType, p);
      }
      ctType
          .getElementCtType()
          .accept(new ReturnListElementCtTypeVisitor(queryMeta, returnMeta), p);
      return null;
    }

    @Override
    public Void visitStreamCtType(StreamCtType ctType, Void p) throws RuntimeException {
      if (!isSuppressed(Message.DOMA4274)) {
        ctx.getNotifier()
            .notify(Kind.WARNING, Message.DOMA4274, returnMeta.getMethodElement(), new Object[] {});
      }
      queryMeta.setResultStream(true);
      ctType
          .getElementCtType()
          .accept(new ReturnStreamElementCtTypeVisitor(queryMeta, returnMeta), p);
      return null;
    }

    @Override
    public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
      ctType
          .getElementCtType()
          .accept(new ReturnOptionalElementCtTypeVisitor(queryMeta, returnMeta), p);
      return null;
    }

    @Override
    public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
        throws RuntimeException {
      return null;
    }

    protected boolean isSuppressed(Message message) {
      if (suppress != null) {
        for (Message suppressMessage : suppress.messages()) {
          if (suppressMessage == message) {
            return true;
          }
        }
      }
      return false;
    }
  }

  protected class ReturnListElementCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected ReturnListElementCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4007, returnMeta.getMethodElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4155, returnMeta.getMethodElement(), new Object[] {ctType.getType()});
      }
      queryMeta.setEntityCtType(ctType);
      return null;
    }

    @Override
    public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
      ctType
          .getElementCtType()
          .accept(new ReturnListOptionalElementCtTypeVisitor(queryMeta, returnMeta), p);
      return null;
    }

    @Override
    public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
        throws RuntimeException {
      return null;
    }
  }

  protected class ReturnStreamElementCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected ReturnStreamElementCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4271, returnMeta.getMethodElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4272, returnMeta.getMethodElement(), new Object[] {ctType.getType()});
      }
      queryMeta.setEntityCtType(ctType);
      return null;
    }

    @Override
    public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
      ctType
          .getElementCtType()
          .accept(new ReturnStreamOptionalElementCtTypeVisitor(queryMeta, returnMeta), p);
      return null;
    }

    @Override
    public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
        throws RuntimeException {
      return null;
    }
  }

  protected class ReturnOptionalElementCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected ReturnOptionalElementCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4235, returnMeta.getMethodElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitMapCtType(MapCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitEntityCtType(EntityCtType ctType, Void p) throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4234, returnMeta.getMethodElement(), new Object[] {ctType.getType()});
      }
      queryMeta.setEntityCtType(ctType);
      return null;
    }
  }

  protected class ReturnListOptionalElementCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected ReturnListOptionalElementCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4267, returnMeta.getMethodElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }
  }

  protected class ReturnStreamOptionalElementCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    protected SqlFileSelectQueryMeta queryMeta;

    protected QueryReturnMeta returnMeta;

    protected ReturnStreamOptionalElementCtTypeVisitor(
        SqlFileSelectQueryMeta queryMeta, QueryReturnMeta returnMeta) {
      this.queryMeta = queryMeta;
      this.returnMeta = returnMeta;
    }

    @Override
    protected Void defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(
          Message.DOMA4267, returnMeta.getMethodElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public Void visitBasicCtType(BasicCtType ctType, Void p) throws RuntimeException {
      return null;
    }

    @Override
    public Void visitDomainCtType(DomainCtType ctType, Void p) throws RuntimeException {
      return null;
    }
  }
}
