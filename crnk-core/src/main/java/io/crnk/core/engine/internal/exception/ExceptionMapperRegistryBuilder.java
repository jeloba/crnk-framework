package io.crnk.core.engine.internal.exception;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import io.crnk.core.engine.error.ExceptionMapper;
import io.crnk.core.engine.error.JsonApiExceptionMapper;
import io.crnk.core.engine.internal.utils.TypeUtils;
import io.crnk.legacy.internal.DefaultExceptionMapperLookup;

public final class ExceptionMapperRegistryBuilder {
	private final Set<ExceptionMapperType> exceptionMappers = new HashSet<>();

	public ExceptionMapperRegistry build(String resourceSearchPackage) {
		return build(new DefaultExceptionMapperLookup(resourceSearchPackage));
	}

	public ExceptionMapperRegistry build(ExceptionMapperLookup exceptionMapperLookup) {
		addDefaultMappers();
		for (JsonApiExceptionMapper<?> exceptionMapper : exceptionMapperLookup.getExceptionMappers()) {
			registerExceptionMapper(exceptionMapper);
		}
		return new ExceptionMapperRegistry(exceptionMappers);
	}

	private void addDefaultMappers() {
		registerExceptionMapper(new CrnkExceptionMapper());
	}

	private void registerExceptionMapper(JsonApiExceptionMapper<? extends Throwable> exceptionMapper) {
		Class<? extends JsonApiExceptionMapper> mapperClass = exceptionMapper.getClass();
		Class<? extends Throwable> exceptionClass = getGenericType(mapperClass);
		if(exceptionClass == null && mapperClass.getName().contains("$$")){
			// deal if dynamic proxies, like in CDI
			mapperClass = (Class<? extends JsonApiExceptionMapper>) mapperClass.getSuperclass();
			exceptionClass = getGenericType(mapperClass);
		}

		exceptionMappers.add(new ExceptionMapperType(exceptionClass, exceptionMapper));
	}

	private Class<? extends Throwable> getGenericType(Class<? extends JsonApiExceptionMapper> mapper) {
		Type[] types = mapper.getGenericInterfaces();
		if (null == types || 0 == types.length) {
			types = new Type[]{mapper.getGenericSuperclass()};
		}

		for (Type type : types) {
			if (type instanceof ParameterizedType && (
					TypeUtils.isAssignable(((ParameterizedType) type).getRawType(), JsonApiExceptionMapper.class)
					|| TypeUtils.isAssignable(((ParameterizedType) type).getRawType(), ExceptionMapper.class))) {
				//noinspection unchecked
				return (Class<? extends Throwable>) ((ParameterizedType) type).getActualTypeArguments()[0];
			}
		}
		//Won't get in here
		return null;
	}

}
