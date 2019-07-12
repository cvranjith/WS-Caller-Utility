package com.ofss.fcubs.custom.mdb;

import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.Connection;
import javax.jms.Session;
import javax.jms.MessageProducer;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.ejb.ActivationConfigProperty;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import com.ofss.fcubs.custom.handler.WSCallerHandler;

public class AQMDB implements MessageListener
{
static weblogic.logging.NonCatalogLogger logger;
static void sendResponseToQueue(String responseStr, String msgID) throws Exception
  {
	logger.info("AQMDB.sendResponseToQueue: Going to send response for msg ID " + msgID);
	logger.debug("AQMDB.sendResponseToQueue: responseStr " + responseStr);
	InitialContext l_ctx = new InitialContext();
	String l_ReplyQ = (String)l_ctx.lookup("java:comp/env/ReplyQueue");
	String l_ReplyQCF = (String)l_ctx.lookup("java:comp/env/ReplyQueueCF");
	logger.debug("AQMDB.sendResponseToQueue: l_ReplyQCF  = "+l_ReplyQCF);
	logger.debug("AQMDB.sendResponseToQueue: l_ReplyQ  = "+l_ReplyQ);
	QueueConnectionFactory jmsqconFactory = (QueueConnectionFactory) l_ctx.lookup(l_ReplyQCF);
	QueueConnection jmsqcon = jmsqconFactory.createQueueConnection();
	QueueSession jmsqsession = jmsqcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
	Queue jmsqueue = (Queue) l_ctx.lookup(l_ReplyQ);
	QueueSender jmsqsender = jmsqsession.createSender(jmsqueue);
	jmsqcon.start();
	TextMessage txtMsg = jmsqsession.createTextMessage();
	txtMsg.setText(responseStr);
	txtMsg.setJMSCorrelationID(msgID);
	jmsqsender.send(txtMsg);
	logger.debug("AQMDB.sendResponseToQueue: Sent Response to Queue");
	jmsqsender.close();
	jmsqsession.close();
	jmsqcon.close();
	logger.debug("AQMDB.sendResponseToQueue: Closed Queue Connection");
  }
public void onMessage(Message message)
  {
	TextMessage textMessage = (TextMessage) message;
	logger = new weblogic.logging.NonCatalogLogger("wscaller");
	logger.info("AQMDB.onMessage: Message Recieved from AD Queue");
	try {
		logger.debug("AQMDB.onMessage: Message  id " + message.getJMSMessageID());
		logger.debug("AQMDB.onMessage: Message Text "+ textMessage.getText());
		WSCallerHandler wsch = new WSCallerHandler();
		String responseStr = wsch.processRequest (textMessage.getText());
		logger.debug("AQMDB.onMessage: Got Response " + responseStr);
		sendResponseToQueue(responseStr,message.getJMSMessageID());
		logger.debug("AQMDB.onMessage: Sent Response to Reply Queue ");
	} catch (Exception e) {
		e.printStackTrace();
		logger.error("AQMDB.onMessage: Exception in AQMDB onMessage", e);
	}
	logger = null;
  }
}