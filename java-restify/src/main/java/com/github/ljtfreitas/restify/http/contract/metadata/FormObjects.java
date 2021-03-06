/*******************************************************************************
 *
 * MIT License
 *
 * Copyright (c) 2016 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *******************************************************************************/
package com.github.ljtfreitas.restify.http.contract.metadata;

import static com.github.ljtfreitas.restify.http.util.Preconditions.isFalse;
import static com.github.ljtfreitas.restify.http.util.Preconditions.isTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.github.ljtfreitas.restify.http.contract.Form;
import com.github.ljtfreitas.restify.http.contract.Parameters;
import com.github.ljtfreitas.restify.http.contract.Form.Field;

public class FormObjects {

	private static final FormObjects singleton = new FormObjects();

	private final Map<Class<?>, FormObject> cache = new ConcurrentHashMap<>();

	private FormObjects() {
	}

	public FormObject of(Class<?> formObjectType) {
		return get(formObjectType).orElseGet(() -> create(formObjectType));
	}

	private FormObject create(Class<?> formObjectType) {
		FormObject formObjectMetadata = new FormObject(formObjectType);

		Arrays.stream(formObjectType.getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(Field.class))
			.forEach(field -> {
				Field annotation = field.getAnnotation(Field.class);

				String name = annotation.value().isEmpty() ? field.getName() : annotation.value();
				boolean indexed = annotation.indexed();

				formObjectMetadata.put(name, indexed, field);
		});

		cache.put(formObjectType, formObjectMetadata);

		return formObjectMetadata;
	}

	private Optional<FormObject> get(Class<?> formObjectType) {
		return Optional.ofNullable(cache.get(formObjectType));
	}

	public static FormObjects cache() {
		return singleton;
	}

	public static class FormObject {

		private final Class<?> type;
		private final String name;
		private final Map<String, FormObjectField> fields = new LinkedHashMap<>();

		private FormObject(Class<?> type) {
			isTrue(type.isAnnotationPresent(Form.class), "Your form class type must be annotated with @Form.");
			this.type = type;
			this.name = Optional.ofNullable(type.getAnnotation(Form.class).value())
					.filter(value -> !value.isEmpty())
						.map(value -> value.endsWith(".") ? value : value + ".")
							.orElse("");
		}

		public Optional<FormObjectField> fieldBy(String name) {
			return Optional.ofNullable(fields.get(name));
		}

		public String serialize(Object source) {
			isTrue(type.isInstance(source), "This FormObject can only serialize objects of class type [" + type + "]");
			return serializeAsParameters(source, name).queryString();
		}

		private Parameters serializeAsParameters(Object source, String name) {
			String prefix = name.isEmpty() || name.endsWith(".") ? name : name + ".";

			Parameters parameters = new Parameters(prefix);

			fields.values().forEach(field -> {
				try {
					field.serialize(source, parameters);
				} catch (Exception e) {
					throw new UnsupportedOperationException(e);
				}
			});

			return parameters;
		}

		private void put(String name, boolean indexed, java.lang.reflect.Field field) {
			isFalse(fields.containsKey(name), "Duplicate field [" + name + " on @Form object: " + type);
			fields.put(name, new FormObjectField(name, indexed, field));
		}

		public class FormObjectField {

			private final String name;
			private final boolean indexed;
			private final java.lang.reflect.Field field;

			FormObjectField(String name, boolean indexed, java.lang.reflect.Field field) {
				this.name = name;
				this.indexed = indexed;
				this.field = field;
			}

			private void serialize(Object source, Parameters parameters) throws Exception {
				field.setAccessible(true);

				Object value = field.get(source);

				apply(name, value, parameters);
			}

			private void apply(String name, Object value, Parameters parameters) throws Exception {
				if (value != null) {
					if (value.getClass().isAnnotationPresent(Form.class)) {
						serializeNested(name, value, parameters);

					} else if (Iterable.class.isAssignableFrom(value.getClass())) {
						serializeIterable(name, value, parameters);

					} else {
						parameters.put(name, value.toString());
					}
				}
			}

			@SuppressWarnings("rawtypes")
			private void serializeIterable(String name, Object value, Parameters parameters) throws Exception {
				Iterable iterable = (Iterable) value;

				int position = 0;

				for (Object element : iterable) {
					String newName = indexed ? name + "[" + position + "]" : name;

					apply(newName, element, parameters);

					position++;
				}
			}

			private void serializeNested(String name, Object value, Parameters parameters) {
				FormObject formObject = FormObjects.cache().of(value.getClass());

				String appendedName = name + (formObject.name.isEmpty() ? "" : "." + formObject.name);

				Parameters nestedParameters = formObject.serializeAsParameters(value, appendedName);

				parameters.putAll(nestedParameters);
			}

			public void applyTo(Object object, Object value) {
				try {
					field.setAccessible(true);
					field.set(object, valueOf(value));

				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new UnsupportedOperationException(e);
				}
			}

			private Object valueOf(Object value) {
				Class<?> fieldType = field.getType();

				if (fieldType.isInstance(value)) {
					return value;

				} else if (fieldType == Byte.class || fieldType == byte.class) {
					return Byte.valueOf(value.toString());

				} else if (fieldType == Short.class || fieldType == short.class) {
					return Short.valueOf(value.toString());

				} else if (fieldType == Integer.class || fieldType == int.class) {
					return Integer.valueOf(value.toString());

				} else if (fieldType == Long.class || fieldType == long.class) {
					return Long.valueOf(value.toString());

				} else if (fieldType == Float.class || fieldType == float.class) {
					return Float.valueOf(value.toString());

				} else if (fieldType == Double.class || fieldType == double.class) {
					return Integer.valueOf(value.toString());

				} else if (fieldType == Boolean.class || fieldType == boolean.class) {
					return Integer.valueOf(value.toString());

				} else if (fieldType == Character.class || fieldType == char.class) {
					return Character.valueOf(value.toString().charAt(0));

				} else {
					return value;
				}
			}
		}
	}

}
