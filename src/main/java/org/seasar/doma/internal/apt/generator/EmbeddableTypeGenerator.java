package org.seasar.doma.internal.apt.generator;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import java.util.*;
import org.seasar.doma.internal.apt.Context;
import org.seasar.doma.internal.apt.cttype.BasicCtType;
import org.seasar.doma.internal.apt.cttype.CtType;
import org.seasar.doma.internal.apt.cttype.DomainCtType;
import org.seasar.doma.internal.apt.cttype.OptionalCtType;
import org.seasar.doma.internal.apt.cttype.OptionalDoubleCtType;
import org.seasar.doma.internal.apt.cttype.OptionalIntCtType;
import org.seasar.doma.internal.apt.cttype.OptionalLongCtType;
import org.seasar.doma.internal.apt.cttype.SimpleCtTypeVisitor;
import org.seasar.doma.internal.apt.cttype.WrapperCtType;
import org.seasar.doma.internal.apt.meta.entity.EmbeddableMeta;
import org.seasar.doma.internal.apt.meta.entity.EmbeddablePropertyMeta;
import org.seasar.doma.jdbc.entity.DefaultPropertyType;
import org.seasar.doma.jdbc.entity.EmbeddableType;
import org.seasar.doma.jdbc.entity.EntityPropertyType;
import org.seasar.doma.jdbc.entity.NamingType;
import org.seasar.doma.jdbc.entity.Property;

public class EmbeddableTypeGenerator extends AbstractGenerator {

  private final EmbeddableMeta embeddableMeta;

  public EmbeddableTypeGenerator(
      Context ctx, ClassName className, Printer printer, EmbeddableMeta embeddableMeta) {
    super(ctx, className, printer);
    assertNotNull(embeddableMeta);
    this.embeddableMeta = embeddableMeta;
  }

  @Override
  public void generate() {
    printPackage();
    printClass();
  }

  private void printPackage() {
    if (!packageName.isEmpty()) {
      iprint("package %1$s;%n", packageName);
      iprint("%n");
    }
  }

  private void printClass() {
    iprint("/** */%n");
    printGenerated();
    iprint(
        "public final class %1$s implements %2$s<%3$s> {%n",
        /* 1 */ simpleName,
        /* 2 */ EmbeddableType.class.getName(),
        /* 3 */ embeddableMeta.getEmbeddableElement().getQualifiedName());
    print("%n");
    indent();
    printValidateVersionStaticInitializer();
    printFields();
    printMethods();
    unindent();
    iprint("}%n");
  }

  private void printFields() {
    printSingletonField();
  }

  private void printSingletonField() {
    iprint("private static final %1$s __singleton = new %1$s();%n", simpleName);
    print("%n");
  }

  private void printMethods() {
    printGetEmbeddablePropertyTypesMethod();
    printNewEmbeddableMethod();
    printGetSingletonInternalMethod();
  }

  private void printGetEmbeddablePropertyTypesMethod() {
    iprint("@Override%n");
    iprint(
        "public <ENTITY> %1$s<%2$s<ENTITY, ?>> getEmbeddablePropertyTypes(String embeddedPropertyName, Class<ENTITY> entityClass, %3$s namingType) {%n",
        List.class.getName(), EntityPropertyType.class.getName(), NamingType.class.getName());
    iprint("    return %1$s.asList(%n", Arrays.class.getName());
    for (Iterator<EmbeddablePropertyMeta> it =
            embeddableMeta.getEmbeddablePropertyMetas().iterator();
        it.hasNext(); ) {
      EmbeddablePropertyMeta pm = it.next();
      EmbeddablePropertyCtTypeVisitor visitor = new EmbeddablePropertyCtTypeVisitor();
      pm.getCtType().accept(visitor, null);
      BasicCtType basicCtType = visitor.basicCtType;
      WrapperCtType wrapperCtType = visitor.wrapperCtType;
      DomainCtType domainCtType = visitor.domainCtType;

      String newWrapperExpr;
      if (basicCtType.isEnum()) {
        newWrapperExpr =
            String.format(
                "new %s(%s.class)", wrapperCtType.getTypeName(), basicCtType.getBoxedTypeName());
      } else {
        newWrapperExpr = String.format("new %s()", wrapperCtType.getTypeName());
      }
      String parentEntityPropertyType = "null";
      String parentEntityBoxedTypeName = Object.class.getName();
      String domainType = "null";
      String domainTypeName = "Object";
      if (domainCtType != null) {
        domainType = domainCtType.getInstantiationCommand();
        domainTypeName = domainCtType.getTypeName();
      }
      iprint(
          "        new %1$s<Object, ENTITY, %3$s, %16$s>(entityClass, %15$s.class, %3$s.class, () -> %9$s, null, %10$s, embeddedPropertyName + \".%4$s\", \"%5$s\", namingType, %6$s, %7$s, %17$s)",
          /* 1 */ DefaultPropertyType.class.getName(),
          /* 2 */ null,
          /* 3 */ basicCtType.getBoxedTypeName(),
          /* 4 */ pm.getName(),
          /* 5 */ pm.getColumnName(),
          /* 6 */ pm.isColumnInsertable(),
          /* 7 */ pm.isColumnUpdatable(),
          /* 8 */ null,
          /* 9 */ newWrapperExpr,
          /* 10 */ domainType,
          /* 11 */ pm.getBoxedTypeName(),
          /* 12 */ parentEntityPropertyType,
          /* 13 */ parentEntityBoxedTypeName,
          /* 14 */ null,
          /* 15 */ pm.getBoxedClassName(),
          /* 16 */ domainTypeName,
          /* 17 */ pm.isColumnQuoteRequired());
      print(it.hasNext() ? ",%n" : "");
    }
    print(");%n");
    iprint("}%n");
    print("%n");
  }

