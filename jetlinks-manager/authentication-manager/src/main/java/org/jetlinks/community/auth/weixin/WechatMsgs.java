package org.jetlinks.community.auth.weixin;


import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class WechatMsgs {

//    @Autowired
//    private LitemallUserService userService;

//    @Autowired
//    private LitemallGroupService litemallGroupService;

//    public void Send(Integer userId, String name, String retailPrice, String starttime, String endtime) {
//
//        // 接口地址
//        String sendMsgApi = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + "你公众号的token";
//
//        //跟据用户id查询订阅过的商家有几个，商家id对应用户表里面的id，
//        //我这边是子查询所以直接把公众号的用户给查出来了, 然后循环
////        List<LitemallUnionid> unionids = litemallGroupService.cxunionid(userId);
////
////        LitemallUser user = userService.findById(userId);
//        //循环公众号列表
//        for (LitemallUnionid u : unionids) {
//
//            //openId
////            String toUser = "" + u.getOpenid() + "";
//
//            //消息模板ID
//            String template_id = "wYhlJQXV4Zr2-lmf7gOMYXhDlWB_wilklbnSq7E2cys";
//            //整体参数map
//            Map<String, Object> paramMap = new HashMap<String, Object>();
//
//            Map<String, Object> xcx = new HashMap<String, Object>();
//
//            //消息主题显示相关map
//            Map<String, Object> dataMap = new HashMap<String, Object>();
//            //根据自己的模板定义内容和颜色
////            dataMap.put("first", new DataEntity("您订阅的" + user.getNickname() + "社群发布了新商品", "#173177"));
////            dataMap.put("Pingou_ProductName", new DataEntity(name, "#0f0f0f"));
////            dataMap.put("Weixin_ID", new DataEntity(user.getNickname(), "#173177"));
////            dataMap.put("Remark", new DataEntity("点击查看详情进入主页", "#F78934"));
//            paramMap.put("touser", toUser);
//            paramMap.put("template_id", template_id);
//            paramMap.put("data", dataMap);
////            xcx.put("appid","小程序的appid"); 这个是跳转
////            xcx.put("pagepath","pages/index/index");
////            paramMap.put("miniprogram",xcx);
//            //需要实现跳转网页的，可以添加下面一行代码实现跳转
//            // paramMap.put("url","http://xxxxxx.html");
//
//            System.out.println(doGetPost(sendMsgApi, "POST", paramMap));
//
//        }
//
//
//    }


    /**
     * 调用接口 post
     *
     * @param apiPath
     */
    public static String doGetPost(String apiPath, String type, Map<String, Object> paramMap) {
        OutputStreamWriter out = null;
        InputStream is = null;
        String result = null;
        try {
            URL url = new URL(apiPath);// 创建连接
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(type); // 设置请求方式
            connection.setRequestProperty("Accept", "application/json"); // 设置接收数据的格式
            connection.setRequestProperty("Content-Type", "application/json"); // 设置发送数据的格式
            connection.connect();
            if (type.equals("POST")) {
                out = new OutputStreamWriter(connection.getOutputStream(), "UTF-8"); // utf-8编码
                out.append(JSON.toJSONString(paramMap));
                out.flush();
                out.close();
            }
            // 读取响应
            is = connection.getInputStream();
            int length = (int) connection.getContentLength();// 获取长度
            if (length != -1) {
                byte[] data = new byte[length];
                byte[] temp = new byte[512];
                int readLen = 0;
                int destPos = 0;
                while ((readLen = is.read(temp)) > 0) {
                    System.arraycopy(temp, 0, data, destPos, readLen);
                    destPos += readLen;
                }
                result = new String(data, "UTF-8"); // utf-8编码
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}