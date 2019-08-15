/*
 *       Copyright© (2019) WeBank Co., Ltd.
 *
 *       This file is part of weidentity-http-service.
 *
 *       weidentity-http-service is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weidentity-http-service is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weidentity-http-service.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.http.service.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartboot.socket.Protocol;
import org.smartboot.socket.extension.decoder.FixedLengthFrameDecoder;
import org.smartboot.socket.transport.AioSession;

import java.nio.ByteBuffer;

/**
 * Modified from smart-socket.
 */
public class FixedLengthProtocol implements Protocol<String> {

    private static final Logger logger = LoggerFactory.getLogger(FixedLengthProtocol.class);
    private static final int INT_BYTES = 4;//int类型的字节长度

    @Override
    public String decode(ByteBuffer readBuffer, AioSession<String> session) {
        if (session.getAttachment() == null
            && readBuffer.remaining() < INT_BYTES) {//首次解码不足四字节，无法知晓消息长度
            logger.info("SmartSocket 首次解码不足四字节，无法知晓消息长度");
            return null;
        }
        FixedLengthFrameDecoder fixedLengthFrameDecoder;
        if (session.getAttachment() != null) {
            fixedLengthFrameDecoder = session.getAttachment();
        } else {
            int length = readBuffer.getInt();//获得消息体长度
            fixedLengthFrameDecoder = new FixedLengthFrameDecoder(length);//构建指定长度的临时缓冲区
            session.setAttachment(fixedLengthFrameDecoder);//缓存临时缓冲区
        }

        if (!fixedLengthFrameDecoder.decode(readBuffer)) {
            logger.info("SmartSocket 已读取的数据不足length，返回null");
            return null;//已读取的数据不足length，返回null
        }
        //数据读取完毕
        ByteBuffer fullBuffer = fixedLengthFrameDecoder.getBuffer();
        byte[] bytes = new byte[fullBuffer.remaining()];
        fullBuffer.get(bytes);
        session.setAttachment(null);//释放临时缓冲区
        return new String(bytes);
    }

    public static ByteBuffer encode(String msg) {
        logger.info("SmartSocket 开始编码");
        byte[] bytes = msg.getBytes();
        ByteBuffer buffer = ByteBuffer.allocate(INT_BYTES + bytes.length);
        buffer.putInt(bytes.length);//消息头
        buffer.put(bytes);//消息体
        buffer.flip();
        return buffer;
    }
}