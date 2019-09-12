package top.hjie.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import top.hjie.annotation.Autowired;
import top.hjie.annotation.Controller;
import top.hjie.annotation.RequestMapping;
import top.hjie.service.IBaseService;

@Controller
@RequestMapping("/test")
public class IndexController {

	@Autowired
	private IBaseService baseService;
	
	@RequestMapping("/index")
	public void index(HttpServletRequest req, HttpServletResponse resp){
		if (req.getParameter("username") == null) {
           try {
                resp.getWriter().write("param username is null");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            String paramName = req.getParameter("username");
            try {
                resp.getWriter().write("param username is " + paramName);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("[INFO-req] New request param username-->" + paramName);
        }
	}
	
}
