package task2;

import com.ibm.mq.MQQueue;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Hashtable;


public class Task2 extends Functions {
    private static MQQueue queue1 = null;
    private static MQQueue queue2 = null;
    private static MQQueue queue3 = null;
    private static MQPutMessageOptions pmo = new MQPutMessageOptions();
    private static MQGetMessageOptions gmo = new MQGetMessageOptions();
    private static MQQueueManager queueManager = null;
    private static MQMessage mqMesIn = new MQMessage();
    private static MQMessage mqMesOut = new MQMessage();
    private static MQMessage mqMesResp = new MQMessage();
    private static final String queueIn = "TASK3.IN";
    private static final String queueOut = "TASK3.OUT";
    private static final String queueResp = "TASK3.RESP";

    @Before
    public void init() throws MQException {
        Hashtable properties = new Hashtable<String, Object>();
        properties.put(MQConstants.HOST_NAME_PROPERTY, "192.168.15.74");
        properties.put(MQConstants.PORT_PROPERTY, 4401);
        properties.put(MQConstants.CHANNEL_PROPERTY, "SYSTEM.DEF.SVRCONN");

        queueManager = new MQQueueManager("QMTESTERS1", properties);
        queue1 = queueManager.accessQueue(queueIn, MQConstants.MQOO_OUTPUT);
        queue2 = queueManager.accessQueue(queueOut, MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INPUT_AS_Q_DEF);
        queue3 = queueManager.accessQueue(queueResp, MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INPUT_AS_Q_DEF);

        gmo.options = MQConstants.MQGMO_WAIT | MQConstants.MQGMO_CONVERT;
        gmo.waitInterval = 10000;
        gmo.matchOptions = MQConstants.MQMO_MATCH_CORREL_ID;

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setNamespaceAware(true);
    }

