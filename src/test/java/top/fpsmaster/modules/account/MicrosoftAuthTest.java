package top.fpsmaster.modules.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import top.fpsmaster.exception.AccountException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MicrosoftAuthTest {

    @Test
    void usesLauncherBuiltinClientId() {
        assertEquals("057064c6-d180-43df-b010-834b4571532f", MicrosoftAuth.DEFAULT_CLIENT_ID);
        assertEquals(MicrosoftAuth.DEFAULT_CLIENT_ID, MicrosoftAuth.clientId());
    }

    @Test
    void dashesMojangProfileIds() {
        assertEquals("3e8e30dd-e200-3b12-bff7-5910bb375519",
                MicrosoftAuth.dashedUuid("3e8e30dde2003b12bff75910bb375519"));
        assertEquals("3e8e30dd-e200-3b12-bff7-5910bb375519",
                MicrosoftAuth.dashedUuid("3E8E30DD-E200-3B12-BFF7-5910BB375519"));
    }

    @Test
    void parsesDeviceLoginPayload() throws AccountException {
        MicrosoftAuth.DeviceLogin login = MicrosoftAuth.parseDeviceLogin(
                "{\"device_code\":\"dc\",\"user_code\":\"ABCD-EFGH\","
                        + "\"verification_uri\":\"https://microsoft.com/link\","
                        + "\"verification_uri_complete\":\"https://microsoft.com/link?otc=ABCD-EFGH\","
                        + "\"expires_in\":900,\"interval\":5}");
        assertEquals("dc", login.deviceCode);
        assertEquals("ABCD-EFGH", login.userCode);
        assertEquals("https://microsoft.com/link?otc=ABCD-EFGH", login.browserUrl());
        assertEquals(5, login.interval);
    }

    @Test
    void classifiesDeviceTokenErrors() {
        assertEquals("pending", MicrosoftAuth.classifyTokenError("authorization_pending"));
        assertEquals("slow_down", MicrosoftAuth.classifyTokenError("slow_down"));
        assertEquals("denied", MicrosoftAuth.classifyTokenError("authorization_declined"));
        assertEquals("expired", MicrosoftAuth.classifyTokenError("expired_token"));
        assertEquals("failed", MicrosoftAuth.classifyTokenError("invalid_grant"));
    }

    @Test
    void detectsJavaEntitlements() {
        JsonObject owned = new JsonParser().parse(
                "{\"items\":[{\"name\":\"product_minecraft\"},{\"name\":\"game_minecraft\"}]}")
                .getAsJsonObject();
        JsonObject empty = new JsonParser().parse("{\"items\":[]}").getAsJsonObject();
        assertTrue(MicrosoftAuth.hasMinecraftLicense(owned));
        assertFalse(MicrosoftAuth.hasMinecraftLicense(empty));
    }
}
