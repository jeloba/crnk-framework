package io.crnk.core.engine.internal.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import io.crnk.core.exception.ResourceException;
import io.crnk.core.utils.Optional;


/**
 * Provides reflection methods for parsing information about a class.
 */
public class ClassUtils {

	public static final String PREFIX_GETTER_IS = "is";

	public static final String PREFIX_GETTER_GET = "get";


	private static final Map<Class<?>, Class<?>> primitiveWrapperMap = new HashMap();

	private static final Map<Class<?>, Class<?>> wrapperPrimitiveMap;

	static {
		primitiveWrapperMap.put(Boolean.TYPE, Boolean.class);
		primitiveWrapperMap.put(Byte.TYPE, Byte.class);
		primitiveWrapperMap.put(Character.TYPE, Character.class);
		primitiveWrapperMap.put(Short.TYPE, Short.class);
		primitiveWrapperMap.put(Integer.TYPE, Integer.class);
		primitiveWrapperMap.put(Long.TYPE, Long.class);
		primitiveWrapperMap.put(Double.TYPE, Double.class);
		primitiveWrapperMap.put(Float.TYPE, Float.class);
		primitiveWrapperMap.put(Void.TYPE, Void.TYPE);
		wrapperPrimitiveMap = new HashMap();
		Iterator i$ = primitiveWrapperMap.keySet().iterator();

		while (i$.hasNext()) {
			Class primitiveClass = (Class) i$.next();
			Class wrapperClass = (Class) primitiveWrapperMap.get(primitiveClass);
			if (!primitiveClass.equals(wrapperClass)) {
				wrapperPrimitiveMap.put(wrapperClass, primitiveClass);
			}
		}
	}

	private ClassUtils() {
	}


	/**
	 * Returns whether the given {@code type} is a primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character}, {@link Short},
	 * {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
	 *
	 * @param type
	 *            The class to query or null.
	 * @return true if the given {@code type} is a primitive wrapper ({@link Boolean}, {@link Byte}, {@link Character}, {@link Short},
	 *         {@link Integer}, {@link Long}, {@link Double}, {@link Float}).
	 * @since 3.1
	 */
	public static boolean isPrimitiveWrapper(final Class<?> type) {
		return wrapperPrimitiveMap.containsKey(type);
	}


	/**
	 * <p>Checks if one {@code Class} can be assigned to a variable of
	 * another {@code Class}.</p>
	 *
	 * <p>Unlike the {@link Class#isAssignableFrom(java.lang.Class)} method,
	 * this method takes into account widenings of primitive classes and
	 * {@code null}s.</p>
	 *
	 * <p>Primitive widenings allow an int to be assigned to a long, float or
	 * double. This method returns the correct result for these cases.</p>
	 *
	 * <p>{@code Null} may be assigned to any reference type. This method
	 * will return {@code true} if {@code null} is passed in and the
	 * toClass is non-primitive.</p>
	 *
	 * <p>Specifically, this method tests whether the type represented by the
	 * specified {@code Class} parameter can be converted to the type
	 * represented by this {@code Class} object via an identity conversion
	 * widening primitive or widening reference conversion. See
	 * <em><a href="http://docs.oracle.com/javase/specs/">The Java Language Specification</a></em>,
	 * sections 5.1.1, 5.1.2 and 5.1.4 for details.</p>
	 *
	 * @param cls  the Class to check, may be null
	 * @param toClass  the Class to try to assign into, returns false if null
	 * @return {@code true} if assignment possible
	 */
	public static boolean isAssignable(Class<?> cls, final Class<?> toClass) {
		final boolean autoboxing = true;
		if (toClass == null) {
			return false;
		}
		// have to check for null, as isAssignableFrom doesn't
		if (cls == null) {
			return !toClass.isPrimitive();
		}
		//autoboxing:
		if (autoboxing) {
			if (cls.isPrimitive() && !toClass.isPrimitive()) {
				cls = primitiveToWrapper(cls);
				if (cls == null) {
					return false;
				}
			}
			if (toClass.isPrimitive() && !cls.isPrimitive()) {
				cls = wrapperToPrimitive(cls);
				if (cls == null) {
					return false;
				}
			}
		}
		if (cls.equals(toClass)) {
			return true;
		}
		if (cls.isPrimitive()) {
			if (toClass.isPrimitive() == false) {
				return false;
			}
			if (Integer.TYPE.equals(cls)) {
				return Long.TYPE.equals(toClass)
						|| Float.TYPE.equals(toClass)
						|| Double.TYPE.equals(toClass);
			}
			if (Long.TYPE.equals(cls)) {
				return Float.TYPE.equals(toClass)
						|| Double.TYPE.equals(toClass);
			}
			if (Boolean.TYPE.equals(cls)) {
				return false;
			}
			if (Double.TYPE.equals(cls)) {
				return false;
			}
			if (Float.TYPE.equals(cls)) {
				return Double.TYPE.equals(toClass);
			}
			if (Character.TYPE.equals(cls)) {
				return Integer.TYPE.equals(toClass)
						|| Long.TYPE.equals(toClass)
						|| Float.TYPE.equals(toClass)
						|| Double.TYPE.equals(toClass);
			}
			if (Short.TYPE.equals(cls)) {
				return Integer.TYPE.equals(toClass)
						|| Long.TYPE.equals(toClass)
						|| Float.TYPE.equals(toClass)
						|| Double.TYPE.equals(toClass);
			}
			if (Byte.TYPE.equals(cls)) {
				return Short.TYPE.equals(toClass)
						|| Integer.TYPE.equals(toClass)
						|| Long.TYPE.equals(toClass)
						|| Float.TYPE.equals(toClass)
						|| Double.TYPE.equals(toClass);
			}
			// should never get here
			return false;
		}
		return toClass.isAssignableFrom(cls);
	}

