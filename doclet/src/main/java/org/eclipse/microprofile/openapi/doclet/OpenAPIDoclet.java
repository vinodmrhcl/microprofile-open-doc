/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.eclipse.microprofile.openapi.doclet;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.bind.Jsonb;

import org.eclipse.microprofile.openapi.models.Constructible;
import org.eclipse.microprofile.openapi.spi.OASFactoryResolver;
import org.eclipse.yasson.internal.JsonBindingBuilder;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationTypeDoc;
import com.sun.javadoc.AnnotationTypeElementDoc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Doclet;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

import io.smallrye.openapi.spi.OASFactoryResolverImpl;

public class OpenAPIDoclet extends Doclet {

	private static final PropertyUtils PROPERTY_UTILS = new PropertyUtils();
	private static final OASFactoryResolver RESOLVER = new OASFactoryResolverImpl();
	private static final Jsonb JSONB = new JsonBindingBuilder().build();

	// private static PrintWriter WRITER = new PrintWriter(new FileWriter(new File("openapi.json")));
	private static final PrintWriter WRITER = new PrintWriter(System.out);

	private static final String ORG_ECLIPSE_MICROPROFILE_OPENAPI_ANNOTATIONS = "org.eclipse.microprofile.openapi.annotations";
	// private static final String O_E_M_O_A = "o.e.m.o.a.";
	// private static final String O_E_M_O_A = "";

	private static List<String> PACKAGES_TO_SCAN = new ArrayList<>();

	private static Map<String, Constructible> MODELS = new LinkedHashMap<>();

	private static Map<String, Class<? extends Constructible>> MODEL_TYPES = new HashMap<>();
	private static Map<String, Class<? extends Enum<?>>> ENUM_TYPES = new HashMap<>();

	private static List<String> DEPRECATED_KEYS = new ArrayList<>();
	private static ModelUpdator DEFAULT_MODEL_UPDATOR = new ReflectionModelUpdator();
	private static Map<String, ModelUpdator> MODEL_UPDATORS = new HashMap<>();

	static {

		PACKAGES_TO_SCAN.add("org.eclipse.microprofile.openapi.apps");

		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition.class.getSimpleName(), org.eclipse.microprofile.openapi.models.OpenAPI.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getSimpleName(), org.eclipse.microprofile.openapi.models.media.Schema.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.security.SecurityScheme.class.getSimpleName(), org.eclipse.microprofile.openapi.models.security.SecurityScheme.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement.class.getSimpleName(), org.eclipse.microprofile.openapi.models.security.SecurityRequirement.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.Components.class.getSimpleName(), org.eclipse.microprofile.openapi.models.Components.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.headers.Header.class.getSimpleName(), org.eclipse.microprofile.openapi.models.headers.Header.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.servers.Server.class.getSimpleName(), org.eclipse.microprofile.openapi.models.servers.Server.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.callbacks.Callback.class.getSimpleName(), org.eclipse.microprofile.openapi.models.callbacks.Callback.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.info.License.class.getSimpleName(), org.eclipse.microprofile.openapi.models.info.License.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.ExternalDocumentation.class.getSimpleName(), org.eclipse.microprofile.openapi.models.ExternalDocumentation.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.info.Info.class.getSimpleName(), org.eclipse.microprofile.openapi.models.info.Info.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.media.ExampleObject.class.getSimpleName(), org.eclipse.microprofile.openapi.models.examples.Example.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class.getSimpleName(), org.eclipse.microprofile.openapi.models.parameters.Parameter.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody.class.getSimpleName(), org.eclipse.microprofile.openapi.models.parameters.RequestBody.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.info.Contact.class.getSimpleName(), org.eclipse.microprofile.openapi.models.info.Contact.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.links.Link.class.getSimpleName(), org.eclipse.microprofile.openapi.models.links.Link.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation.class.getSimpleName(), org.eclipse.microprofile.openapi.models.callbacks.Callback.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.tags.Tag.class.getSimpleName(), org.eclipse.microprofile.openapi.models.tags.Tag.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.media.Content.class.getSimpleName(), org.eclipse.microprofile.openapi.models.media.Content.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.responses.APIResponse.class.getSimpleName(), org.eclipse.microprofile.openapi.models.responses.APIResponse.class);
		MODEL_TYPES.put(org.eclipse.microprofile.openapi.annotations.servers.ServerVariable.class.getSimpleName(), org.eclipse.microprofile.openapi.models.servers.ServerVariable.class);

