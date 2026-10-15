package com.pyplatform;
import jakarta.servlet.*; import jakarta.servlet.annotation.WebListener;
@WebListener public class AppListener implements ServletContextListener { public void contextInitialized(ServletContextEvent e) { Db.seed(); } }
