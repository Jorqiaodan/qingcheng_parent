package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.wxpay.sdk.Config;
import com.github.wxpay.sdk.WXPayRequest;
import com.github.wxpay.sdk.WXPayUtil;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.pojo.order.OrderLog;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.order.WxPayService;
import com.qingcheng.util.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class WxPayServiceImpl implements WxPayService {
    @Autowired
    private Config config;
    @Override
    public Map wxPay(String orderId, Integer money, String notifyUrl) {
        try {
            //1.封装请求参数
            Map<String,String> map=new HashMap();
            map.put("appid",config.getAppID());//公众账号ID
            map.put("mch_id",config.getMchID());//商户号
            map.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
            map.put("body","青橙");//商品描述
            map.put("out_trade_no",orderId);//订单号
            map.put("total_fee",money+"");//金额
            map.put("spbill_create_ip","127.0.0.1");//终端IP
            map.put("notify_url",notifyUrl);//回调地址
            map.put("trade_type","NATIVE");//交易类型
            String xmlParam  = WXPayUtil.generateSignedXml(map, config.getKey());
            System.out.println("参数："+xmlParam);

            //2.发送请求
            WXPayRequest wxPayRequest=new WXPayRequest(config);
            String xmlResult = wxPayRequest.requestWithCert("/pay/unifiedorder", null, xmlParam, false);
            System.out.println("结果："+xmlResult);

            //3.解析返回结果
            Map<String, String> mapResult = WXPayUtil.xmlToMap(xmlResult);

            Map m=new HashMap();
            m.put("code_url", mapResult.get("code_url"));
            m.put("total_fee",money+"");
            m.put("out_trade_no",orderId);
            return m;

        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap();
        }

    }
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Reference
    private OrderService orderService;
    @Autowired
    private OrderLogMapper orderLogMapper;
    @Override
    public void payNotify(String xml) {
        //1.解析xml
        try {
            Map<String, String> map = WXPayUtil.xmlToMap(xml);
            //1.1校验签名
            boolean signatureValid = WXPayUtil.isSignatureValid(map, config.getKey());
            if(signatureValid){
                //1.2校验返回状态、
                if("SUCCESS".equals(map.get("result_code"))){
                    //2.修改订单状态
                    orderService.updatePayStatus(map.get("out_trade_no"),map.get("transaction_id"));
                    //3.向消息队列中发送消息
                    rabbitTemplate.convertAndSend("wxpaynotify","",map.get("out_trade_no"));//订单号放里面
                }else {
                    OrderLog orderLog=new OrderLog();
                    IdWorker idWorker=new IdWorker();
                    orderLog.setId(idWorker.nextId()+"");//id
                    orderLog.setOperater("system");
                    orderLog.setOperateTime(new Date());
                    orderLog.setOrderId(Long.valueOf(map.get("out_trade_no")));
                    orderLog.setOrderStatus("0");
                    orderLog.setRemarks("微信支付失败");
                    orderLogMapper.insertSelective(orderLog);
                }

            }else {
                //记录日志
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map queryPayStatus(String orderId) {
        try {
            //1.封装参数
            Map<String,String> param=new HashMap<>();
            param.put("appid",config.getAppID());
            param.put("mch_id",config.getMchID());
            param.put("out_trade_no",orderId);
            param.put("nonce_str",WXPayUtil.generateNonceStr());
            String xmlParam = WXPayUtil.generateSignedXml(param, config.getKey());

            //2.调用接口
            WXPayRequest wxPayRequest=new WXPayRequest(config);
            String result = wxPayRequest.requestWithCert("/pay/orderquery", null, xmlParam, false);

            //3.解析结果
            Map<String, String> map = WXPayUtil.xmlToMap(result);
            return map;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }



    /**
     * 关闭支付
     * @param  map
     */
    @Override
    public void closePay(Map map) {
        try {
            //1.封装参数
            Map<String,String> param=new HashMap<>();
            param.put("appid",config.getAppID());
            param.put("mch_id",config.getMchID());
            param.put("out_trade_no",map.get("out_trade_no")+"");
            param.put("nonce_str",WXPayUtil.generateNonceStr());
            //todo 关闭订单失败了
            param.put("sign",config.getKey());
            String xmlParam = WXPayUtil.mapToXml(param);
            //2.调用接口 关闭订单
            WXPayRequest wxPayRequest=new WXPayRequest(config);
            String result = wxPayRequest.requestWithCert("/pay/closeorder ", null, xmlParam, false);

            //3.解析结果 暂时不写返回值
            Map<String, String> map1 = WXPayUtil.xmlToMap(result);

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


}
