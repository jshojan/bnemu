package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;

public class GetChannelListHandler extends BncsPacketHandler {
    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_GETCHANNELLIST;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
//        var productId = input.readBytes(4);

        var output = new BncsPacketBuffer()
                .writeString("bnemu")
                .writeString("The Void")
                .writeString("");

        ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_GETCHANNELLIST, output));
    }
}
