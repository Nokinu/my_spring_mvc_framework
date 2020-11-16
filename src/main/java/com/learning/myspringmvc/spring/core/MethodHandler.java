package com.learning.myspringmvc.spring.core;

import java.lang.reflect.Method;
import java.util.List;

import lombok.Data;

@Data
public class MethodHandler {

	private Object object;

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public List<String> getParams() {
		return params;
	}

	public void setParams(List<String> params) {
		this.params = params;
	}

	private Method method;

	private List<String> params;
}
