package org.bnemu.bncs.handler;

import io.netty.channel.ChannelHandlerContext;
import org.bnemu.bncs.net.packet.BncsPacket;
import org.bnemu.bncs.net.packet.BncsPacketBuffer;
import org.bnemu.bncs.net.packet.BncsPacketId;
import org.bnemu.core.auth.SelectedCharacterStore;
import org.bnemu.core.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnterChatHandler extends BncsPacketHandler {
    private static final Logger logger = LoggerFactory.getLogger(EnterChatHandler.class);

    private final SessionManager sessions;
    private final SelectedCharacterStore selectedCharStore;

    public EnterChatHandler(SessionManager sessions, SelectedCharacterStore selectedCharStore) {
        this.sessions = sessions;
        this.selectedCharStore = selectedCharStore;
    }

    @Override
    public BncsPacketId bncsPacketId() {
        return BncsPacketId.SID_ENTERCHAT;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, BncsPacket packet) {
        var input = packet.payload();
        var username = input.readString();

        // Get product code from session (set by AuthInfoHandler)
        // Read product before the username guard — W3 clients send empty username/statstring
        String product = sessions.get(ctx.channel(), "product");
        if (product == null || product.isEmpty()) {
            product = "RATS"; // Default to StarCraft
        }

        if ((username == null || username.isEmpty()) && !isW3Product(product)) {
            var error = new BncsPacketBuffer().writeString("You must provide a username.");
            ctx.writeAndFlush(new BncsPacket(BncsPacketId.SID_MESSAGEBOX, error));
            return;
        }

        // Get account name from session — prefer the explicit "accountName" attribute
        // because getUsername() is overwritten with the channel display name
        // (e.g., "CharName*AccountName" for D2 realm chars) on each SID_ENTERCHAT.
        String accountName = sessions.get(ctx.channel(), "accountName");
        if (accountName == null) {
            accountName = sessions.getUsername(ctx.channel());
        }
        if (accountName == null) {
            accountName = username;
        }

        String statstring;
        // channelDisplayName is what appears in chat events (EID_SHOWUSER, EID_JOIN, EID_TALK, etc.)
        String channelDisplayName = username;

        // Check for D2 products and realm character
        if (isD2Product(product) && selectedCharStore != null) {
            var selectedChar = selectedCharStore.getSelectedCharacter(accountName);
            if (selectedChar != null) {
                // Don't clear — CharLogonHandler overwrites on new selection,
                // and the 5-minute expiry in SelectedCharacterStore handles cleanup.
                // Clearing here caused re-entering chat (after creating a new char)
                // to fall into the "Open Battle.net" branch due to a race between
                // MCP_CHARLOGON (D2CS) and SID_ENTERCHAT (BNCS).

                // PvPGN format: channel display name is "CharName*AccountName"
                channelDisplayName = selectedChar.characterName() + "*" + accountName;
                // D2 statstring format: ProductID + RealmName + ',' + CharacterName + ',' + PortraitBytes
                // No comma between product and realm - product is concatenated directly
                statstring = product + selectedChar.realmName() + "," +
                             selectedChar.characterName() + "," + selectedChar.buildStatstring();

                logger.info("SID_ENTERCHAT: {} entering as {} on realm {} (channel name: {})",
                    accountName, selectedChar.characterName(), selectedChar.realmName(), channelDisplayName);
            } else {
                // D2 without realm character - Open Battle.net (just account name + product ID)
                statstring = product;
                logger.info("SID_ENTERCHAT: {} entering as Open Battle.net D2", accountName);
            }
        } else if (isW3Product(product)) {
            // W3 statstring format per PvPGN conn_update_w3_playerinfo():
            // "%s %1u%c3W %u" = revtag, tier+raceicon+"3W", level
            // Default: tier 1, Random race, level 0, no clan
            channelDisplayName = accountName;
            statstring = product + " 1R3W 0";
            logger.info("SID_ENTERCHAT: {} entering as Warcraft III user", accountName);
        } else {
            // Non-D2, non-W3 product - generic statstring
            statstring = product + " 0 0 0 0 0 0 0 0 " + product;
        }

        // Store channel display name and statstring in session for use in channel events
        sessions.setUsername(ctx.channel(), channelDisplayName);
        sessions.set(ctx.channel(), "statstring", statstring);
        sessions.set(ctx.channel(), "accountName", accountName);

        // SID_ENTERCHAT response per PvPGN:
        // Field 1 (Unique Name): account name (not character name)
        // Field 2 (Statstring): product + realm info
        // Field 3 (Account Name): account name
        var output = new BncsPacketBuffer()
            .writeString(accountName)
            .writeString(statstring)
            .writeString(accountName);
        send(ctx, output);
    }

    private boolean isD2Product(String product) {
        return "VD2D".equals(product) || "PX2D".equals(product);
    }

    private boolean isW3Product(String product) {
        return "3RAW".equals(product) || "PX3W".equals(product);
    }
}
