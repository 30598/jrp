/**
 * 与Ngrokd建立控制连接，并交换控制信息
 */
package jrp.client.connect;

import java.io.IOException;
import java.net.Socket;

import jrp.Protocol;
import jrp.client.Context;
import jrp.client.Message;
import jrp.client.model.Tunnel;
import jrp.log.Logger;
import jrp.socket.PacketReader;
import jrp.socket.SocketHelper;
import jrp.util.GsonUtil;

public class ControlConnect implements Runnable
{
	private Socket socket;
	private Context context;
	private Logger log;

	public ControlConnect(Socket socket, Context context)
	{
		this.socket = socket;
		this.context = context;
		this.log = context.getLog();
	}

	@Override
	public void run()
	{
		try(Socket socket = this.socket)
		{
			String clientId = null;
			SocketHelper.sendpack(socket, Message.Auth());
			PacketReader pr = new PacketReader(socket);
			while(true)
			{
				String msg = pr.read();
				if(msg == null)
				{
					break;
				}
				log.log("收到服务器信息：" + msg);
				Protocol protocol = GsonUtil.toBean(msg, Protocol.class);
				if("ReqProxy".equals(protocol.Type))
				{
					try
					{
						Socket remoteSocket = SocketHelper.newSSLSocket(context.getServerHost(), context.getServerPort());
						Thread thread = new Thread(new ProxyConnect(remoteSocket, clientId, context));
						thread.setDaemon(true);
						thread.start();
					}
					catch(Exception e)
					{
						log.err(e.getMessage());
					}
				}
				else if("NewTunnel".equals(protocol.Type))
				{
					if(protocol.Payload.Error == null || "".equals(protocol.Payload.Error))
					{
						log.log("管道注册成功：%s:%d", context.getServerHost(), protocol.Payload.RemotePort);
					}
					else
					{
						log.err("管道注册失败：" + protocol.Payload.Error);
						try{Thread.sleep(30);}catch(InterruptedException e){}
					}
				}
				else if("AuthResp".equals(protocol.Type))
				{
					clientId = protocol.Payload.ClientId;
					SocketHelper.sendpack(socket, Message.Ping());
					for(Tunnel tunnel : context.getTunnelList())
					{
						SocketHelper.sendpack(socket, Message.ReqTunnel(tunnel));
					}
				}
			}
		}
		catch(IOException e)
		{
			log.err(e.getMessage());
		}
	}
}