  private void printNewEmbeddableMethod() {
    iprint("@Override%n");
    iprint(
        "public <ENTITY> %1$s newEmbeddable(String embeddedPropertyName, %2$s<String, %3$s<ENTITY, ?>> __args) {%n",
        embeddableMeta.getEmbeddableElement().getQualifiedName(),
        Map.class.getName(),
        Property.class.getName());
    if (embeddableMeta.isAbstract()) {
      iprint("    return null;%n");
    } else {
      iprint("    return new %1$s(%n", embeddableMeta.getEmbeddableElement().getQualifiedName());
      for (Iterator<EmbeddablePropertyMeta> it =
              embeddableMeta.getEmbeddablePropertyMetas().iterator();
          it.hasNext(); ) {
        EmbeddablePropertyMeta propertyMeta = it.next();
        iprint(
            "        (%1$s)(__args.get(embeddedPropertyName + \".%2$s\") != null ? __args.get(embeddedPropertyName + \".%2$s\").get() : null)",
            ctx.getTypes().boxIfPrimitive(propertyMeta.getType()), propertyMeta.getName());
        if (it.hasNext()) {
          print(",%n");
        }
      }
      print(");%n");
    }
    iprint("}%n");
    print("%n");
  }

  private void printGetSingletonInternalMethod() {
    iprint("/**%n");
    iprint(" * @return the singleton%n");
    iprint(" */%n");
    iprint("public static %1$s getSingletonInternal() {%n", simpleName);
    iprint("    return __singleton;%n");
    iprint("}%n");
    print("%n");
  }

  protected class EmbeddablePropertyCtTypeVisitor
      extends SimpleCtTypeVisitor<Void, Void, RuntimeException> {

    private BasicCtType basicCtType;

    private WrapperCtType wrapperCtType;

    private DomainCtType domainCtType;

    @Override
    protected Void defaultAction(CtType ctType, Void p) throws RuntimeException {
      assertNotNull(basicCtType);
      assertNotNull(wrapperCtType);
      return null;
    }

    @Override
    public Void visitOptionalCtType(OptionalCtType ctType, Void p) throws RuntimeException {
      return ctType.getElementCtType().accept(this, p);
    }

    @Override
    public Void visitOptionalIntCtType(OptionalIntCtType ctType, Void p) throws RuntimeException {
      return ctType.getElementCtType().accept(this, p);
    }

    @Override
    public Void visitOptionalLongCtType(OptionalLongCtType ctType, Void p) throws RuntimeException {
      return ctType.getElementCtType().accept(this, p);
    }

    @Override
    public Void visitOptionalDoubleCtType(OptionalDoubleCtType ctType, Void p)
        throws RuntimeException {
      return ctType.getElementCtType().accept(this, p);
    }

    @Override
    public Void visitBasicCtType(BasicCtType basicCtType, Void p) throws RuntimeException {
      this.basicCtType = basicCtType;
      this.wrapperCtType = basicCtType.getWrapperCtType();
      return defaultAction(basicCtType, p);
    }

    @Override
    public Void visitDomainCtType(DomainCtType domainCtType, Void p) throws RuntimeException {
      this.domainCtType = domainCtType;
      return visitBasicCtType(domainCtType.getBasicCtType(), p);
    }
  }
}
