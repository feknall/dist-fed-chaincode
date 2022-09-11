package org.hyperledger.fabric.samples.assettransfer;

import com.owlike.genson.annotation.JsonProperty;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType()
public final class PersonalInfo {
    @Property()
    private final String clientId;
    @Property()
    private final String role;
    @Property()
    private final Boolean selectedForRound;
    @Property()
    private final Boolean checkedIn;
    @Property()
    private final String mspId;
    @Property()
    private final String username;

    public PersonalInfo(@JsonProperty("clientId") final String clientId,
                        @JsonProperty("role") final String role,
                        @JsonProperty("mspId") final String mspId,
                        @JsonProperty("username") final String username,
                        @JsonProperty("selectedForRound") final Boolean selectedForRound,
                        @JsonProperty("checkedIn") final Boolean checkedIn) {
        this.clientId = clientId;
        this.role = role;
        this.selectedForRound = selectedForRound;
        this.checkedIn = checkedIn;
        this.mspId = mspId;
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public Boolean isSelectedForRound() {
        return selectedForRound;
    }

    public String getClientId() {
        return clientId;
    }

    public String getMspId() {
        return mspId;
    }

    public String getUsername() {
        return username;
    }

    public Boolean getCheckedIn() {
        return checkedIn;
    }
}
