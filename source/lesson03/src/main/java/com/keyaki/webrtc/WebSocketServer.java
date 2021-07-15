package com.keyaki.webrtc;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ServerEndpoint(value = "/test/demo")
@Component
public class WebSocketServer {

	private static AtomicInteger onlineCount = new AtomicInteger(0);
	private static Map<String, Session> clients = new ConcurrentHashMap<>();
	
	@OnOpen
    public void onOpen(Session session) {
        onlineCount.incrementAndGet(); 
        clients.put(session.getId(), session);
        log.info("onOpen from {}，count {}", session.getId(), onlineCount.get());
    }

    @OnClose
    public void onClose(Session session) {
        onlineCount.decrementAndGet(); 
        clients.remove(session.getId());
        log.info("onClose from {}，count {}", session.getId(), onlineCount.get());
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("server received from {}, message:{}", session.getId(), message);
        try {
        	boolean isSend = false;
        	@SuppressWarnings("unchecked")
			HashMap<String,String> myMessage = JSON.parseObject(message, HashMap.class);
        	
        	if(myMessage != null) {
            	if (myMessage.get("candidate")!=null) {
            		isSend = true;
            	}else if ("offer".equals(myMessage.get("type")) || "answer".equals(myMessage.get("type"))) {
            		isSend = true;
            	}       		
        	}
        	if(isSend) {
                for (Map.Entry<String, Session> sessionEntry : clients.entrySet()) {
                    Session toSession = sessionEntry.getValue();
                    if (!session.getId().equals(toSession.getId())) {
                        log.info("server send to {}, message: {}", toSession.getId(), message);
                        toSession.getBasicRemote().sendText(message);
                    }
                }                        		
        	}
        } catch (Exception e) {
            log.error("parse error {}", e);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("error");
        error.printStackTrace();
    }

    private void sendMessage(String message, Session toSession) {
        try {
            log.info("服务端给客户端[{}]发送消息[{}]", toSession.getId(), message);
            toSession.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error("服务端发送消息给客户端失败：{}", e);
        }
    }	
}