	public static Class<?> primitiveToWrapper(Class<?> cls) {
		Class convertedClass = cls;
		if (cls != null && cls.isPrimitive()) {
			convertedClass = (Class) primitiveWrapperMap.get(cls);
		}

		return convertedClass;
	}

	public static Class<?>[] primitivesToWrappers(Class... classes) {
		if (classes == null) {
			return null;
		}
		else if (classes.length == 0) {
			return classes;
		}
		else {
			Class[] convertedClasses = new Class[classes.length];

			for (int i = 0; i < classes.length; ++i) {
				convertedClasses[i] = primitiveToWrapper(classes[i]);
			}

			return convertedClasses;
		}
	}

	public static Class<?> wrapperToPrimitive(Class<?> cls) {
		return (Class) wrapperPrimitiveMap.get(cls);
	}

	public static Class<?>[] wrappersToPrimitives(Class... classes) {
		if (classes == null) {
			return null;
		}
		else if (classes.length == 0) {
			return classes;
		}
		else {
			Class[] convertedClasses = new Class[classes.length];

			for (int i = 0; i < classes.length; ++i) {
				convertedClasses[i] = wrapperToPrimitive(classes[i]);
			}

			return convertedClasses;
		}
	}


	@Deprecated // at least current use cases should be eliminated and replace by resourceType
	public static Class<?> getResourceClass(Type genericType, Class baseClass) {
		if (Iterable.class.isAssignableFrom(baseClass)) {
			if (genericType instanceof ParameterizedType) {
				ParameterizedType aType = (ParameterizedType) genericType;
				Type[] fieldArgTypes = aType.getActualTypeArguments();
				if (fieldArgTypes.length == 1 && fieldArgTypes[0] instanceof Class<?>) {
					return (Class) fieldArgTypes[0];
				}
				else {
					throw new IllegalArgumentException("Wrong type: " + aType);
				}
			}
			else {
				throw new IllegalArgumentException("The relationship must be parametrized (cannot be wildcard or array): "
						+ genericType);
			}
		}
		return baseClass;
	}

	public static boolean existsClass(String className) {
		try {
			Class.forName(className);
			return true;
		}
		catch (ClassNotFoundException e) {
			return false;
		}
	}

