package org.seasar.doma.internal.apt.generator;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import javax.lang.model.element.TypeParameterElement;
import org.seasar.doma.internal.apt.Context;
import org.seasar.doma.internal.apt.meta.domain.DomainMeta;
import org.seasar.doma.internal.apt.util.MetaUtil;
import org.seasar.doma.internal.util.BoxedPrimitiveUtil;
import org.seasar.doma.jdbc.domain.AbstractDomainType;

public class DomainTypeGenerator extends AbstractGenerator {

  private final DomainMeta domainMeta;

  private final String typeName;

  private final String simpleMetaClassName;

  private final String typeParamDecl;

  public DomainTypeGenerator(
      Context ctx, ClassName className, Printer printer, DomainMeta domainMeta) {
    super(ctx, className, printer);
    assertNotNull(domainMeta);
    this.domainMeta = domainMeta;
    this.typeName = ctx.getTypes().getTypeName(domainMeta.getType());
    this.simpleMetaClassName = MetaUtil.toSimpleMetaName(domainMeta.getTypeElement(), ctx);
    this.typeParamDecl = makeTypeParamDecl(typeName);
  }

  private String makeTypeParamDecl(String typeName) {
    int pos = typeName.indexOf("<");
    if (pos == -1) {
      return "";
    }
    return typeName.substring(pos);
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
    if (domainMeta.getTypeElement().getTypeParameters().isEmpty()) {
      iprint("/** */%n");
    } else {
      iprint("/**%n");
      for (TypeParameterElement typeParam : domainMeta.getTypeElement().getTypeParameters()) {
        iprint(" * @param <%1$s> %1$s%n", typeParam.getSimpleName());
      }
      iprint(" */%n");
    }
    printGenerated();
    iprint(
        "public final class %1$s%5$s extends %2$s<%3$s, %4$s> {%n",
        simpleMetaClassName,
        AbstractDomainType.class.getName(),
        ctx.getTypes().boxIfPrimitive(domainMeta.getValueType()),
        typeName,
        typeParamDecl);
    print("%n");
    indent();
    printValidateVersionStaticInitializer();
    printFields();
    printConstructors();
    printMethods();
    unindent();
    unindent();
    iprint("}%n");
  }

  private void printFields() {
    if (domainMeta.isParameterized()) {
      iprint("@SuppressWarnings(\"rawtypes\")%n");
    }
    iprint("private static final %1$s singleton = new %1$s();%n", simpleName);
    print("%n");
  }

  private void printConstructors() {
    iprint("private %1$s() {%n", simpleName);
    if (domainMeta.getBasicCtType().isEnum()) {
      iprint(
          "    super(() -> new %1$s(%2$s.class));%n",
          domainMeta.getWrapperCtType().getTypeName(),
          ctx.getTypes().boxIfPrimitive(domainMeta.getValueType()));
    } else {
      iprint("    super(() -> new %1$s());%n", domainMeta.getWrapperCtType().getTypeName());
    }
    iprint("}%n");
    print("%n");
  }

  private void printMethods() {
    printNewDomainMethod();
    printGetBasicValueMethod();
    printGetBasicClassMethod();
    printGetDomainClassMethod();
    printGetSingletonInternalMethod();
  }

  private void printNewDomainMethod() {
    boolean primitive = domainMeta.getBasicCtType().isPrimitive();
    iprint("@Override%n");
    iprint(
        "protected %1$s newDomain(%2$s value) {%n",
        typeName, ctx.getTypes().boxIfPrimitive(domainMeta.getValueType()));
    if (!primitive && !domainMeta.getAcceptNull()) {
      iprint("    if (value == null) {%n");
      iprint("        return null;%n");
      iprint("    }%n");
    }
    if (domainMeta.providesConstructor()) {
      if (primitive) {
        iprint(
            "    return new %1$s(%2$s.unbox(value));%n",
            /* 1 */ typeName, BoxedPrimitiveUtil.class.getName());
      } else {
        iprint("    return new %1$s(value);%n", /* 1 */ typeName);
      }
    } else {
      if (primitive) {
        iprint(
            "    return %1$s.%2$s(%3$s.unbox(value));%n",
            /* 1 */ domainMeta.getTypeElement().getQualifiedName(),
            /* 2 */ domainMeta.getFactoryMethod(),
            /* 3 */ BoxedPrimitiveUtil.class.getName());
      } else {
        iprint(
            "    return %1$s.%2$s(value);%n",
            /* 1 */ domainMeta.getTypeElement().getQualifiedName(),
            /* 2 */ domainMeta.getFactoryMethod());
      }
    }
    iprint("}%n");
    print("%n");
  }

  private void printGetBasicValueMethod() {
    iprint("@Override%n");
    iprint(
        "protected %1$s getBasicValue(%2$s domain) {%n",
        ctx.getTypes().boxIfPrimitive(domainMeta.getValueType()), typeName);
    iprint("    if (domain == null) {%n");
    iprint("        return null;%n");
    iprint("    }%n");
    iprint("    return domain.%1$s();%n", domainMeta.getAccessorMethod());
    iprint("}%n");
    print("%n");
  }

  private void printGetBasicClassMethod() {
    iprint("@Override%n");
    iprint("public Class<?> getBasicClass() {%n");
    iprint("    return %1$s.class;%n", domainMeta.getValueType());
    iprint("}%n");
    print("%n");
  }

  private void printGetDomainClassMethod() {
    if (domainMeta.isParameterized()) {
      iprint("@SuppressWarnings(\"unchecked\")%n");
    }
    iprint("@Override%n");
    iprint("public Class<%1$s> getDomainClass() {%n", typeName);
    if (domainMeta.isParameterized()) {
      iprint("    Class<?> clazz = %1$s.class;%n", domainMeta.getTypeElement().getQualifiedName());
      iprint("    return (Class<%1$s>) clazz;%n", typeName);
    } else {
      iprint("    return %1$s.class;%n", domainMeta.getTypeElement().getQualifiedName());
    }
    iprint("}%n");
    print("%n");
  }

  private void printGetSingletonInternalMethod() {
    iprint("/**%n");
    iprint(" * @return the singleton%n");
    iprint(" */%n");
    if (domainMeta.isParameterized()) {
      iprint("@SuppressWarnings(\"unchecked\")%n");
      iprint(
          "public static %1$s %2$s%1$s getSingletonInternal() {%n",
          typeParamDecl, simpleMetaClassName);
      iprint("    return (%2$s%1$s) singleton;%n", typeParamDecl, simpleMetaClassName);
    } else {
      iprint("public static %1$s getSingletonInternal() {%n", simpleMetaClassName);
      iprint("    return singleton;%n");
    }
    iprint("}%n");
    print("%n");
  }
}