    @After
    public void close() {
        try {
            if (queue1 != null)
                queue1.close();
            if (queue2 != null)
                queue2.close();
            if (queue3 != null)
                queue3.close();
            if (queueManager != null)
                queueManager.close();
        } catch (MQException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test1_valid() {
        try {
            String input = readFileAsString("/Rest-Assured/target/templates/task2/ex3_msg1.xml");
            System.out.println(input);
            mqMesIn.writeString(input);
            mqMesIn.setStringProperty("System", "DMS");
            mqMesIn.setStringProperty("TypeRequest", "loan");
            System.out.println("***Отправляем сообщение***");
            System.out.println(mqMesIn);
            queue1.put(mqMesIn, pmo);
            queueManager.commit();

            System.out.println("***Получаем сообщение Out***");
            queue2.get(mqMesOut, gmo);
            String actualMesOut = mqMesOut.readStringOfCharLength(mqMesOut.getMessageLength());
            System.out.println(actualMesOut);
            String actualSystem = mqMesOut.getStringProperty("System");
            String actualTypeRequest = mqMesOut.getStringProperty("TypeRequest");
            String actualValidityCheck = mqMesOut.getStringProperty("ValidityCheck");
            System.out.println("***Проверяем заголовки сообщения Out***");
            Assert.assertEquals("ESB", actualSystem);
            Assert.assertEquals("loan", actualTypeRequest);
            Assert.assertEquals("OK", actualValidityCheck);
            String actualmqMesOutNoTimeStamp = actualMesOut.replaceAll("<TimeStamp>(.*)</TimeStamp>", "<TimeStamp></TimeStamp>");
            String expectedMesOut = readFileAsString("/Rest-Assured/target/templates/task2/ex3_out_response_msg_1.xml");
            Assert.assertEquals(expectedMesOut, actualmqMesOutNoTimeStamp);

            System.out.println("***Получаем сообщение Resp***");
            queue3.get(mqMesResp, gmo);
            String resp = mqMesResp.readStringOfByteLength(mqMesResp.getDataLength());
            System.out.println(resp);
            String actualError = mqMesResp.getStringProperty("ERROR");
            System.out.println("***Проверяем заголовки сообщения Resp***");
            Assert.assertEquals("NULL", actualError);

            Document doc = convertStringToXMLDocument(resp);
            String expr = "/Transaction-Result/Validation-Result";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String validationResult = getStringValueFromXML(doc, xpath, expr);

            System.out.println("***Проверяем наличие статуса Valid в сообщении***");
            Assert.assertTrue(validationResult.matches("org.custommonkey.xmlunit.Validator@\\w{8}:\\[valid\\]"));

        } catch (IOException | MQException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test2_emptyHeaders() {
        try {
            String input = readFileAsString("/Rest-Assured/target/templates/task2/ex3_msg1.xml");
            System.out.println(input);
            mqMesIn.writeString(input);
            System.out.println("***Отправляем сообщение***");
            System.out.println(mqMesIn);
            queue1.put(mqMesIn, pmo);
            queueManager.commit();

            System.out.println("***Получаем сообщение Resp***");
            queue3.get(mqMesResp, gmo);
            String resp = mqMesResp.readStringOfByteLength(mqMesResp.getDataLength());
            System.out.println(resp);
            String actualError = mqMesResp.getStringProperty("ERROR");
            System.out.println("***Проверяем заголовки сообщения Resp***");
            Assert.assertEquals("Invalid or Empty Header", actualError);

            Document doc = convertStringToXMLDocument(resp);
            String expr = "/Transaction-Result/Validation-Result";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String validationResult = getStringValueFromXML(doc, xpath, expr);
            System.out.println("***Проверяем наличие ошибки в сообщении***");
            Assert.assertEquals("Parsing Error: Header values either invalid or empty", validationResult);
        } catch (IOException | MQException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test3_wrongSystemHeader() {
        try {
            String input = readFileAsString("/Rest-Assured/target/templates/task2/ex3_msg1.xml");
            System.out.println(input);
            mqMesIn.writeString(input);
            mqMesIn.setStringProperty("System", "WRONG");
            mqMesIn.setStringProperty("TypeRequest", "client");
            System.out.println("***Отправляем сообщение***");
            System.out.println(mqMesIn);
            queue1.put(mqMesIn, pmo);
            queueManager.commit();

            System.out.println("***Получаем сообщение Resp***");
            queue3.get(mqMesResp, gmo);
            String resp = mqMesResp.readStringOfByteLength(mqMesResp.getDataLength());
            System.out.println(resp);
            String actualError = mqMesResp.getStringProperty("ERROR");
            System.out.println("***Проверяем заголовки сообщения Resp***");
            Assert.assertEquals("Invalid or Empty Header", actualError);

            Document doc = convertStringToXMLDocument(resp);
            String expr = "/Transaction-Result/Validation-Result";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String validationResult = getStringValueFromXML(doc, xpath, expr);
            System.out.println("***Проверяем наличие ошибки в сообщении***");
            Assert.assertEquals("Parsing Error: Header values either invalid or empty", validationResult);
        } catch (IOException | MQException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test4_wrongTypeRequestHeader() {
        try {
            String input = readFileAsString("/Rest-Assured/target/templates/task2/ex3_msg1.xml");
            System.out.println(input);
            mqMesIn.writeString(input);
            mqMesIn.setStringProperty("System", "CRM");
            mqMesIn.setStringProperty("TypeRequest", "WRONG");
            System.out.println("***Отправляем сообщение***");
            System.out.println(mqMesIn);
            queue1.put(mqMesIn, pmo);
            queueManager.commit();

            System.out.println("***Получаем сообщение Resp***");
            queue3.get(mqMesResp, gmo);
            String resp = mqMesResp.readStringOfByteLength(mqMesResp.getDataLength());
            System.out.println(resp);
            String actualError = mqMesResp.getStringProperty("ERROR");
            System.out.println("***Проверяем заголовки сообщения Resp***");
            Assert.assertEquals("Invalid or Empty Header", actualError);

            Document doc = convertStringToXMLDocument(resp);
            String expr = "/Transaction-Result/Validation-Result";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String validationResult = getStringValueFromXML(doc, xpath, expr);
            System.out.println("***Проверяем наличие ошибки в сообщении***");
            Assert.assertEquals("Parsing Error: Header values either invalid or empty", validationResult);
        } catch (IOException | MQException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void test5_invalid() {
        try {
            String input = readFileAsString("/Rest-Assured/target/templates/task2/ex3_msg1.xml");
            String wrong = "WRONG";
            String wrongInput = input.replaceAll("<Currecny>(.*)</Currecny>", "<Currecny>" + wrong + "</Currecny>");
            System.out.println(wrongInput);
            mqMesIn.writeString(wrongInput);
            mqMesIn.setStringProperty("System", "ABS");
            mqMesIn.setStringProperty("TypeRequest", "account");
            System.out.println("***Отправляем сообщение***");
            System.out.println(mqMesIn);
            queue1.put(mqMesIn, pmo);
            queueManager.commit();

            System.out.println("***Получаем сообщение Resp***");
            queue3.get(mqMesResp, gmo);
            String resp = mqMesResp.readStringOfByteLength(mqMesResp.getDataLength());
            System.out.println(resp);
            String actualError = mqMesResp.getStringProperty("ERROR");
            System.out.println("***Проверяем заголовки сообщения Resp***");
            Assert.assertEquals("NULL", actualError);

            Document doc = convertStringToXMLDocument(resp);
            String expr = "/Transaction-Result/Validation-Result";
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            String validationResult = getStringValueFromXML(doc, xpath, expr);
            System.out.println("***Проверяем наличие ошибки в сообщении***");
            boolean isValid = validationResult.contains("is not valid");
            Assert.assertTrue(isValid);
        } catch (IOException | MQException e) {
            e.printStackTrace();
        }
    }
}