		ENUM_TYPES.put(org.eclipse.microprofile.openapi.annotations.enums.SchemaType.class.getSimpleName(), org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.class);
		ENUM_TYPES.put(org.eclipse.microprofile.openapi.annotations.enums.ParameterIn.class.getSimpleName(), org.eclipse.microprofile.openapi.models.parameters.Parameter.In.class);
		ENUM_TYPES.put(org.eclipse.microprofile.openapi.annotations.enums.ParameterStyle.class.getSimpleName(), org.eclipse.microprofile.openapi.models.parameters.Parameter.Style.class);
		ENUM_TYPES.put(org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn.class.getSimpleName(), org.eclipse.microprofile.openapi.models.security.SecurityScheme.In.class);
		ENUM_TYPES.put(org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType.class.getSimpleName(), org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type.class);

		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.servers.ServerVariable.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.media.Schema.class.getSimpleName() + ".implementation");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.responses.APIResponse.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.responses.APIResponse.class.getSimpleName() + ".responseCode");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.media.ExampleObject.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.parameters.RequestBody.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.headers.Header.class.getSimpleName() + ".name");
		DEPRECATED_KEYS.add(org.eclipse.microprofile.openapi.annotations.links.Link.class.getSimpleName() + ".name");

		MODEL_UPDATORS.put(org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement.class.getSimpleName(), new SecurityRequirementMU());
		MODEL_UPDATORS.put(org.eclipse.microprofile.openapi.annotations.parameters.Parameter.class.getSimpleName(), new ParameterMU());
		MODEL_UPDATORS.put(org.eclipse.microprofile.openapi.annotations.media.Content.class.getSimpleName(), new ContentMU());
		MODEL_UPDATORS.put(org.eclipse.microprofile.openapi.annotations.security.SecurityScheme.class.getSimpleName(), new SecuritySchemeMU());
		MODEL_UPDATORS.put(org.eclipse.microprofile.openapi.annotations.links.Link.class.getSimpleName(), new LinkMU());
	}

	private interface ModelUpdator {
		Constructible updateModel(Constructible model, String key, Object value) throws Exception;
	}

	private static class ReflectionModelUpdator implements ModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			Property property = PROPERTY_UTILS.getProperty(model.getClass(), key);
			property.set(model, value);
			return model;
		}
	}

	private static class SecurityRequirementMU implements ModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			org.eclipse.microprofile.openapi.models.security.SecurityRequirement sr = (org.eclipse.microprofile.openapi.models.security.SecurityRequirement) model;
			sr.addScheme(value.toString());
			return model;
		}
	}

	private static class ContentMU implements ModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			org.eclipse.microprofile.openapi.models.media.Content c = (org.eclipse.microprofile.openapi.models.media.Content) model;
			if (key.equals("mediaType")) {

				org.eclipse.microprofile.openapi.models.media.Encoding enc = RESOLVER.createObject(org.eclipse.microprofile.openapi.models.media.Encoding.class);
				enc.setContentType(value.toString());

				org.eclipse.microprofile.openapi.models.media.MediaType mt = RESOLVER.createObject(org.eclipse.microprofile.openapi.models.media.MediaType.class);
				mt.addEncoding("", enc);

				c.addMediaType("", mt);
			}
			return model;
		}
	}

	private static class ParameterMU extends ReflectionModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			org.eclipse.microprofile.openapi.models.parameters.Parameter param = (org.eclipse.microprofile.openapi.models.parameters.Parameter) model;
			if (key.equals("exlpode")) {
				org.eclipse.microprofile.openapi.annotations.enums.Explode explode = (org.eclipse.microprofile.openapi.annotations.enums.Explode) value;
				if (explode == org.eclipse.microprofile.openapi.annotations.enums.Explode.TRUE) {
					param.setExplode(true);
				} else if (explode == org.eclipse.microprofile.openapi.annotations.enums.Explode.FALSE) {
					param.setExplode(false);
				}
			} else {
				super.updateModel(model, key, value);
			}
			return model;
		}
	}

	private static class SecuritySchemeMU extends ReflectionModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			org.eclipse.microprofile.openapi.models.security.SecurityScheme ss = (org.eclipse.microprofile.openapi.models.security.SecurityScheme) model;
			if (key.equals("securitySchemeName")) {
				ss.setName(value.toString());
			} else {
				super.updateModel(model, key, value);
			}
			return model;
		}
	}
	
	private static class LinkMU extends ReflectionModelUpdator {

		@Override
		public Constructible updateModel(Constructible model, String key, Object value) throws Exception {
			org.eclipse.microprofile.openapi.models.links.Link link = (org.eclipse.microprofile.openapi.models.links.Link) model;
			if (key.equals("parameters")) {
				link.setParameters(null);
			} else {
				super.updateModel(model, key, value);
			}
			return model;
		}
	}

	private static void log(String base, Object data) {
		WRITER.println(base + " : " + data);
		WRITER.flush();
	}

	private static void printModel(Object key, Object value) {
		if (value != null) {
			value = JSONB.toJson(value);
		}
		WRITER.println(key + " : " + value);
	}

	private static void error(String key, Object value) {
		error(null, key, value);
	}

	private static void error(Throwable e, String key, Object value) {
		log(key, value + "----" + (e != null ? e.getMessage() : ""));
	}

	public static boolean start(RootDoc root) {

		try {
			ClassDoc[] classes = root.classes();
			for (int i = 0; i < classes.length; ++i) {
				ClassDoc clazz = classes[i];
				printClass(clazz);
			}

			WRITER.println("");
			MODELS.entrySet().stream().forEach(e -> {
				String annotationTypeName = e.getKey();
				Constructible model = e.getValue();
				if (model != null) {
					printModel(annotationTypeName, model);
				}
			});

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			WRITER.close();
		}

		return true;
	}

	private static Constructible buildModel(String annotationTypeName) {
		Class<? extends Constructible> modelType = MODEL_TYPES.get(annotationTypeName);
		if (modelType != null) {
			Constructible model = RESOLVER.createObject(modelType);
			if (model != null) {
				return model;
			} else {
				error(annotationTypeName, modelType);
			}
		} else {
			error(annotationTypeName, null);
		}
		return null;
	}

	private static void printClass(ClassDoc clazz) throws Exception {
		if (PACKAGES_TO_SCAN.stream().anyMatch(p -> clazz.containingPackage().name().startsWith(p))) {
			log("Class", clazz.name());

			AnnotationDesc[] annotations = clazz.annotations();
			printAnnotations(clazz.name(), annotations);
		}
	}

	private static void printAnnotations(String base, AnnotationDesc[] annotations) throws Exception {
		for (int j = 0; j < annotations.length; ++j) {
			AnnotationDesc annotation = annotations[j];

			AnnotationTypeDoc annotationType = annotation.annotationType();
			if (annotationType.containingPackage().name().startsWith(ORG_ECLIPSE_MICROPROFILE_OPENAPI_ANNOTATIONS)) {
				// String typeName = decorateQName(annotationType.qualifiedName(), annotationType.typeName());
				String typeName = annotationType.typeName();
				// log(base + ANNOTATIONS + "[" + j + "]" + ANNOTATION_TYPE, typeName);
				// printAnnotation(base + ANNOTATIONS + "[" + j + "]", annotation);
				printAnnotation(base + "." + typeName, annotation);
			}

		}
	}

	private static void printAnnotation(String base, AnnotationDesc annotation) throws Exception {
		String annotationTypeName = annotation.annotationType().typeName();
		Constructible model = buildModel(annotationTypeName);
		MODELS.put(base, model);
		// log2(base, model);
		printEVPs(annotationTypeName, model, base, annotation.elementValues());
	}

	// private static String decorateQName(String qualifiedName, String typeName) {
	// if (qualifiedName.startsWith(ORG_ECLIPSE_MICROPROFILE_OPENAPI_ANNOTATIONS)) {
	// qualifiedName = O_E_M_O_A + typeName;
	// }
	// return qualifiedName;
	// }

	private static void printEVPs(String annotationTypeName, Constructible model, String base, ElementValuePair[] evps) throws Exception {
		for (int k = 0; k < evps.length; k++) {
			ElementValuePair evp = evps[k];
			AnnotationTypeElementDoc element = evp.element();
			AnnotationValue annotationValue = evp.value();
			// printValue(base + EVP + "[" + k + "]", element, annotationValue, true);
			updateValue(annotationTypeName, model, base + "." + element.name(), element, annotationValue, true);
		}
	}

	private static void updateValue(String annotationTypeName, Constructible model, String base, AnnotationTypeElementDoc element, AnnotationValue annotationValue, boolean isEVP) throws Exception {
		Object value_value = annotationValue.value();
		if (isCompound(value_value)) {
			if (isEVP) {
				// log(base, element.name() + " - " + element.returnType().typeName());
			}
			printCompoundValue(annotationTypeName, model, base, element, value_value);
		} else {

			String name = element.name();
			if (DEPRECATED_KEYS.contains(annotationTypeName + "." + name)) {
				return;
			}

			Object value = null;
			try {
				value = getValue(base, value_value);
				ModelUpdator mu = MODEL_UPDATORS.get(annotationTypeName);
				if (mu == null) {
					mu = DEFAULT_MODEL_UPDATOR;
				}
				mu.updateModel(model, name, value);
				// log(base, value);
			} catch (Throwable e) {
				error(e, base, value);
			}
		}
	}

	private static boolean isCompound(Object value_value) {
		return value_value instanceof AnnotationDesc || //
				(value_value instanceof AnnotationValue[] && //
						(((AnnotationValue[]) value_value).length == 0 || !(((AnnotationValue[]) value_value)[0].value() instanceof String)));
	}

	private static Object getValue(String base, Object value_value) {
		try {
			if (value_value instanceof String || value_value instanceof Boolean || value_value instanceof Integer || value_value instanceof Class) {
				return value_value;
				// log(base + "." + VALUE, value_value);
			} else if (value_value instanceof Type) {
				Type type = (Type) value_value;
				return Class.forName(type.qualifiedTypeName());
				// log(base + "." + VALUE, type.typeName());
			} else if (value_value instanceof FieldDoc) {
				FieldDoc field = (FieldDoc) value_value;
				Type type = field.type();
				Class enumType = ENUM_TYPES.get(type.typeName());
				Enum enumValue = Enum.valueOf(enumType, field.name());
				return enumValue;
				// String fieldName = type.typeName() + "." + field.name();
				// log(base + "." + VALUE + ".field", fieldName);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static void printCompoundValue(String annotationTypeName, Constructible model, String base, AnnotationTypeElementDoc element, Object value_value) throws Exception {
		if (value_value instanceof AnnotationDesc) {
			AnnotationDesc annotation = (AnnotationDesc) value_value;
			printAnnotation(base, annotation);
		} else if (value_value instanceof AnnotationValue[]) {
			AnnotationValue[] annotationValues_Value = (AnnotationValue[]) value_value;
			for (int l = 0; l < annotationValues_Value.length; l++) {
				AnnotationValue annotatedValue_Value = annotationValues_Value[l];
				updateValue(annotationTypeName, model, base + "[" + l + "]", element, annotatedValue_Value, false);
			}
		}

	}

}