package io.crnk.jpa;

import io.crnk.core.engine.information.resource.ResourceField;
import io.crnk.core.engine.information.resource.ResourceInformation;
import io.crnk.core.engine.parser.TypeParser;
import io.crnk.jpa.internal.JpaResourceInformationBuilder;
import io.crnk.jpa.merge.MergedResource;
import io.crnk.jpa.meta.JpaMetaProvider;
import io.crnk.jpa.model.*;
import io.crnk.jpa.util.ResourceFieldComparator;
import io.crnk.legacy.registry.DefaultResourceInformationBuilderContext;
import io.crnk.meta.MetaLookup;
import io.crnk.meta.model.MetaAttribute;
import io.crnk.meta.model.MetaDataObject;
import io.crnk.meta.provider.resource.ResourceMetaProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JpaResourceInformationBuilderTest {

	private JpaResourceInformationBuilder builder;
	private MetaLookup lookup;

	@Before
	public void setup() {
		lookup = new MetaLookup();
		lookup.addProvider(new JpaMetaProvider());
		lookup.addProvider(new ResourceMetaProvider(false));
		builder = new JpaResourceInformationBuilder(lookup);
		builder.init(new DefaultResourceInformationBuilderContext(builder, new TypeParser()));
	}

	@Test
	public void test() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		ResourceInformation info = builder.build(TestEntity.class);
		ResourceField idField = info.getIdField();
		assertNotNull(idField);
		assertEquals("id", idField.getJsonName());
		assertEquals("id", idField.getUnderlyingName());
		assertEquals(Long.class, idField.getType());
		assertEquals(Long.class, idField.getGenericType());

		List<ResourceField> attrFields = new ArrayList<ResourceField>(info.getAttributeFields().getFields());
		Collections.sort(attrFields, ResourceFieldComparator.INSTANCE);
		assertEquals(5, attrFields.size());
		ResourceField embField = attrFields.get(1);
		assertEquals(TestEntity.ATTR_embValue, embField.getJsonName());
		assertEquals(TestEntity.ATTR_embValue, embField.getUnderlyingName());
		assertEquals(TestEmbeddable.class, embField.getType());
		assertEquals(TestEmbeddable.class, embField.getGenericType());
		Assert.assertTrue(embField.getAccess().isPostable());
		Assert.assertTrue(embField.getAccess().isPatchable());
		Assert.assertTrue(embField.getAccess().isSortable());
		Assert.assertTrue(embField.getAccess().isFilterable());

		ArrayList<ResourceField> relFields = new ArrayList<ResourceField>(info.getRelationshipFields());
		Collections.sort(relFields, ResourceFieldComparator.INSTANCE);
		assertEquals(4, relFields.size());
		boolean found = false;
		for (ResourceField relField : relFields) {
			if (relField.getUnderlyingName().equals(TestEntity.ATTR_oneRelatedValue)) {
				assertEquals(TestEntity.ATTR_oneRelatedValue, relField.getJsonName());
				assertEquals(RelatedEntity.class, relField.getType());
				assertEquals(RelatedEntity.class, relField.getGenericType());
				found = true;
			}
		}
		Assert.assertTrue(found);
	}

	@Test
	public void testPrimitiveTypesProperlyRecognized() {
		ResourceInformation info = builder.build(TestEntity.class);
		ResourceField field = info.findAttributeFieldByName("longValue");
		Assert.assertNotNull(field);
		Assert.assertEquals(long.class, field.getType());
		Assert.assertEquals(long.class, field.getGenericType());
	}

	@Test
	public void testIdAccess() {
		ResourceInformation info = builder.build(TestEntity.class);
		ResourceField idField = info.getIdField();
		Assert.assertTrue(idField.getAccess().isPostable());
		Assert.assertFalse(idField.getAccess().isPatchable());
		Assert.assertTrue(idField.getAccess().isSortable());
		Assert.assertTrue(idField.getAccess().isFilterable());
	}

	@Test
	public void testStringAttributeAccess() {
		ResourceInformation info = builder.build(TestEntity.class);
		ResourceField field = info.findAttributeFieldByName("stringValue");
		Assert.assertTrue(field.getAccess().isPostable());
		Assert.assertTrue(field.getAccess().isPatchable());
		Assert.assertTrue(field.getAccess().isSortable());
		Assert.assertTrue(field.getAccess().isFilterable());
	}

	@Test
	public void testLongAttributeAccess() {
		ResourceInformation info = builder.build(VersionedEntity.class);
		ResourceField field = info.findAttributeFieldByName("longValue");
		Assert.assertTrue(field.getAccess().isPostable());
		Assert.assertTrue(field.getAccess().isPatchable());
	}

	@Test
	public void testVersionAccess() {
		ResourceInformation info = builder.build(VersionedEntity.class);
		ResourceField field = info.findAttributeFieldByName("version");
		// must not be immutable to support optimistic locking
		Assert.assertTrue(field.getAccess().isPostable());
		Assert.assertTrue(field.getAccess().isPatchable());
	}

	@Test
	public void testAttributeAnnotations() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		ResourceInformation info = builder.build(AnnotationTestEntity.class);

		ResourceField lobField = info.findAttributeFieldByName("lobValue");
		ResourceField fieldAnnotatedField = info.findAttributeFieldByName("fieldAnnotatedValue");
		ResourceField columnAnnotatedField = info.findAttributeFieldByName("columnAnnotatedValue");

		Assert.assertFalse(lobField.getAccess().isSortable());
		Assert.assertFalse(lobField.getAccess().isFilterable());
		Assert.assertTrue(lobField.getAccess().isPostable());
		Assert.assertTrue(lobField.getAccess().isPatchable());

		Assert.assertFalse(fieldAnnotatedField.getAccess().isSortable());
		Assert.assertFalse(fieldAnnotatedField.getAccess().isFilterable());
		Assert.assertTrue(fieldAnnotatedField.getAccess().isPostable());
		Assert.assertFalse(fieldAnnotatedField.getAccess().isPatchable());

		Assert.assertTrue(columnAnnotatedField.getAccess().isSortable());
		Assert.assertTrue(columnAnnotatedField.getAccess().isFilterable());
		Assert.assertFalse(columnAnnotatedField.getAccess().isPostable());
		Assert.assertTrue(columnAnnotatedField.getAccess().isPatchable());

		MetaDataObject meta = lookup.getMeta(AnnotationTestEntity.class).asDataObject();
		Assert.assertTrue(meta.getAttribute("lobValue").isLob());
		Assert.assertFalse(meta.getAttribute("fieldAnnotatedValue").isLob());
	}

	@Test
	public void testRenamedResourceType() {
		ResourceInformation info = builder.build(RenamedTestEntity.class);
		Assert.assertEquals("renamedResource", info.getResourceType());
	}

	@Test
	public void testReadOnlyField() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		ResourceInformation info = builder.build(AnnotationTestEntity.class);

		ResourceField field = info.findAttributeFieldByName("readOnlyValue");

		Assert.assertFalse(field.getAccess().isPostable());
		Assert.assertFalse(field.getAccess().isPatchable());

		MetaDataObject meta = lookup.getMeta(AnnotationTestEntity.class).asDataObject();
		MetaAttribute attribute = meta.getAttribute("readOnlyValue");

		Assert.assertFalse(attribute.isInsertable());
		Assert.assertFalse(attribute.isUpdatable());
	}

	@Test
	@Ignore
	public void mergeRelationsAnnotation() {
		Assert.assertTrue(builder.accept(MergedResource.class));

		ResourceInformation info = builder.build(MergedResource.class);
		Assert.assertEquals("merged", info.getResourceType());
		Assert.assertEquals(MergedResource.class, info.getResourceClass());
		Assert.assertNull(info.findRelationshipFieldByName("oneRelatedValue"));
		Assert.assertNull(info.findRelationshipFieldByName("manyRelatedValues"));
		Assert.assertNotNull(info.findAttributeFieldByName("oneRelatedValue"));
		Assert.assertNotNull(info.findAttributeFieldByName("manyRelatedValues"));
	}

	@Test
	public void testMappedSuperclass() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		ResourceInformation info = builder.build(AnnotationMappedSuperclassEntity.class);

		Assert.assertNull(info.getResourceType());

		ResourceField lobField = info.findAttributeFieldByName("lobValue");
		ResourceField fieldAnnotatedField = info.findAttributeFieldByName("fieldAnnotatedValue");
		ResourceField columnAnnotatedField = info.findAttributeFieldByName("columnAnnotatedValue");

		Assert.assertFalse(lobField.getAccess().isSortable());
		Assert.assertFalse(lobField.getAccess().isFilterable());
		Assert.assertTrue(lobField.getAccess().isPostable());
		Assert.assertTrue(lobField.getAccess().isPatchable());

		Assert.assertFalse(fieldAnnotatedField.getAccess().isSortable());
		Assert.assertFalse(fieldAnnotatedField.getAccess().isFilterable());
		Assert.assertTrue(fieldAnnotatedField.getAccess().isPostable());
		Assert.assertFalse(fieldAnnotatedField.getAccess().isPatchable());

		Assert.assertTrue(columnAnnotatedField.getAccess().isSortable());
		Assert.assertTrue(columnAnnotatedField.getAccess().isFilterable());
		Assert.assertFalse(columnAnnotatedField.getAccess().isPostable());
		Assert.assertTrue(columnAnnotatedField.getAccess().isPatchable());

		MetaDataObject meta = lookup.getMeta(AnnotationMappedSuperclassEntity.class).asDataObject();
		Assert.assertTrue(meta.getAttribute("lobValue").isLob());
		Assert.assertFalse(meta.getAttribute("fieldAnnotatedValue").isLob());
	}

}
