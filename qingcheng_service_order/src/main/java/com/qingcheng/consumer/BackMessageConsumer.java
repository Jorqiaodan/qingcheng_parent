package com.qingcheng.consumer;

import com.alibaba.fastjson.JSON;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.pojo.order.Order;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.StockBackService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class BackMessageConsumer implements MessageListener {

    @Autowired
    private OrderService orderService;
    @Autowired
    private WxPayService wxPayService;
    @Autowired
    private  OrderMapper orderMapper;
    //处理超时未支付订单
    public void onMessage(Message message) {
        try {
            System.out.println("handle unpay order...");
            //提取消息
            String orderId = new String(message.getBody());
         //先查询业务系统的订单状态，
            Order order = orderService.findById(orderId);
            if(order!=null&&order.getPayStatus().equals("0")){
                //如果订单状态为未支付，调用微信支付查询订单的方法查询。
                Map map=wxPayService.queryPayStatus(orderId);
                //结果是未支付，调用关闭订单的业务逻辑方法
                if(!"SUCCESS ".equals(map.get("trade_state"))){
                    //关闭微信订单
                    wxPayService.closePay(map);
                    //关闭order订单
                    orderService.closeUnPay(order);
                }else {
                    //如果返回结果是已支付，实现补偿操,修改订单的状态
                    //补偿order
                    order.setPayStatus("1");
                    order.setUpdateTime(new Date());
                    orderMapper.updateByPrimaryKeySelective(order);
                    //记录日志
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            //记录日志，之后人工干预
        }
    }
}
