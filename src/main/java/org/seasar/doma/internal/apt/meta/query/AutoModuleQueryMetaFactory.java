package org.seasar.doma.internal.apt.meta.query;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import org.seasar.doma.In;
import org.seasar.doma.InOut;
import org.seasar.doma.Out;
import org.seasar.doma.internal.apt.AptException;
import org.seasar.doma.internal.apt.Context;
import org.seasar.doma.internal.apt.annot.ResultSetAnnot;
import org.seasar.doma.internal.apt.cttype.BasicCtType;
import org.seasar.doma.internal.apt.cttype.CtType;
import org.seasar.doma.internal.apt.cttype.DomainCtType;
import org.seasar.doma.internal.apt.cttype.EntityCtType;
import org.seasar.doma.internal.apt.cttype.IterableCtType;
import org.seasar.doma.internal.apt.cttype.MapCtType;
import org.seasar.doma.internal.apt.cttype.OptionalCtType;
import org.seasar.doma.internal.apt.cttype.OptionalDoubleCtType;
import org.seasar.doma.internal.apt.cttype.OptionalIntCtType;
import org.seasar.doma.internal.apt.cttype.OptionalLongCtType;
import org.seasar.doma.internal.apt.cttype.ReferenceCtType;
import org.seasar.doma.internal.apt.cttype.SimpleCtTypeVisitor;
import org.seasar.doma.internal.apt.meta.dao.DaoMeta;
import org.seasar.doma.internal.apt.meta.parameter.*;
import org.seasar.doma.message.Message;

