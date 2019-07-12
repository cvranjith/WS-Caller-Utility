package com.ofss.fcubs.custom.servlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;
import java.io.PrintWriter;
import com.ofss.fcubs.custom.handler.WSCallerHandler;

public class WSCallerServlet
  extends HttpServlet
{
static weblogic.logging.NonCatalogLogger logger;
protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
  {
	logger = new weblogic.logging.NonCatalogLogger("wscaller");
	logger.info("WSCallerServlet.doPost: WS Caller Servlet doPost ");
    PrintWriter out = response.getWriter();
	response.setContentType("text/html;charset=UTF-8");	
	String requestStr = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
	logger.debug("WSCallerServlet.doPost: requestStr "+requestStr);
	WSCallerHandler wsch = new WSCallerHandler();
	String responseStr = wsch.processRequest (requestStr);
	logger.debug("WSCallerServlet.doPost: responseStr "+responseStr);
	out.println(responseStr);
	logger = null;
  }
}
