package org.hyperledger.fabric.samples.assettransfer;

import com.google.gson.Gson;
import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class TrainerMetadata {
    @Property()
    private final String clientId;
    @Property()
    private final String checkedInTimestamp;
    @Property()
    private final int roundSelectedFor;
    @Property()
    private final String username;

    public TrainerMetadata(@JsonProperty("clientId") final String clientId,
                           @JsonProperty("username") final String username,
                           @JsonProperty("checkedInTimestamp") final String checkedInTimestamp,
                           @JsonProperty("roundSelectedFor") final int roundSelectedFor) {
        this.clientId = clientId;
        this.username = username;
        this.checkedInTimestamp = checkedInTimestamp;
        this.roundSelectedFor = roundSelectedFor;
    }

    public TrainerMetadata(@JsonProperty("clientId") final String clientId,
                           @JsonProperty("username") final String username,
                           @JsonProperty("checkedInTimestamp") final String checkedInTimestamp) {
        this.clientId = clientId;
        this.username = username;
        this.checkedInTimestamp = checkedInTimestamp;
        this.roundSelectedFor = -1;
    }


    public String getClientId() {
        return clientId;
    }

    public String getCheckedInTimestamp() {
        return checkedInTimestamp;
    }

    public int getRoundSelectedFor() {
        return roundSelectedFor;
    }

    public String getUsername() {
        return username;
    }

    public String serialize() {
        return new Gson().toJson(this);
    }

    public static TrainerMetadata deserialize(final String json) {
        return new Gson().fromJson(json, TrainerMetadata.class);
    }
}
