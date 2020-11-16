package com.learning.myspringmvc.mvc.controller;

import com.learning.myspringmvc.mvc.service.MyServiceDemo;
import com.learning.myspringmvc.spring.annotation.MyAutowired;
import com.learning.myspringmvc.spring.annotation.MyController;
import com.learning.myspringmvc.spring.annotation.MyRequestMapping;
import com.learning.myspringmvc.spring.annotation.MyResponseBody;

@MyController
@MyRequestMapping("/simple-webapp")
public class MyControllerDemo {

	@MyAutowired
	private MyServiceDemo myServiceDemo;

	@MyRequestMapping("/")
	public String getIndex() {
		return "index";
	}

	@MyRequestMapping("hello")
	public String getHello() {
		return "hello";
	}

	@MyRequestMapping("userinfo")
	@MyResponseBody
	public String getUserInfo() {
		return myServiceDemo.getUserInfo("1");
	}
}
