package com.qingcheng.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/wxpay")
public class WxPayController {
    @Reference
    private OrderService orderService;
    @Reference
    private WxPayService wxPayService;

    @GetMapping("/createNative")
    public Map createNative(String orderId){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        //1.查询用户当前订单
        Order order = orderService.findById(orderId);
        Map map = new HashMap();
        //1.1校验数据
        if(order!=null){
            if(order.getPayStatus().equals("0")&&order.getOrderStatus().equals("0")&&username.equals(order.getUsername())){
                    map = wxPayService.wxPay(orderId, order.getPayMoney(), "http://qingchenglvyezong.utools.club/wxpay/notify.do");
                    return map;
                }
            else {
                return null;
            }

        }
        else {return null;}
    }

    /**
     * 微信支付回调
     * @param request
     */
    @RequestMapping("/notify")
    public void notifyPay(HttpServletRequest request){
        System.out.println("支付成功回调...");
        //1获取微信回调信息
        try {
            InputStream inputStream = request.getInputStream();//此处用inputStream接受
            ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
            int len=0;
            byte[] buffer=new byte[1024];
            while ( (len=inputStream.read(buffer))!= -1   ){
                outputStream.write(buffer,0,len);
            }
            outputStream.close();
            inputStream.close();
            String result =new String (outputStream.toByteArray(),"utf-8");//结果集转为string数组
            wxPayService.payNotify(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
