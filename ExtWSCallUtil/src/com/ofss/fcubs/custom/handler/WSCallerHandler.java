package com.ofss.fcubs.custom.handler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.Writer;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.ssl.SSLContextBuilder;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

public class WSCallerHandler
{
static weblogic.logging.NonCatalogLogger logger;
static String tagVal(Document reqXML, String tag) throws Exception
  {
	logger.debug("WSCallerHandler.tagVal: tagVal " + tag);
	XPath xpath = XPathFactory.newInstance().newXPath();
	XPathExpression expr = xpath.compile("//WS_REQUEST/"+tag+"/text()");
	String val = (String)expr.evaluate(reqXML, XPathConstants.STRING);
	logger.debug("WSCallerHandler.tagVal:  Returning tagVal " + val);
	return val;
  }

static NodeList nodelist(Document reqXML, String tag) throws Exception
  {
	logger.debug("WSCallerHandler.nodelist:  nodelist " + tag);
	XPath xpath = XPathFactory.newInstance().newXPath();
	XPathExpression expr = xpath.compile("//WS_REQUEST/WS_HTTP_HEADERS/WS_HTTP_HEADER/"+tag+"/text()");
    Object result = expr.evaluate(reqXML, XPathConstants.NODESET);
    NodeList nodes = (NodeList) result;
	logger.debug("WSCallerHandler.nodelist: Returning nodelist");
	return nodes;
  }

static CloseableHttpResponse httppost(CloseableHttpClient httpClient, String wsurl, StringEntity wspayload, NodeList wshdrnames, NodeList wshdrvals) throws Exception
  {
	 logger.debug("WSCallerHandler.httppost: in httppost");
	HttpPost request = new HttpPost(wsurl);
	for (int i = 0; i < wshdrnames.getLength(); i++) {
		logger.debug("WSCallerHandler.httppost: Setting Header " + wshdrnames.item(i).getNodeValue() + " " + wshdrvals.item(i).getNodeValue());
		request.setHeader(wshdrnames.item(i).getNodeValue(), wshdrvals.item(i).getNodeValue());
	}
	request.setEntity(wspayload);
	logger.debug("WSCallerHandler.httppost: Before Executing httpClient");
	CloseableHttpResponse response = httpClient.execute(request);
	return response;
  }
static CloseableHttpResponse httpput(CloseableHttpClient httpClient, String wsurl, StringEntity wspayload, NodeList wshdrnames, NodeList wshdrvals) throws Exception
  {
	logger.debug("WSCallerHandler.httpput: in HttpPut");
	HttpPut request = new HttpPut(wsurl);
	for (int i = 0; i < wshdrnames.getLength(); i++) {
		logger.debug("WSCallerHandler.httpput: Setting Header " + wshdrnames.item(i).getNodeValue() + " " + wshdrvals.item(i).getNodeValue());
		request.setHeader(wshdrnames.item(i).getNodeValue(), wshdrvals.item(i).getNodeValue());
	}
	request.setEntity(wspayload);
	logger.debug("WSCallerHandler.httpput: Before Executing httpClient");
	CloseableHttpResponse response = httpClient.execute(request);
	return response;
  }
static CloseableHttpResponse httppatch(CloseableHttpClient httpClient, String wsurl, StringEntity wspayload, NodeList wshdrnames, NodeList wshdrvals) throws Exception
  {
	 logger.debug("WSCallerHandler.httppatch: in httppatch");
	HttpPatch request = new HttpPatch(wsurl);
	for (int i = 0; i < wshdrnames.getLength(); i++) {
		logger.debug("WSCallerHandler.httppatch: Setting Header " + wshdrnames.item(i).getNodeValue() + " " + wshdrvals.item(i).getNodeValue());
		request.setHeader(wshdrnames.item(i).getNodeValue(), wshdrvals.item(i).getNodeValue());
	}
	request.setEntity(wspayload);
	logger.debug("WSCallerHandler.httppatch: Before Executing httpClient");
	CloseableHttpResponse response = httpClient.execute(request);
	return response;
  }
static CloseableHttpResponse httpdelete(CloseableHttpClient httpClient, String wsurl, NodeList wshdrnames, NodeList wshdrvals) throws Exception
  {
	 logger.debug("WSCallerHandler.httpdelete: in httppost");
	HttpDelete request = new HttpDelete(wsurl);
	for (int i = 0; i < wshdrnames.getLength(); i++) {
		logger.debug("WSCallerHandler.httpdelete: Setting Header " + wshdrnames.item(i).getNodeValue() + " " + wshdrvals.item(i).getNodeValue());
		request.setHeader(wshdrnames.item(i).getNodeValue(), wshdrvals.item(i).getNodeValue());
	}
	logger.debug("WSCallerHandler.httpdelete: Before Executing httpClient");
	CloseableHttpResponse response = httpClient.execute(request);
	return response;
  }
static CloseableHttpResponse httpget(CloseableHttpClient httpClient, String wsurl, NodeList wshdrnames, NodeList wshdrvals ) throws Exception
  {
	logger.debug("WSCallerHandler.httpget: in httpget");
	HttpGet request = new HttpGet(wsurl);
	for (int i = 0; i < wshdrnames.getLength(); i++) {
		logger.debug("WSCallerHandler.httpget: Setting Header " + wshdrnames.item(i).getNodeValue() + " " + wshdrvals.item(i).getNodeValue());
		request.setHeader(wshdrnames.item(i).getNodeValue(), wshdrvals.item(i).getNodeValue());
	}
	logger.debug("WSCallerHandler.httpget: Before Executing httpClient");
	CloseableHttpResponse response = httpClient.execute(request);
	return response;
  }
static String callWS(Document reqXML) throws Exception
  {
	logger.debug("WSCallerHandler.callWS: in callWS " + reqXML);
	final SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (x509CertChain, authType) -> true).build();
	CloseableHttpClient httpClient = HttpClients.custom().setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).setSSLContext(sslContext).build();
	CloseableHttpResponse response;
	String httpverb = tagVal(reqXML,"WS_HTTP_VERB");
	NodeList wshdrnames = nodelist(reqXML,"WS_HTTP_HEADER_NAME");
	NodeList wshdrvals = nodelist(reqXML,"WS_HTTP_HEADER_VALUE");
	String wsurl = tagVal(reqXML,"WS_URL");
	StringEntity wspayload = new StringEntity (tagVal(reqXML,"WS_PAYLOAD"));
	if (httpverb.equals("POST")) {
		response = httppost(httpClient,wsurl,wspayload,wshdrnames,wshdrvals);
	}else if (httpverb.equals("PUT")){
		response = httpput(httpClient,wsurl,wspayload,wshdrnames,wshdrvals);
	}else if (httpverb.equals("PATCH")){
		response = httppatch(httpClient,wsurl,wspayload,wshdrnames,wshdrvals);
	}else if (httpverb.equals("DELETE")){
		response = httpdelete(httpClient,wsurl,wshdrnames,wshdrvals);
	}else {
		response = httpget(httpClient,wsurl,wshdrnames,wshdrvals);
	}		
	
	BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
	logger.debug("WSCallerHandler.callWS: Resp : " + rd.read());
	String line = "";
	String responseStr = "";
	while ((line = rd.readLine()) != null) {
		logger.debug("WSCallerHandler.callWS: Resp line : " + line);
		responseStr = responseStr + "\n" + line;
	}
	return responseStr;
  }
static Document parseRequest(String requestStr) throws Exception
  {
	logger.debug("WSCallerHandler.parseRequest: in parseRequest");
	InputStream reqbody = new ByteArrayInputStream(requestStr.getBytes());
	DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
	Document reqXML = builder.parse(reqbody);
	logger.debug("WSCallerHandler.parseRequest: Returning reqXML "+ reqXML);
    return reqXML;	
  }
public String processRequest(String requestStr)
  {
	logger = new weblogic.logging.NonCatalogLogger("wscaller");
	logger.info("WSCallerHandler.processRequest: WS Caller processRequest");
	logger.debug("WSCallerHandler.processRequest: Request String " + requestStr);
	String responseStr;
	try {
		Document reqXML = parseRequest(requestStr);
		responseStr = callWS(reqXML);
		logger.debug("WSCallerHandler.processRequest: Response String " + responseStr);
	} catch (Exception e) {
		logger.error("WSCallerHandler.processRequest: Exception in wscaller processRequest", e);
		Writer writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		e.printStackTrace(printWriter);
		responseStr = "<ERROR>"+writer.toString()+"</ERROR>";
        } 
	logger = null;
	return responseStr;
  }
}
