package top.fpsmaster.modules.client.api.model;

/**
 * One entry of {@code GET /api/v1/launcher/servers} — the official featured/partner server list.
 * Unknown backend fields are ignored by Gson.
 */
public class PromotedServerView {
    private String id;
    private String name;
    private String address;
    private String description;
    private boolean active;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
