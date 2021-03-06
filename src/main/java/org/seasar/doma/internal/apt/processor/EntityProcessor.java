package org.seasar.doma.internal.apt.processor;

import static org.seasar.doma.internal.util.AssertionUtil.assertNotNull;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.TypeElement;
import org.seasar.doma.Entity;
import org.seasar.doma.internal.apt.Options;
import org.seasar.doma.internal.apt.generator.ClassName;
import org.seasar.doma.internal.apt.generator.EntityTypeGenerator;
import org.seasar.doma.internal.apt.generator.Generator;
import org.seasar.doma.internal.apt.generator.Printer;
import org.seasar.doma.internal.apt.meta.entity.EntityMeta;
import org.seasar.doma.internal.apt.meta.entity.EntityMetaFactory;

@SupportedAnnotationTypes({"org.seasar.doma.Entity"})
@SupportedOptions({
  Options.ENTITY_FIELD_PREFIX,
  Options.DOMAIN_CONVERTERS,
  Options.VERSION_VALIDATION,
  Options.RESOURCES_DIR,
  Options.LOMBOK_VALUE,
  Options.LOMBOK_ALL_ARGS_CONSTRUCTOR,
  Options.TEST,
  Options.DEBUG
})
public class EntityProcessor extends AbstractGeneratingProcessor<EntityMeta> {

  public EntityProcessor() {
    super(Entity.class);
  }

  @Override
  protected EntityMetaFactory createTypeElementMetaFactory() {
    return new EntityMetaFactory(ctx);
  }

  @Override
  protected ClassName createNameSpec(TypeElement typeElement, EntityMeta meta) {
    assertNotNull(typeElement, meta);
    return ctx.getClassNames().newEntityMetaTypeClassName(typeElement);
  }

  @Override
  protected Generator createGenerator(ClassName className, Printer printer, EntityMeta meta) {
    assertNotNull(className, meta, printer);
    return new EntityTypeGenerator(ctx, className, printer, meta);
  }
}
