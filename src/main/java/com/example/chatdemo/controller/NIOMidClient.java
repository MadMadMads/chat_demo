package com.example.chatdemo.controller;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.Set;

/**
 * @author: Luo
 * @description:
 * @time: 2019/11/29 17:54
 * Modified By:
 */
public class NIOMidClient {
    /*发送数据缓冲区*/
    private static ByteBuffer sBuffer = ByteBuffer.allocate(1024);
    /*接受数据缓冲区*/
    private static ByteBuffer rBuffer = ByteBuffer.allocate(1024);
    private static Selector selector;
    private static SocketChannel client;
    private static String receiveText;
    private static String sendText;
    private Charset cs = Charset.forName("utf-8");
    /*服务器地址*/
    private InetSocketAddress SERVER;
    private static  int count = 0;
    NIOMidClient() {
        SERVER = new InetSocketAddress("127.0.0.1",1234);
        init();
    }
    public void init() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            selector = Selector.open();
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
            //连接，但是还没有成功
            socketChannel.connect(SERVER);
            while (true) {
                //阻塞直到通道事件就绪
                selector.select();
                //取出所有的key
                Set<SelectionKey> selectors = selector.selectedKeys();
                for (SelectionKey selectionKey : selectors) {
                    handle(selectionKey);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void handle(SelectionKey selectionKey) throws Exception {
        //key中已有连接,accept为服务器连接客户端,connect为客服端连接服务器，当且仅当 readyops() & op_connect 为非零值时才返回 true
        if(selectionKey.isConnectable()) {
            client = (SocketChannel) selectionKey.channel();
            //发起了连接但是并未调用
            if (client.isConnectionPending()) {
                //再次调用connect进行连接
                client.finishConnect();
                String info =
                        "客户端:"+"["+client.socket().getInetAddress().toString().substring(1,client.socket().getInetAddress().toString().length())+":"+client.socket().getLocalPort() + "]启动";
                System.out.println(info);
                System.out.println(client.socket().getLocalAddress());
                System.out.println(client.socket().getRemoteSocketAddress());
                /*
                 * 启动线程一直监听客户端输入，有信息输入则发往服务器端
                 * 因为输入流是阻塞的，所以单独线程监听
                 */
                new Thread() {
                    @Override
                    public void run() {
                        while (true) {
                           /* clear 写缓存区调用
                            position = 0;limit = capacity;mark = -1;  有点初始化的味道，但是并不影响底层byte数组的内容*/
                            sBuffer.clear();
                            Scanner in = new Scanner(System.in);
                            sendText = in.nextLine();
                            try {
                                sBuffer.put(sendText.getBytes("utf-8"));
                                /*flip 读缓存区调用
                                limit = position;position = 0;mark = -1;  翻转，也就是让flip之后的position到limit这块区域变成之前的0到position这块，翻转就是将一个处于存数据状态的缓冲区变为一个处于准备取数据的状态*/
                                sBuffer.flip();
                                client.write(sBuffer);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }.start();
            }
            client.register(selector,SelectionKey.OP_READ);
        }else if (selectionKey.isReadable()){
            client = (SocketChannel) selectionKey.channel();
            rBuffer.clear();
            count = client.read(rBuffer);
            if (count>0) {
                rBuffer.flip();
                receiveText = String.valueOf(cs.decode(rBuffer).array());
                System.out.println(receiveText);
                client = (SocketChannel) selectionKey.channel();
                client.register(selector, SelectionKey.OP_READ);
            }
        }

    }

    public static void main(String[] args) {
        new NIOClient();
    }

}
