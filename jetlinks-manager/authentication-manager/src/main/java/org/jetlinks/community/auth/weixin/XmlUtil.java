package org.jetlinks.community.auth.weixin;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import com.thoughtworks.xstream.io.xml.XppDriver;
//import org.dom4j.Document;
//import org.dom4j.DocumentException;
//import org.dom4j.Element;
//import org.dom4j.io.SAXReader;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpRequest;

//import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import reactor.core.publisher.Mono;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

public class XmlUtil {
    /**
     * xml转map
     */
    public static Mono<Map<String, String>> parseXml(ServerHttpRequest request) {
        System.out.println("request:");
        return request.getBody().switchIfEmpty(Mono.error(new RuntimeException("R9equest body is empty")))  // 请求体为空时抛出错误doOnNext(body -> System.out.println("Received body chunk: " + body)) // 调试日志doOnSubscribe(subscription -> System.out.println("getBody subscribed"))
                      .map(dataBuffer -> {
                          System.out.println("ff");

                          byte[] bytes = new byte[dataBuffer.readableByteCount()];
                          dataBuffer.read(bytes);
                          System.out.println(new String(bytes, StandardCharsets.UTF_8));
                          return new String(bytes, StandardCharsets.UTF_8);
                      })
                      .reduce((prev, next) -> prev + next) // 拼接分块传输的内容
                      .flatMap(XmlUtil::xmlToMap); // 将 XML 转换为 Map
    }
    public static Mono<Map<String, String>> xmlToMap(String xml) {
        try {
            System.out.println("xml:"+xml);
            // 解析 XML 字符串为 DOM
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xml)));

            // 遍历 XML 元素，存入 Map
            Map<String, String> resultMap = new HashMap<>();
            Element root = document.getDocumentElement();
            NodeList nodeList = root.getChildNodes();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    resultMap.put(node.getNodeName(), node.getTextContent());
                }
            }

            return Mono.just(resultMap);
        } catch (Exception e) {
            return Mono.error(new RuntimeException("XML 解析失败", e));
        }
    }
//    public static Mono<Map<String, String>> xmlToMap(ServerHttpRequest request) {
//        return request.getBody()  // Get the request body as Flux<DataBuffer>
//                      .reduce(DataBuffer::write)  // Combine the chunks into a single DataBuffer
//                      .flatMap(dataBuffer -> {
//                          try {
//                              byte[] bytes = new byte[dataBuffer.readableByteCount()];
//                              dataBuffer.read(bytes); // Convert DataBuffer to byte array
//                              System.out.println(new String(bytes, StandardCharsets.UTF_8));
//                              // Parse the XML using SAXReader
//                              SAXReader reader = new SAXReader();
//                              ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
//                              Document doc = reader.read(byteArrayInputStream);
//
//                              Element root = doc.getRootElement();
//                              @SuppressWarnings("unchecked")
//                              List<Element> list = (List<Element>) root.elements();
//
//                              HashMap<String, String> map = new HashMap<>();
//                              for (Element e : list) {
//                                  map.put(e.getName(), e.getText());
//                              }
//
//                              // Return the parsed data as a HashMap
////                              return map;
//                              return Mono.just(map);
//                          } catch (DocumentException e) {
//                              return Mono.error(new RuntimeException("Failed to parse XML", e));
//                          }
//                      });
//    }


    public static WechatNotifyRequestVO fromXML(String xml, Class clazz, Class... childClazz) {
        XStream xmlStream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_")));

        XStream.setupDefaultSecurity(xmlStream);

        xmlStream.processAnnotations(new Class[]{clazz});

        xmlStream.processAnnotations(childClazz);

        xmlStream.allowTypes(new Class[]{clazz});

        xmlStream.allowTypes(childClazz);

        xmlStream.ignoreUnknownElements();

        Object result = xmlStream.fromXML(xml);

        return (WechatNotifyRequestVO) result;

    }
    /**

     * 对象组装成xml

     *

     * @param object 要转换的对象

     * @return xml文档

     */

    public static String toXML(Object object) {
//转换成XML

        XStream xmlStream = new XStream(new XppDriver(new XmlFriendlyNameCoder("_-", "_")));

        XStream.setupDefaultSecurity(xmlStream);

        xmlStream.processAnnotations(new Class[]{object.getClass()});

        xmlStream.allowTypes(new Class[]{object.getClass()});

        return xmlStream.toXML(object);

    }
}

