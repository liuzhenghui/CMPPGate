package com.zx.sms.handler.api.gate;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.math.RandomUtils;
import org.marre.wap.push.SmsMmsNotificationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zx.sms.codec.cmpp.msg.CmppDeliverRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppDeliverResponseMessage;
import com.zx.sms.codec.cmpp.msg.CmppReportRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitRequestMessage;
import com.zx.sms.codec.cmpp.msg.CmppSubmitResponseMessage;
import com.zx.sms.codec.cmpp.msg.Message;
import com.zx.sms.common.util.ChannelUtil;
//import com.zx.sms.common.util.MsgId;
import com.zx.sms.connect.manager.EndpointEntity;
import com.zx.sms.connect.manager.EventLoopGroupFactory;
import com.zx.sms.connect.manager.ExitUnlimitCirclePolicy;
import com.zx.sms.connect.manager.ServerEndpoint;
import com.zx.sms.handler.api.AbstractBusinessHandler;
import com.zx.sms.session.cmpp.SessionState;

/**
 * 
 * @author Lihuanghe(18852780@qq.com)
 *
 */
public class SessionConnectedHandler extends AbstractBusinessHandler {
	private static final Logger logger = LoggerFactory.getLogger(SessionConnectedHandler.class);

	private static int TOTLE = 100000;
	private int totleCnt =TOTLE;
	
	public int getTotleCnt() {
		return totleCnt;
	}
	public void setTotleCnt(int totleCnt) {
		this.totleCnt = totleCnt;
	}
	
	public SessionConnectedHandler(){
		TOTLE = 100000;
	}
	
	public SessionConnectedHandler(int t){
		TOTLE = t;
	}
	
	@Override
	public void userEventTriggered(final ChannelHandlerContext ctx, Object evt) throws Exception {

		if (evt == SessionState.Connect) {
			
			final EndpointEntity finalentity = (EndpointEntity) getEndpointEntity();
			final Channel ch = ctx.channel();
			EventLoopGroupFactory.INS.submitUnlimitCircleTask(new Callable<Boolean>() {
				private Message createTestReq(String content) {
					
					if (finalentity instanceof ServerEndpoint) {
						CmppDeliverRequestMessage msg = new CmppDeliverRequestMessage();
						msg.setDestId("13800138000");
						msg.setLinkid("0000");
//						msg.setMsgContent(sb.toString());
						msg.setMsgContent(content);
//						msg.setMsgId(new MsgId());
						msg.setRegisteredDelivery((short) 0);
						if (msg.getRegisteredDelivery() == 1) {
							msg.setReportRequestMessage(new CmppReportRequestMessage());
						}
						msg.setServiceid("10086");
						msg.setSrcterminalId(String.valueOf(System.nanoTime()));
						msg.setSrcterminalType((short) 1);
//						msg.setMsgContent(new SmsMmsNotificationMessage("http://www.baidu.com/abc/sfd",50*1024));
						
						return msg;
					} else {
						CmppSubmitRequestMessage msg = new CmppSubmitRequestMessage();
						msg.setDestterminalId(String.valueOf(System.nanoTime()));
						msg.setLinkID("0000");
						msg.setMsgContent("a");
//						msg.setMsgid(new MsgId());
						msg.setServiceId("10086");
						msg.setSrcId("10086");
//						msg.setMsgContent(new SmsMmsNotificationMessage("http://www.baidu.com/abc/sfd",50*1024));
						return msg;
					}
				}

				@Override
				public Boolean call() throws Exception{
					int cnt = RandomUtils.nextInt() & 0x1f;
					while(cnt-->0 && totleCnt>0) {
						ChannelFuture future = ctx.writeAndFlush(createTestReq(String.valueOf(totleCnt)) );
						if(future == null){
							break;
						}
						try{
//							future.sync();
							totleCnt--;
						}catch(Exception e){
							break;
						}
					}
					return true;
				}
			}, new ExitUnlimitCirclePolicy() {
				@Override
				public boolean notOver(Future future) {
					boolean over =   ch.isActive() && totleCnt > 0;
					if(!over)logger.info("========send over.============");
					return over;
				}
			},1);
		}
		ctx.fireUserEventTriggered(evt);

	}

	
	@Override
	public String name() {
		return "SessionConnectedHandler-Gate";
	}
	
	public SessionConnectedHandler clone() throws CloneNotSupportedException {
		SessionConnectedHandler ret = (SessionConnectedHandler) super.clone();
		ret.totleCnt = TOTLE;
		return ret;
	}

}
