package com.learning.myspringmvc.spring.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reflections.Reflections;
import org.reflections.scanners.MethodParameterNamesScanner;

import com.learning.myspringmvc.spring.annotation.MyAutowired;
import com.learning.myspringmvc.spring.annotation.MyController;
import com.learning.myspringmvc.spring.annotation.MyRequestMapping;
import com.learning.myspringmvc.spring.annotation.MyResponseBody;
import com.learning.myspringmvc.spring.annotation.MyService;
import com.learning.myspringmvc.spring.core.MethodHandler;

public class MyDispatcherServlet extends HttpServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4215045070813642267L;

	// Properties file
	private Properties properties = new Properties();

	// store all classes with annotations
	private List<String> classNameList = new ArrayList<String>();

	private Map<String, Object> IOCByType = new HashMap<String, Object>();

	private Map<String, Object> IOCByName = new HashMap<String, Object>();

	private Map<String, MethodHandler> urlHandler = new HashMap<String, MethodHandler>();

	@Override
	public void init() throws ServletException {
		System.out.println("Start initilation for servlet!");
		doLoadConfig();
		doScanner(this.properties.getProperty("basepackage"));
		doIOC();
		try {
			doAutowired();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		doURLMapping();
		doJSPRegister();
		System.out.println("Servlet is ready for use");
	}

	/**
	 * Step 1: load the config
	 */
	private void doLoadConfig() {
		ServletConfig config = this.getServletConfig();
		String configurationPath = config.getInitParameter("contextConfigLocation");
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(configurationPath);

		try {
			this.properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("**** Step 1 : Loading Config Done ! **** ");
	}

	/**
	 * Step 2 : do the scanner
	 */
	private void doScanner(String path) {
		if (path.endsWith(".class")) {
			String className = path.substring(0, path.lastIndexOf(".class"));
			this.classNameList.add(className);
			return;
		}

		URL url = this.getClass().getClassLoader().getResource("/" + path.replaceAll("\\.", "/"));
		File file = new File(url.getFile());
		File[] files = file.listFiles();
		for (File f : files) {
			doScanner(path + "." + f.getName());
		}
	}

	private static String lowerFirstCase(String str) {
		char[] chars = str.toCharArray();
		chars[0] += 32;
		return String.valueOf(chars);
	}

	/**
	 * Step 3 Initial all classes and IOC
	 */
	private void doIOC() {
		if (this.classNameList.isEmpty()) {
			System.out.println("No class being loaded !");
			return;
		}

		try {
			for (String className : classNameList) {
				Class<?> clazz = Class.forName(className);
				// Key value generation for IOC container if annotation is attached
				if (clazz.isAnnotationPresent(MyController.class)) {
					MyController controller = clazz.getAnnotation(MyController.class);
					String beanName = controller.value().trim();
					if (beanName == "") {
						beanName = lowerFirstCase(clazz.getSimpleName());
					}
					Object instance = clazz.getDeclaredConstructor().newInstance();
					IOCByName.put(beanName, instance);
					IOCByType.put(clazz.getName(), instance);
				} else if (clazz.isAnnotationPresent(MyService.class)) {
					MyService myService = clazz.getAnnotation(MyService.class);
					String beanName = myService.value().trim();
					if (beanName == "") {
						beanName = lowerFirstCase(clazz.getSimpleName());
					}
					Object instance = clazz.newInstance();
					IOCByName.put(beanName, instance);
					IOCByType.put(clazz.getName(), instance);
					Class<?>[] interfaces = clazz.getInterfaces();
					for (Class<?> interfac : interfaces) {
						IOCByName.put(lowerFirstCase(interfac.getSimpleName()), instance);
						IOCByType.put(interfac.getName(), instance);
					}
				} else {
					continue;
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("Name : " + IOCByName);
		System.out.println("Type : " + IOCByType);
		System.out.println("**** Step 3: IOC creation Done ! ****");
	}

	/**
	 * Step 4 -- do autowired
	 */
	private void doAutowired() throws IllegalAccessException {
		if (IOCByName.isEmpty() && IOCByType.isEmpty()) {
			System.out.println("IOC container is empty!");
			return;
		}
		for (Entry<String, Object> entry : IOCByType.entrySet()) {

			Field[] fields = entry.getValue().getClass().getDeclaredFields();
			for (Field field : fields) {

				field.setAccessible(true);
				if (field.isAnnotationPresent(MyAutowired.class)) {
					Object instance = null;
					String typeName = field.getType().getName();
					String beanName = lowerFirstCase(field.getType().getSimpleName());
					System.out.println("typename : " + typeName);
					System.out.println("beanname : " + beanName);
					if (IOCByType.containsKey(typeName)) {
						instance = IOCByType.get(typeName);
					} else if (IOCByName.containsKey(beanName)) {
						instance = IOCByName.get(beanName);
					} else {
						throw new RuntimeException("class : " + typeName + " is not injected in IOC container!");
					}

					try {
						field.set(entry.getValue(), instance);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		System.out.println("**** Step 4: Autowired register Done! ****");
	}

	private void doURLMapping() {
		if (IOCByName.isEmpty() && IOCByType.isEmpty()) {
			System.out.println("IOC container is empty !");
			return;
		}

		for (Entry<String, Object> entry : IOCByType.entrySet()) {
			Class<?> clazz = entry.getValue().getClass();
			// Check if it is the controller
			if (clazz.isAnnotationPresent(MyController.class)) {
				String startUrl = "/";
				// Check if controller has MyRequestMapping annotation
				if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
					MyRequestMapping mapping = clazz.getAnnotation(MyRequestMapping.class);
					String value = mapping.value();
					if (value != "") {
						startUrl += value;
					}
				}
				// loop all methods from controller, and add the url into the urlHandler
				Method[] methods = clazz.getDeclaredMethods();
				for (Method method : methods) {
					if (method.isAnnotationPresent(MyRequestMapping.class)) {
						MyRequestMapping methodMapping = method.getAnnotation(MyRequestMapping.class);
						String methodUrl = startUrl + "/" + methodMapping.value().trim();
						methodUrl = methodUrl.replaceAll("/+", "/");
						System.out.println("method url is : " + methodUrl);
						MethodHandler methodHandler = new MethodHandler();
						methodHandler.setMethod(method);

						try {
							methodHandler.setObject(entry.getValue());
						} catch (Exception e) {
							e.printStackTrace();
						}

						List<String> params = doParamHandler(method);
						methodHandler.setParams(params);
						urlHandler.put(methodUrl, methodHandler);
					}
				}
			}
		}
		System.out.println("**** Step 5: URL Mapping Done! ****");
	}

	/**
	 * step 5 - register JSP/Asset
	 */
	private void doJSPRegister() {
		ServletContext context = getServletContext();
		ServletRegistration jspServlet = context.getServletRegistration("jsp");
		jspServlet.addMapping(this.properties.getProperty("view.prefix") + "*");

		System.out.println("**** Step 6: JSP register Done! ****");
	}

	private List<String> doParamHandler(Method method) {
		Reflections reflections = new Reflections(new MethodParameterNamesScanner());
		return reflections.getMethodParamNames(method);
	}

	@Override
	protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException {
		this.doHandler(httpServletRequest, httpServletResponse);
	}

	@Override
	protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException {
		this.doHandler(httpServletRequest, httpServletResponse);
	}

	private void doHandler(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
			throws IOException {
		boolean jsonResult = false;
		String url = httpServletRequest.getRequestURI();
		PrintWriter writer = httpServletResponse.getWriter();
		if (!urlHandler.containsKey(url)) {
			writer.write("404 Not found!");
			return;
		}

		// lookup the method handlerbased on url
		MethodHandler handler = urlHandler.get(url);
		// Get the method/controller/params
		Method method = handler.getMethod();

		Object controller = handler.getObject();

		List<String> params = handler.getParams();

		if (controller.getClass().isAnnotationPresent(MyResponseBody.class)
				|| method.isAnnotationPresent(MyResponseBody.class)) {
			jsonResult = true;
		}

		List<Object> args = new ArrayList<Object>();
		for (String param : params) {
			args.add(httpServletRequest.getParameter(param));
		}

		try {
			Object result = method.invoke(controller, args.toArray());
			if (jsonResult) {
				writer.write(result.toString());
			} else {
				doResolveView(result.toString(), httpServletRequest, httpServletResponse);
			}
		} catch (Exception e) {
			e.printStackTrace();
			writer.write("500 Internal Server Error !");
			return;
		}
	}

	private void doResolveView(String string, HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) throws ServletException, IOException {
		String prefix = this.properties.getProperty("view.prefix");

		String suffix = this.properties.getProperty("view.suffix");

		String view = (prefix + string + suffix).trim().replaceAll("/+", "/");
		httpServletRequest.getRequestDispatcher(view).forward(httpServletRequest, httpServletResponse);
	}
}
