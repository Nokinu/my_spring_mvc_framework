package com.learning.myspringmvc.mvc.service.impl;

import com.learning.myspringmvc.mvc.service.MyServiceDemo;
import com.learning.myspringmvc.spring.annotation.MyService;

@MyService
public class MyServiceImpl implements MyServiceDemo {

	@Override
	public String getUserInfo(String id) {
		return "This is the uerinfo" + id;
	}

}