public abstract class AutoModuleQueryMetaFactory<M extends AutoModuleQueryMeta>
    extends AbstractQueryMetaFactory<M> {

  protected AutoModuleQueryMetaFactory(Context ctx) {
    super(ctx);
  }

  @Override
  protected void doParameters(M queryMeta, ExecutableElement method, DaoMeta daoMeta) {
    for (VariableElement parameter : method.getParameters()) {
      QueryParameterMeta parameterMeta = createParameterMeta(parameter, queryMeta);
      queryMeta.addParameterMeta(parameterMeta);

      CallableSqlParameterMeta callableSqlParameterMeta = createParameterMeta(parameterMeta);
      queryMeta.addCallableSqlParameterMeta(callableSqlParameterMeta);

      if (parameterMeta.isBindable()) {
        queryMeta.addBindableParameterCtType(parameterMeta.getName(), parameterMeta.getCtType());
      }
    }
  }

  protected CallableSqlParameterMeta createParameterMeta(final QueryParameterMeta parameterMeta) {
    ResultSetAnnot resultSetAnnot =
        ctx.getAnnotations().newResultSetAnnot(parameterMeta.getElement());
    if (resultSetAnnot != null) {
      return createResultSetParameterMeta(parameterMeta, resultSetAnnot);
    }
    if (parameterMeta.isAnnotated(In.class)) {
      return createInParameterMeta(parameterMeta);
    }
    if (parameterMeta.isAnnotated(Out.class)) {
      return createOutParameterMeta(parameterMeta);
    }
    if (parameterMeta.isAnnotated(InOut.class)) {
      return createInOutParameterMeta(parameterMeta);
    }
    throw new AptException(Message.DOMA4066, parameterMeta.getElement(), new Object[] {});
  }

  protected CallableSqlParameterMeta createResultSetParameterMeta(
      final QueryParameterMeta parameterMeta, final ResultSetAnnot resultSetAnnot) {
    IterableCtType iterableCtType =
        parameterMeta.getCtType().accept(new ResultSetCtTypeVisitor(parameterMeta), null);
    return iterableCtType
        .getElementCtType()
        .accept(new ResultSetElementCtTypeVisitor(parameterMeta, resultSetAnnot), false);
  }

  protected CallableSqlParameterMeta createInParameterMeta(final QueryParameterMeta parameterMeta) {
    return parameterMeta.getCtType().accept(new InCtTypeVisitor(parameterMeta), false);
  }

  protected CallableSqlParameterMeta createOutParameterMeta(
      final QueryParameterMeta parameterMeta) {
    final ReferenceCtType referenceCtType =
        parameterMeta.getCtType().accept(new OutCtTypeVisitor(parameterMeta), null);
    return referenceCtType
        .getReferentCtType()
        .accept(new OutReferentCtTypeVisitor(parameterMeta, referenceCtType), false);
  }

  protected CallableSqlParameterMeta createInOutParameterMeta(
      final QueryParameterMeta parameterMeta) {
    final ReferenceCtType referenceCtType =
        parameterMeta.getCtType().accept(new InOutCtTypeVisitor(parameterMeta), null);
    return referenceCtType
        .getReferentCtType()
        .accept(new InOutReferentCtTypeVisitor(parameterMeta, referenceCtType), false);
  }

  protected class ResultSetCtTypeVisitor
      extends SimpleCtTypeVisitor<IterableCtType, Void, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    protected ResultSetCtTypeVisitor(QueryParameterMeta parameterMeta) {
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected IterableCtType defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(Message.DOMA4062, parameterMeta.getElement(), new Object[] {});
    }

    @Override
    public IterableCtType visitIterableCtType(IterableCtType ctType, Void p)
        throws RuntimeException {
      if (!ctType.isList()) {
        defaultAction(ctType, p);
      }
      return ctType;
    }
  }

  protected class ResultSetElementCtTypeVisitor
      extends SimpleCtTypeVisitor<CallableSqlParameterMeta, Boolean, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    protected final ResultSetAnnot resultSetAnnot;

    public ResultSetElementCtTypeVisitor(
        QueryParameterMeta parameterMeta, ResultSetAnnot resultSetAnnot) {
      this.parameterMeta = parameterMeta;
      this.resultSetAnnot = resultSetAnnot;
    }

    @Override
    protected CallableSqlParameterMeta defaultAction(CtType type, Boolean p)
        throws RuntimeException {
      throw new AptException(
          Message.DOMA4186, parameterMeta.getElement(), new Object[] {type.getTypeName()});
    }

    @Override
    public CallableSqlParameterMeta visitEntityCtType(EntityCtType ctType, Boolean p)
        throws RuntimeException {
      if (ctType.isAbstract()) {
        throw new AptException(
            Message.DOMA4157, parameterMeta.getElement(), new Object[] {ctType.getTypeName()});
      }
      return new EntityListParameterMeta(
          parameterMeta.getName(), ctType, resultSetAnnot.getEnsureResultMappingValue());
    }

    @Override
    public CallableSqlParameterMeta visitMapCtType(MapCtType ctType, Boolean p)
        throws RuntimeException {
      return new MapListParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitBasicCtType(BasicCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalBasicListParameterMeta(parameterMeta.getName(), ctType);
      }
      return new BasicListParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitDomainCtType(DomainCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalDomainListParameterMeta(parameterMeta.getName(), ctType);
      }
      return new DomainListParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalCtType(OptionalCtType ctType, Boolean p)
        throws RuntimeException {
      return ctType.getElementCtType().accept(this, true);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalIntCtType(OptionalIntCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalIntListParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalLongCtType(OptionalLongCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalLongListParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalDoubleCtType(
        OptionalDoubleCtType ctType, Boolean p) throws RuntimeException {
      return new OptionalDoubleListParameterMeta(parameterMeta.getName());
    }
  }

  protected class InCtTypeVisitor
      extends SimpleCtTypeVisitor<CallableSqlParameterMeta, Boolean, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    public InCtTypeVisitor(QueryParameterMeta parameterMeta) {
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected CallableSqlParameterMeta defaultAction(CtType type, Boolean p)
        throws RuntimeException {
      throw new AptException(
          Message.DOMA4101, parameterMeta.getElement(), new Object[] {parameterMeta.getType()});
    }

    @Override
    public CallableSqlParameterMeta visitBasicCtType(BasicCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalBasicInParameterMeta(parameterMeta.getName(), ctType);
      }
      return new BasicInParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitDomainCtType(DomainCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalDomainInParameterMeta(parameterMeta.getName(), ctType);
      }
      return new DomainInParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalCtType(OptionalCtType ctType, Boolean p)
        throws RuntimeException {
      return ctType.getElementCtType().accept(this, true);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalIntCtType(OptionalIntCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalIntInParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalLongCtType(OptionalLongCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalLongInParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalDoubleCtType(
        OptionalDoubleCtType ctType, Boolean p) throws RuntimeException {
      return new OptionalDoubleInParameterMeta(parameterMeta.getName());
    }
  }

  protected class OutCtTypeVisitor
      extends SimpleCtTypeVisitor<ReferenceCtType, Void, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    public OutCtTypeVisitor(QueryParameterMeta parameterMeta) {
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected ReferenceCtType defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(Message.DOMA4098, parameterMeta.getElement(), new Object[] {});
    }

    @Override
    public ReferenceCtType visitReferenceCtType(ReferenceCtType ctType, Void p)
        throws RuntimeException {
      return ctType;
    }
  }

  protected class OutReferentCtTypeVisitor
      extends SimpleCtTypeVisitor<CallableSqlParameterMeta, Boolean, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    protected final ReferenceCtType referenceCtType;

    public OutReferentCtTypeVisitor(
        QueryParameterMeta parameterMeta, ReferenceCtType referenceCtType) {
      this.parameterMeta = parameterMeta;
      this.referenceCtType = referenceCtType;
    }

    @Override
    protected CallableSqlParameterMeta defaultAction(CtType type, Boolean p)
        throws RuntimeException {
      throw new AptException(
          Message.DOMA4100,
          parameterMeta.getElement(),
          new Object[] {referenceCtType.getReferentTypeMirror()});
    }

    @Override
    public CallableSqlParameterMeta visitBasicCtType(BasicCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalBasicOutParameterMeta(parameterMeta.getName(), ctType);
      }
      return new BasicOutParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitDomainCtType(DomainCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalDomainOutParameterMeta(parameterMeta.getName(), ctType);
      }
      return new DomainOutParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalCtType(OptionalCtType ctType, Boolean p)
        throws RuntimeException {
      return ctType.getElementCtType().accept(this, true);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalIntCtType(OptionalIntCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalIntOutParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalLongCtType(OptionalLongCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalLongOutParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalDoubleCtType(
        OptionalDoubleCtType ctType, Boolean p) throws RuntimeException {
      return new OptionalDoubleOutParameterMeta(parameterMeta.getName());
    }
  }

  protected class InOutCtTypeVisitor
      extends SimpleCtTypeVisitor<ReferenceCtType, Void, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    public InOutCtTypeVisitor(QueryParameterMeta parameterMeta) {
      this.parameterMeta = parameterMeta;
    }

    @Override
    protected ReferenceCtType defaultAction(CtType type, Void p) throws RuntimeException {
      throw new AptException(Message.DOMA4111, parameterMeta.getElement(), new Object[] {});
    }

    @Override
    public ReferenceCtType visitReferenceCtType(ReferenceCtType ctType, Void p)
        throws RuntimeException {
      return ctType;
    }
  }

  protected class InOutReferentCtTypeVisitor
      extends SimpleCtTypeVisitor<CallableSqlParameterMeta, Boolean, RuntimeException> {

    protected final QueryParameterMeta parameterMeta;

    protected final ReferenceCtType referenceCtType;

    public InOutReferentCtTypeVisitor(
        QueryParameterMeta parameterMeta, ReferenceCtType referenceCtType) {
      this.parameterMeta = parameterMeta;
      this.referenceCtType = referenceCtType;
    }

    @Override
    protected CallableSqlParameterMeta defaultAction(CtType type, Boolean p)
        throws RuntimeException {
      throw new AptException(
          Message.DOMA4100,
          parameterMeta.getElement(),
          new Object[] {referenceCtType.getReferentTypeMirror()});
    }

    @Override
    public CallableSqlParameterMeta visitBasicCtType(BasicCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalBasicInOutParameterMeta(parameterMeta.getName(), ctType);
      }
      return new BasicInOutParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitDomainCtType(DomainCtType ctType, Boolean optional)
        throws RuntimeException {
      if (Boolean.TRUE == optional) {
        return new OptionalDomainInOutParameterMeta(parameterMeta.getName(), ctType);
      }
      return new DomainInOutParameterMeta(parameterMeta.getName(), ctType);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalCtType(OptionalCtType ctType, Boolean p)
        throws RuntimeException {
      return ctType.getElementCtType().accept(this, true);
    }

    @Override
    public CallableSqlParameterMeta visitOptionalIntCtType(OptionalIntCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalIntInOutParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalLongCtType(OptionalLongCtType ctType, Boolean p)
        throws RuntimeException {
      return new OptionalLongInOutParameterMeta(parameterMeta.getName());
    }

    @Override
    public CallableSqlParameterMeta visitOptionalDoubleCtType(
        OptionalDoubleCtType ctType, Boolean p) throws RuntimeException {
      return new OptionalDoubleInOutParameterMeta(parameterMeta.getName());
    }
  }
}
