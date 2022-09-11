package org.hyperledger.fabric.samples.assettransfer;

import com.google.gson.Gson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class AggregatorMetadata {
    @Property()
    private final String clientId;
    @Property()
    private final String checkedInTimestamp;
    @Property()
    private final String username;

    public AggregatorMetadata(@JsonProperty("clientId") final String clientId,
                              @JsonProperty("username") final String username,
                              @JsonProperty("checkedInTimestamp") final String checkedInTimestamp) {
        this.clientId = clientId;
        this.username = username;
        this.checkedInTimestamp = checkedInTimestamp;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCheckedInTimestamp() {
        return checkedInTimestamp;
    }

    public String getUsername() {
        return username;
    }

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static AggregatorMetadata deserialize(final String json) {
        return new Gson().fromJson(json, AggregatorMetadata.class);
    }
}