	/**
	 * Returns a list of class fields. Supports inheritance and doesn't return synthetic fields.
	 *
	 * @param beanClass class to be searched for
	 * @return a list of found fields
	 */
	public static List<Field> getClassFields(Class<?> beanClass) {
		Map<String, Field> resultMap = new HashMap<>();
		LinkedList<Field> results = new LinkedList<>();

		Class<?> currentClass = beanClass;
		while (currentClass != null && currentClass != Object.class) {
			for (Field field : currentClass.getDeclaredFields()) {
				if (!field.isSynthetic()) {
					Field v = resultMap.get(field.getName());
					if (v == null) {
						resultMap.put(field.getName(), field);
						results.add(field);
					}
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		return results;
	}

	/**
	 * Returns an instance of bean's annotation
	 *
	 * @param beanClass class to be searched for
	 * @param annotationClass type of an annotation
	 * @param <T> type of an annotation
	 * @return an instance of an annotation
	 */
	public static <T extends Annotation> Optional<T> getAnnotation(Class<?> beanClass, Class<T> annotationClass) {
		Class<?> currentClass = beanClass;
		while (currentClass != null && currentClass != Object.class) {
			if (currentClass.isAnnotationPresent(annotationClass)) {
				return Optional.of(currentClass.getAnnotation(annotationClass));
			}
			currentClass = currentClass.getSuperclass();
		}

		return Optional.empty();
	}

	/**
	 * Tries to find a class fields. Supports inheritance and doesn't return synthetic fields.
	 *
	 * @param beanClass class to be searched for
	 * @param fieldName field name
	 * @return a list of found fields
	 */
	public static Field findClassField(Class<?> beanClass, String fieldName) {
		Class<?> currentClass = beanClass;
		while (currentClass != null && currentClass != Object.class) {
			for (Field field : currentClass.getDeclaredFields()) {
				if (field.isSynthetic()) {
					continue;
				}

				if (field.getName().equals(fieldName)) {
					return field;
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		return null;
	}

	public static Method findGetter(Class<?> beanClass, String fieldName) {
		for (Method method : beanClass.getMethods()) {
			if (!isGetter(method)) {
				continue;
			}
			String methodGetterName = getGetterFieldName(method);
			if (fieldName.equals(methodGetterName)) {
				return method;
			}
		}
		return null;
	}


	public static String getGetterFieldName(Method getter) {
		int getterPrefixLength = getPropertyGetterPrefixLength(getter);
		if (getterPrefixLength == 0) {
			return null;
		}

		return StringUtils.decapitalize(getter.getName().substring(getterPrefixLength));
	}

	private static boolean isValidBeanGetter(Method getter) {
		// property getters must have non-null return type and zero parameters
		int parameterCount = getter.getParameterTypes().length;
		Class returnType = getter.getReturnType();
		return returnType != null && parameterCount == 0;
	}

	private static int getPropertyGetterPrefixLength(Method getter) {
		if (!isValidBeanGetter(getter)) {
			return 0;
		}

		String name = getter.getName();
		boolean isBooleanReturnType = isBoolean(getter.getReturnType());

		int prefixLength = 0;
		if (isBooleanReturnType && name.startsWith(PREFIX_GETTER_IS)) {
			prefixLength = 2;
		}

		if (name.startsWith(PREFIX_GETTER_GET)) {
			prefixLength = 3;
		}

		// check for methods called "get" and "is", as these aren't valid getters
		if (prefixLength == name.length()) {
			prefixLength = 0;
		}

		return prefixLength;
	}

	private static boolean isBoolean(Class<?> returnType) {
		return boolean.class.equals(returnType) || Boolean.class.equals(returnType);
	}


	public static Method findSetter(Class<?> beanClass, String fieldName, Class<?> fieldType) {
		String upperCaseName = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);

		try {
			return beanClass.getMethod("set" + upperCaseName, fieldType);
		}
		catch (NoSuchMethodException e1) {
			return null;
		}
	}

	/**
	 * <p>
	 * Return a list of class getters. Supports inheritance and overriding, that is when a method is found on the
	 * lowest level of inheritance chain, no other method can override it. Supports inheritance and
	 * doesn't return synthetic methods.
	 * <p>
	 * A getter:
	 * <ul>
	 * <li>Starts with an <i>is</i> if returns <i>boolean</i> or {@link Boolean} value</li>
	 * <li>Starts with a <i>get</i> if returns non-boolean value</li>
	 * </ul>
	 *
	 * @param beanClass class to be searched for
	 * @return a list of found getters
	 */
	public static List<Method> getClassGetters(Class<?> beanClass) {
		Map<String, Method> resultMap = new HashMap<>();
		LinkedList<Method> results = new LinkedList<>();

		Class<?> currentClass = beanClass;
		while (currentClass != null && currentClass != Object.class) {
			getDeclaredClassGetters(currentClass, resultMap, results);
			currentClass = currentClass.getSuperclass();
		}

		return results;
	}

	private static void getDeclaredClassGetters(Class<?> currentClass, Map<String, Method> resultMap,
			LinkedList<Method> results) {
		for (Method method : currentClass.getDeclaredMethods()) {
			if (!method.isSynthetic() && isGetter(method)) {
				Method v = resultMap.get(method.getName());
				if (v == null) {
					resultMap.put(method.getName(), method);
					results.add(method);
				}
			}
		}
	}

	/**
	 * Return a list of class setters. Supports inheritance and overriding, that is when a method is found on the
	 * lowest level of inheritance chain, no other method can override it.  Supports inheritance
	 * and doesn't return synthetic methods.
	 *
	 * @param beanClass class to be searched for
	 * @return a list of found getters
	 */
	public static List<Method> getClassSetters(Class<?> beanClass) {
		Map<String, Method> result = new HashMap<>();

		Class<?> currentClass = beanClass;
		while (currentClass != null && currentClass != Object.class) {
			for (Method method : currentClass.getDeclaredMethods()) {
				if (!method.isSynthetic() && isSetter(method)) {
					Method v = result.get(method.getName());
					if (v == null) {
						result.put(method.getName(), method);
					}
				}
			}
			currentClass = currentClass.getSuperclass();
		}

		return new LinkedList<>(result.values());
	}

	/**
	 * Return a first occurrence of a method annotated with specified annotation
	 *
	 * @param searchClass class to be searched
	 * @param annotationClass annotation class
	 * @return annotated method or null
	 */
	public static Method findMethodWith(Class<?> searchClass, Class<? extends Annotation> annotationClass) {
		while (searchClass != null && searchClass != Object.class) {
			for (Method method : searchClass.getDeclaredMethods()) {
				if (method.isAnnotationPresent(annotationClass)) {
					return method;
				}
			}
			searchClass = searchClass.getSuperclass();
		}
		return null;
	}

	/**
	 * Create a new instance of a object using a default constructor
	 *
	 * @param clazz new instance class
	 * @param <T> new instance class
	 * @return new instance
	 */
	public static <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new ResourceException(String.format("couldn't create a new instance of %s", clazz));
		}
	}

	private static boolean isGetter(Method method) {
		return isBooleanGetter(method) || isNonBooleanGetter(method);
	}

	public static boolean isBooleanGetter(Method method) {
		boolean startsWithValidPrefix = getPropertyGetterPrefixLength(method) > 0;

		if (!startsWithValidPrefix || method.getParameterTypes().length != 0) {
			return false;
		}

		return boolean.class.equals(method.getReturnType()) || Boolean.class.equals(method.getReturnType());
	}

	private static boolean isNonBooleanGetter(Method method) {

		if (!method.getName().startsWith("get")) {
			return false;
		}
		if (method.getName().length() < 4) {
			return false;
		}
		if (method.getParameterTypes().length != 0) {
			return false;
		}

		return !void.class.equals(method.getReturnType());
	}

	private static boolean isSetter(Method method) {

		if (!method.getName().startsWith("set")) {
			return false;
		}
		if (method.getName().length() < 4) {
			return false;
		}

		return method.getParameterTypes().length == 1;
	}

	public static Class<?> getRawType(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		}
		else if (type instanceof ParameterizedType) {
			return getRawType(((ParameterizedType) type).getRawType());
		}
		else {
			throw new IllegalStateException("unknown type: " + type);
		}
	}

	public static boolean isPrimitiveType(Class<?> type) {
		boolean isInt = type == byte.class || type == short.class || type == int.class || type == long.class;
		boolean isDecimal = type == short.class || type == double.class;
		return type == boolean.class || isInt || isDecimal;
	}

}
