package top.hjie.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import top.hjie.annotation.Autowired;
import top.hjie.annotation.Controller;
import top.hjie.annotation.RequestMapping;
import top.hjie.annotation.Service;

public class BasicDispatchServlet extends HttpServlet{
	
	/**
     * 属性配置文件
     */
    private Properties contextConfig = new Properties();

    private List<String> classNameList = new ArrayList<>();
    
    /**
     * IOC 容器
     */
    Map<String, Object> iocMap = new HashMap<String, Object>();

    Map<String, Method> handlerMapping = new HashMap<String, Method>();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			this.doDispatch(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			this.doDispatch(req, resp);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 拦截请求，匹配
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	public void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception{
		String uri = req.getRequestURI();
		String contextPath = req.getContextPath();
		uri = uri.replaceAll(contextPath, "").replaceAll("/+", "/");
		System.out.println("[INFO-doDispatch] request uri-->" + uri);
		if(!this.handlerMapping.containsKey(uri)){
			try {
                resp.getWriter().write("404 NOT FOUND!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
		}
		
		Method method = this.handlerMapping.get(uri);
		System.out.println("[INFO-method] method-->" + method);
		
		String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
		// 第一个参数是获取方法，后面是参数，多个参数直接加，按顺序对应
		method.invoke(iocMap.get(beanName), req ,resp);
		System.out.println("[INFO-ioc] method.invoke put {" + iocMap.get(beanName) + "}.");
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		// 1.加载配置文件
		doLoadConfig();
		// 2.扫描相关的类
		doScanner(contextConfig.getProperty("scan-package"));
		// 3.初始化ioc容器，将所有相关的类实例保存到 IOC 容器中
		doInstance();
		// 4.依赖注入
        doAutowired();
        // 5.初始化 HandlerMapping
        initHandlerMapping();
        System.out.println("Spring FrameWork is init.");
        // 6.打印数据
        doTestPrintData();
	}

	/**
     * 1、加载配置文件
     *
     * @param contextConfigLocation web.xml --> servlet/init-param
     */
    private void doLoadConfig() {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("application.properties");

        try {
            // 保存在内存
            contextConfig.load(inputStream);

            System.out.println("[INFO-1] property file has been saved in contextConfig.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 2、扫描相关的类
     *
     * @param scanPackage properties --> scan-package
     */
    private void doScanner(String scanPackage) {

        // package's . ==> /
        URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        if (resourcePath == null) {
            return;
        }

        File classPath = new File(resourcePath.getFile());

        for (File file : classPath.listFiles()) {

            if (file.isDirectory()) {

                System.out.println("[INFO-2] {" + file.getName() + "} is a directory.");

                // 子目录递归
                doScanner(scanPackage + "." + file.getName());

            } else {

                if (!file.getName().endsWith(".class")) {
                    System.out.println("[INFO-2] {" + file.getName() + "} is not a class file.");
                    continue;
                }

                String className = (scanPackage + "." + file.getName()).replace(".class", "");

                // 保存在内容
                classNameList.add(className);

                System.out.println("[INFO-2] {" + className + "} has been saved in classNameList.");
            }
        }
    }
    
    /**
     * 3、初始化 IOC 容器，将所有相关的类实例保存到 IOC 容器中
     */
    private void doInstance() {
        if (classNameList.isEmpty()) {
            return;
        }

        try {
            for (String className : classNameList) {

                Class<?> clazz = Class.forName(className);

                if (clazz.isAnnotationPresent(Controller.class)) {
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    Object instance = clazz.newInstance();

                    // 保存在 ioc 容器
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO-3] {" + beanName + "} has been saved in iocMap.");

                } else if (clazz.isAnnotationPresent(Service.class)) {

                    String beanName = toLowerFirstCase(clazz.getSimpleName());

                    // 如果注解包含自定义名称
                    Service service = clazz.getAnnotation(Service.class);
                    if (!"".equals(service.value())) {
                        beanName = service.value();
                    }

                    Object instance = clazz.newInstance();
                    iocMap.put(beanName, instance);
                    System.out.println("[INFO-3] {" + beanName + "} has been saved in iocMap.");

                    // 找类的接口
                    for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist.");
                        }

                        iocMap.put(i.getName(), instance);
                        System.out.println("[INFO-3] {" + i.getName() + "} has been saved in iocMap.");
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	
    /**
     * 4、依赖注入
     */
    private void doAutowired() {
        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {

            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {
                if (!field.isAnnotationPresent(Autowired.class)) {
                    continue;
                }

                System.out.println("[INFO-4] Existence XAutowired.");

                // 获取注解对应的类
                Autowired autowired = field.getAnnotation(Autowired.class);
                String beanName = autowired.value().trim();

                // 获取 XAutowired 注解的值
                if ("".equals(beanName)) {
                    System.out.println("[INFO] autowired.value() is null");
                    beanName = field.getType().getName();
                }

                // 只要加了注解，都要加载，不管是 private 还是 protect
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), iocMap.get(beanName));

                    System.out.println("[INFO-4] field set {" + entry.getValue() + "} - {" + iocMap.get(beanName) + "}.");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 5、初始化 HandlerMapping
     */
    private void initHandlerMapping() {

        if (iocMap.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(Controller.class)) {
                continue;
            }

            String baseUrl = "";

            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping RequestMapping = clazz.getAnnotation(RequestMapping.class);
                baseUrl = RequestMapping.value();
            }

            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(RequestMapping.class)) {
                    continue;
                }

                RequestMapping RequestMapping = method.getAnnotation(RequestMapping.class);

                String url = ("/" + baseUrl + "/" + RequestMapping.value()).replaceAll("/+", "/");

                handlerMapping.put(url, method);

                System.out.println("[INFO-5] handlerMapping put {" + url + "} - {" + method + "}.");

            }
        }

    }
    
    /**
     * 6、打印数据
     */
    private void doTestPrintData() {

        System.out.println("[INFO-6]----data------------------------");

        System.out.println("contextConfig.propertyNames()-->" + contextConfig.propertyNames());

        System.out.println("[classNameList]-->");
        for (String str : classNameList) {
            System.out.println(str);
        }

        System.out.println("[iocMap]-->");
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[handlerMapping]-->");
        for (Map.Entry<String, Method> entry : handlerMapping.entrySet()) {
            System.out.println(entry);
        }

        System.out.println("[INFO-6]----done-----------------------");

        System.out.println("====启动成功====");
        System.out.println("测试地址：http://localhost:8080/test/query?username=xiaopengwei");
        System.out.println("测试地址：http://localhost:8080/test/listClassName");
    }
    
	/**
     * 获取类的首字母小写的名称
     *
     * @param className ClassName
     * @return java.lang.String
     */
    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }
}
